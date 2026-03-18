package com.gamematchmaker.steam.auth;

import com.gamematchmaker.steam.api.SteamApiClient;
import com.gamematchmaker.steam.exception.SteamApiException;
import com.gamematchmaker.steam.exception.SteamAuthException;
import com.gamematchmaker.steam.model.SteamModels.SteamProfile;
import com.gamematchmaker.steam.util.SteamConstants;
import com.gamematchmaker.steam.util.SteamHttpClient;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * SteamAuthService.java
 *   One responsibility: manage the Steam OpenID 2.0 authentication flow.
 *   It does NOT serve HTTP responses, does NOT manage sessions, does NOT
 *   fetch game libraries — those are other classes' jobs. One reason to
 *   change: if Steam's OpenID protocol or endpoint changes.

 *
 * How the 3-step login flow works:
 *
 * STEP 1 — buildLoginUrl()
 *   User clicks "Login with Steam" → we redirect them to Steam's OpenID page.
 *   Steam handles their username/password — we never see it.
 *
 * STEP 2 — validateCallback()
 *   After login, Steam redirects user back to /auth/steam/callback with
 *   openid.* parameters. We POST those back to Steam as a "check" to confirm
 *   they are genuine (not a forged redirect). Steam replies "is_valid:true"
 *   or "is_valid:false". We extract the SteamID from claimed_id on success.
 *
 * STEP 3 — fetchProfile()
 *   Use the verified SteamID to call Steam's Web API for the user's name,
 *   avatar, and privacy state.
 */
public class SteamAuthService {

    private static final Logger LOGGER = Logger.getLogger(SteamAuthService.class.getName());

    // Regex to pull the 64-bit SteamID number from Steam's claimed_id URL:
    // "https://steamcommunity.com/openid/id/76561198000000001" → "76561198000000001"
    private static final Pattern STEAM_ID_PATTERN = Pattern.compile(
            Pattern.quote(SteamConstants.OPENID_CLAIMED_ID_PREFIX) + "(\\d+)"
    );

    // Where Steam will redirect the user after they log in
    // (e.g. "http://localhost:8080/auth/steam/callback")
    private final String callbackUrl;

    // DIP: we depend on SteamApiClient (the abstraction), not on HttpURLConnection directly
    private final SteamApiClient apiClient;

    /**
     * Constructor.
     *
     * apiKey:       Steam Web API key from STEAM_API_KEY env var.
     * callbackUrl:  Full URL Steam redirects back to. Must match exactly what
     *               you registered at steamcommunity.com/dev/apikey.
     */
    public SteamAuthService(String apiKey, String callbackUrl) {
        if (apiKey == null || apiKey.isBlank())
            throw new IllegalArgumentException("Steam API key is required.");
        if (callbackUrl == null || callbackUrl.isBlank())
            throw new IllegalArgumentException("Callback URL is required.");

        this.callbackUrl = callbackUrl;
        this.apiClient   = new SteamApiClient(apiKey);
    }

    // STEP 1 — Build login redirect URL
    // Chain of Responsibility: first handler in the auth chain

    /**
     * Builds the URL to redirect the user's browser to Steam's login page.
     *
     * The URL includes all OpenID 2.0 required parameters. The user logs
     * in on Steam's own domain — this application never handles their password.
     */
    public String buildLoginUrl() {
        // LinkedHashMap preserves insertion order, keeping URL params consistent
        Map<String, String> params = new LinkedHashMap<>();
        params.put("openid.ns",         SteamConstants.OPENID_NS);
        params.put("openid.mode",       SteamConstants.OPENID_MODE_CHECKID);
        params.put("openid.return_to",  callbackUrl);
        params.put("openid.realm",      extractRealm(callbackUrl));
        params.put("openid.identity",   SteamConstants.OPENID_IDENTIFIER_SELECT);
        params.put("openid.claimed_id", SteamConstants.OPENID_IDENTIFIER_SELECT);

        return SteamConstants.OPENID_ENDPOINT + "?" + SteamHttpClient.buildQueryString(params);
    }

    // STEP 2 — Validate Steam's callback
    // Chain of Responsibility: second handler — verifies and extracts SteamID

    /**
     * Verifies that Steam's callback is genuine — not a forged redirect.
     *
     * We take the parameters Steam sent us, change openid.mode to
     * "check_authentication", and POST them back to Steam. Steam replies
     * with "is_valid:true" only if the callback was genuinely from them.
     *
     * Returns the verified 64-bit SteamID string, or null if validation fails.
     * Throws SteamAuthException if we cannot reach Steam's servers.
     */
    public String validateCallback(Map<String, String> callbackParams) throws SteamAuthException {
        // Both fields must be present for a valid OpenID response
        if (!callbackParams.containsKey("openid.mode")
                || !callbackParams.containsKey("openid.claimed_id")) {
            LOGGER.warning("Callback is missing required OpenID fields.");
            return null;
        }

        // Copy params and switch the mode to trigger verification
        Map<String, String> verifyParams = new LinkedHashMap<>(callbackParams);
        verifyParams.put("openid.mode", SteamConstants.OPENID_MODE_CHECK_AUTH);

        String response;
        try {
            response = SteamHttpClient.post(SteamConstants.OPENID_ENDPOINT, verifyParams);
        } catch (SteamApiException e) {
            // Wrap the lower-level exception — callers only need to handle SteamAuthException
            throw new SteamAuthException("Couldn't reach Steam to verify login.", e);
        }

        // Steam's plain-text response contains "is_valid:true" on success
        if (response == null || !response.contains("is_valid:true")) {
            LOGGER.warning("Steam said the login is not valid. Response: " + response);
            return null;
        }

        // Extract the SteamID number from the claimed_id URL
        String claimedId = callbackParams.get("openid.claimed_id");
        String steamId   = extractSteamId(claimedId);

        if (steamId == null) {
            LOGGER.warning("Couldn't parse SteamID from: " + claimedId);
        } else {
            LOGGER.info("Login verified! SteamID: " + steamId);
        }
        return steamId;
    }

    // STEP 3 — Fetch the verified user's profile
    // Chain of Responsibility: final handler — retrieves user data from Steam

    /**
     * Retrieves the user's public profile from the Steam Web API using their
     * verified SteamID. Returns null if no profile is found.
     * Throws SteamAuthException if the API call fails.
     */
    public SteamProfile fetchProfile(String steamId) throws SteamAuthException {
        try {
            return apiClient.fetchProfile(steamId);
        } catch (SteamApiException e) {
            throw new SteamAuthException(
                "Couldn't fetch profile for SteamID " + steamId + ": " + e.getMessage(), e);
        }
    }

    // Private helpers (Encapsulation — implementation hidden from callers)

    // Extracts the SteamID number from Steam's claimed_id URL
    // Input:  "https://steamcommunity.com/openid/id/76561198000000001"
    // Output: "76561198000000001"
    private String extractSteamId(String claimedId) {
        if (claimedId == null) return null;
        Matcher m = STEAM_ID_PATTERN.matcher(claimedId);
        return m.find() ? m.group(1) : null;
    }

    // Extracts scheme + host from a URL for the OpenID realm parameter
    // Input:  "https://myapp.com/auth/steam/callback"
    // Output: "https://myapp.com"
    private String extractRealm(String url) {
        try {
            URI uri = URI.create(url);
            String port = uri.getPort() > 0 ? ":" + uri.getPort() : "";
            return uri.getScheme() + "://" + uri.getHost() + port;
        } catch (Exception e) {
            LOGGER.warning("Couldn't parse realm from URL: " + url);
            return url;
        }
    }
}
