package com.gamematchmaker.steam.auth;

import com.gamematchmaker.steam.exception.SteamAuthException;
import com.gamematchmaker.steam.model.SteamModels.SteamProfile;

import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.*;
import java.util.logging.*;

// =============================================================================
// GET /auth/steam/login
// Step 1 — Redirect the user's browser to Steam's OpenID login page.
// FR 3.1.1
// =============================================================================
@WebServlet(name = "SteamLoginServlet", urlPatterns = "/auth/steam/login")
class SteamLoginServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(SteamLoginServlet.class.getName());
    private SteamAuthService authService;

    @Override
    public void init() throws ServletException {
        String apiKey  = getEnv("STEAM_API_KEY");
        String cbUrl   = getEnv("STEAM_CALLBACK_URL");
        if (apiKey == null || cbUrl == null)
            throw new ServletException("STEAM_API_KEY and STEAM_CALLBACK_URL environment variables are required.");
        authService = new SteamAuthService(apiKey, cbUrl);
        LOGGER.info("SteamLoginServlet ready. Callback: " + cbUrl);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.sendRedirect(authService.buildLoginUrl());
    }

    private String getEnv(String key) {
        String v = getInitParameter(key);
        return (v == null || v.isBlank()) ? System.getenv(key) : v;
    }
}

// =============================================================================
// GET /auth/steam/callback
// Step 2+3 — Validate Steam's callback, fetch profile, create session.
// FR 3.1.1
// =============================================================================
@WebServlet(name = "SteamCallbackServlet", urlPatterns = "/auth/steam/callback")
class SteamCallbackServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(SteamCallbackServlet.class.getName());
    private SteamAuthService authService;

    @Override
    public void init() throws ServletException {
        String apiKey = getEnv("STEAM_API_KEY");
        String cbUrl  = getEnv("STEAM_CALLBACK_URL");
        if (apiKey == null || cbUrl == null)
            throw new ServletException("STEAM_API_KEY and STEAM_CALLBACK_URL environment variables are required.");
        authService = new SteamAuthService(apiKey, cbUrl);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Collect all openid.* parameters from Steam's redirect
        Map<String, String> params = new HashMap<>();
        req.getParameterMap().forEach((k, v) -> {
            if (k.startsWith("openid.") && v.length > 0) params.put(k, v[0]);
        });

        try {
            // Step 2: Verify the callback is genuinely from Steam
            String steamId = authService.validateCallback(params);
            if (steamId == null) {
                LOGGER.warning("Steam callback validation failed — possible forgery.");
                resp.sendRedirect(req.getContextPath() + "/login?error=validation_failed");
                return;
            }

            // Step 3: Fetch public profile from Steam Web API
            SteamProfile profile = authService.fetchProfile(steamId);
            if (profile == null) {
                LOGGER.warning("Steam profile not found for SteamID: " + steamId);
                resp.sendRedirect(req.getContextPath() + "/login?error=profile_not_found");
                return;
            }

            // Create authenticated session
            SteamSessionManager.createSession(req, profile);
            LOGGER.info("Session created for: " + profile.personaName + " (" + steamId + ")");

            // Redirect to application dashboard
            resp.sendRedirect(req.getContextPath() + "/dashboard");

        } catch (SteamAuthException e) {
            LOGGER.log(Level.SEVERE, "Auth error during Steam callback.", e);
            resp.sendRedirect(req.getContextPath() + "/login?error=steam_unavailable");
        }
    }

    private String getEnv(String key) {
        String v = getInitParameter(key);
        return (v == null || v.isBlank()) ? System.getenv(key) : v;
    }
}

// =============================================================================
// GET /auth/steam/logout
// Invalidates the session and returns the user to the home page.
// =============================================================================
@WebServlet(name = "SteamLogoutServlet", urlPatterns = "/auth/steam/logout")
class SteamLogoutServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        SteamSessionManager.invalidateSession(req);
        resp.sendRedirect(req.getContextPath() + "/");
    }
}
