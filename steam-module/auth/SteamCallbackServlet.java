package com.gamematchmaker.steam.auth;

import com.gamematchmaker.steam.exception.SteamAuthException;
import com.gamematchmaker.steam.model.SteamModels.SteamProfile;

import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.*;
import java.util.logging.*;

/*
 * SteamCallbackServlet.java
 *

 *   One responsibility: handle the OpenID callback from Steam. It collects
 *   the openid.* parameters, delegates validation to SteamAuthService, then
 *   delegates session creation to SteamSessionManager, and finally redirects.
 *   It does NOT build the login URL (SteamLoginServlet's job), does NOT check

 */
@WebServlet(name = "SteamCallbackServlet", urlPatterns = "/auth/steam/callback")
public class SteamCallbackServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(SteamCallbackServlet.class.getName());
    private SteamAuthService authService;

    @Override
    public void init() throws ServletException {
        String apiKey      = getEnv("STEAM_API_KEY");
        String callbackUrl = getEnv("STEAM_CALLBACK_URL");
        if (apiKey == null || callbackUrl == null) {
            throw new ServletException(
                "Missing config: STEAM_API_KEY and STEAM_CALLBACK_URL must be set.");
        }
        authService = new SteamAuthService(apiKey, callbackUrl);
    }

    /**
     * Handles GET /auth/steam/callback — the redirect from Steam after login.
     *
     * Processing steps (Chain of Responsibility — this is the second handler):
     *   1. Collect the openid.* query parameters Steam attached to the redirect
     *   2. Delegate to SteamAuthService to verify they are genuine
     *   3. Delegate to SteamAuthService to fetch the user's profile
     *   4. Delegate to SteamSessionManager to store the profile in the session
     *   5. Redirect to /dashboard
     *
     * If any step fails, redirect to the home page with an error code.
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Step 1: Collect all openid.* parameters from the callback URL
        // req.getParameterMap() returns Map<String, String[]> — we take values[0]
        Map<String, String> openIdParams = new HashMap<>();
        req.getParameterMap().forEach((key, values) -> {
            if (key.startsWith("openid.") && values.length > 0) {
                openIdParams.put(key, values[0]);
            }
        });

        try {
            // Step 2: Ask SteamAuthService to verify the callback is genuinely from Steam
            String steamId = authService.validateCallback(openIdParams);

            if (steamId == null) {
                LOGGER.warning("Steam callback validation failed — possible fake redirect.");
                resp.sendRedirect(req.getContextPath() + "/?error=validation_failed");
                return;
            }

            // Step 3: Fetch the user's public profile using the verified SteamID
            SteamProfile profile = authService.fetchProfile(steamId);

            if (profile == null) {
                LOGGER.warning("No Steam profile found for SteamID: " + steamId);
                resp.sendRedirect(req.getContextPath() + "/?error=profile_not_found");
                return;
            }

            // Step 4: Store the profile in the session — only public data, no credentials
            SteamSessionManager.createSession(req, profile);
            LOGGER.info("Login successful: " + profile.personaName + " (" + steamId + ")");

            // Step 5: Send to dashboard — the user is now authenticated
            resp.sendRedirect(req.getContextPath() + "/dashboard");

        } catch (SteamAuthException e) {
            // Network failure or Steam server error — tell user to try again
            LOGGER.log(Level.SEVERE, "Steam auth error during callback.", e);
            resp.sendRedirect(req.getContextPath() + "/?error=steam_unavailable");
        }
    }

    private String getEnv(String key) {
        String value = getInitParameter(key);
        if (value == null || value.isBlank()) value = System.getenv(key);
        return (value == null || value.isBlank()) ? null : value;
    }
}
