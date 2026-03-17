package com.gamematchmaker.steam.auth;

import com.gamematchmaker.steam.exception.SteamAuthException;
import com.gamematchmaker.steam.exception.SteamApiException;
import com.gamematchmaker.steam.api.SteamApiClient;
import com.gamematchmaker.steam.model.SteamModels.SteamProfile;
import com.gamematchmaker.steam.util.SteamConstants;
import com.gamematchmaker.steam.util.SteamHttpClient;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages the three-step Steam OpenID 2.0 authentication flow (FR 3.1.1).
 *
 * Step 1 — {@link #buildLoginUrl()}
 *   Build the URL to redirect the user's browser to Steam's login page.
 *
 * Step 2 — {@link #validateCallback(Map)}
 *   After Steam redirects back, verify the callback is genuine by
 *   re-posting the parameters to Steam with mode=check_authentication.
 *   Returns the verified SteamID on success, null on failure.
 *
 * Step 3 — {@link #fetchProfile(String)}
 *   Use the verified SteamID to fetch the user's public profile data
 *   from the Steam Web API (ISteamUser/GetPlayerSummaries).
 *
 * Security notes (SRS §3.2.3):
 *   - No Steam credentials are stored at any point.
 *   - The API key is read from an environment variable, never hard-coded.
 *   - All traffic uses HTTPS (enforced by Steam's endpoints).
 */
public class SteamAuthService {

    private static final Logger LOGGER = Logger.getLogger(SteamAuthService.class.getName());

    /** Regex to extract the 64-bit SteamID from Steam's claimed_id URL. */
    private static final Pattern STEAM_ID_PATTERN = Pattern.compile(
            Pattern.quote(SteamConstants.OPENID_CLAIMED_ID_PREFIX) + "(\\d+)"
    );

    private final String        callbackUrl;
    private final SteamApiClient apiClient;

    /**
     * @param apiKey      Steam Web API key (from env var STEAM_API_KEY).
     * @param callbackUrl Full URL Steam will redirect to after login,
     *                    e.g. {@code "https://yourdomain.com/auth/steam/callback"}.
     */
    public SteamAuthService(String apiKey, String callbackUrl) {
        if (apiKey == null || apiKey.isBlank())
            throw new IllegalArgumentException("Steam API key must not be null or blank.");
        if (callbackUrl == null || callbackUrl.isBlank())
            throw new IllegalArgumentException("Callback URL must not be null or blank.");

        this.callbackUrl = callbackUrl;
        this.apiClient   = new SteamApiClient(apiKey);
    }

    // =========================================================================
    // Step 1 — Build login redirect URL
    // =========================================================================

    /**
     * Builds the full URL to redirect the user's browser to for Steam login.
     *
     * The URL points to Steam's OpenID endpoint with all required OpenID 2.0
     * parameters. The user authenticates on Steam's own domain; this application
     * never sees their Steam password (SRS §3.2.3).
     *
     * @return Ready-to-use redirect URL string.
     */
    public String buildLoginUrl() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("openid.ns",         SteamConstants.OPENID_NS);
        params.put("openid.mode",       SteamConstants.OPENID_MODE_CHECKID);
        params.put("openid.return_to",  callbackUrl);
        params.put("openid.realm",      extractRealm(callbackUrl));
        params.put("openid.identity",   SteamConstants.OPENID_IDENTIFIER_SELECT);
        params.put("openid.claimed_id", SteamConstants.OPENID_IDENTIFIER_SELECT);

        return SteamConstants.OPENID_ENDPOINT + "?" + SteamHttpClient.buildQueryString(params);
    }

    // =========================================================================
    // Step 2 — Validate Steam's callback and extract SteamID
    // =========================================================================

    /**
     * Verifies the OpenID callback that Steam sends to the callback URL.
     *
     * Sends the callback parameters back to Steam with
     * {@code openid.mode=check_authentication}. Steam replies with
     * {@code is_valid:true} if the response is genuine.
     *
     * @param callbackParams All query parameters from the callback request
     *                       (typically from {@code HttpServletRequest.getParameterMap()}).
     * @return The verified 64-bit SteamID string, or {@code null} if validation fails.
     * @throws SteamAuthException If network communication with Steam fails.
     */
    public String validateCallback(Map<String, String> callbackParams) throws SteamAuthException {
        // Pre-condition: both openid.mode and openid.claimed_id must be present
        if (!callbackParams.containsKey("openid.mode")
                || !callbackParams.containsKey("openid.claimed_id")) {
            LOGGER.warning("Callback missing required OpenID fields.");
            return null;
        }

        // Switch mode to check_authentication for verification POST
        Map<String, String> verifyParams = new LinkedHashMap<>(callbackParams);
        verifyParams.put("openid.mode", SteamConstants.OPENID_MODE_CHECK_AUTH);

        String response;
        try {
            response = SteamHttpClient.post(SteamConstants.OPENID_ENDPOINT, verifyParams);
        } catch (SteamApiException e) {
            throw new SteamAuthException("Network error verifying Steam callback.", e);
        }

        if (response == null || !response.contains("is_valid:true")) {
            LOGGER.warning("Steam OpenID verification failed. Response: " + response);
            return null;
        }

        // Extract and return the SteamID from claimed_id
        String claimedId = callbackParams.get("openid.claimed_id");
        String steamId   = extractSteamId(claimedId);

        if (steamId == null) {
            LOGGER.warning("Could not parse SteamID from claimed_id: " + claimedId);
        } else {
            LOGGER.info("OpenID verification successful. SteamID: " + steamId);
        }
        return steamId;
    }

    // =========================================================================
    // Step 3 — Fetch public profile
    // =========================================================================

    /**
     * Fetches the user's public profile data from the Steam Web API
     * using their verified SteamID.
     *
     * Satisfies the post-condition of FR 3.1.1:
     * "The user's basic public Steam profile data (SteamID, persona name,
     * avatar) is imported."
     *
     * @param steamId The verified 64-bit SteamID from {@link #validateCallback}.
     * @return A populated {@link SteamProfile}, or {@code null} if not found.
     * @throws SteamAuthException If the Steam API call fails.
     */
    public SteamProfile fetchProfile(String steamId) throws SteamAuthException {
        try {
            return apiClient.fetchProfile(steamId);
        } catch (SteamApiException e) {
            throw new SteamAuthException("Failed to fetch Steam profile for SteamID: " + steamId, e);
        }
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Extracts the 64-bit SteamID from Steam's claimed_id URL.
     * e.g. "https://steamcommunity.com/openid/id/76561198000000001" → "76561198000000001"
     */
    private String extractSteamId(String claimedId) {
        if (claimedId == null) return null;
        Matcher m = STEAM_ID_PATTERN.matcher(claimedId);
        return m.find() ? m.group(1) : null;
    }

    /**
     * Extracts the scheme + host realm from the callback URL.
     * e.g. "https://myapp.com/auth/callback" → "https://myapp.com"
     */
    private String extractRealm(String url) {
        try {
            URI uri = URI.create(url);
            return uri.getScheme() + "://" + uri.getHost()
                    + (uri.getPort() > 0 ? ":" + uri.getPort() : "");
        } catch (Exception e) {
            LOGGER.warning("Could not extract realm from: " + url);
            return url;
        }
    }
}
