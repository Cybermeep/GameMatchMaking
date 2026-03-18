package com.gamematchmaker.steam.auth;

import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.logging.*;

/*
 * SteamLoginServlet.java

 *   One job: receive a GET request to /auth/steam/login, then redirect the
 *   user to Steam's OpenID login page. That's it. No session logic, no API
 *   calls, no HTML generation. One reason to change: if the login URL path
 *   or startup config validation changes.

 */
@WebServlet(name = "SteamLoginServlet", urlPatterns = "/auth/steam/login")
public class SteamLoginServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(SteamLoginServlet.class.getName());

    // Stored once at startup so we don't re-create it on every request
    private SteamAuthService authService;

    /**
     * Called once when Tomcat first loads this servlet.
     * Reads config and creates the auth service. Throws on misconfiguration
     * so problems are caught at startup rather than hidden until first use.
     */
    @Override
    public void init() throws ServletException {
        String apiKey      = getEnv("STEAM_API_KEY");
        String callbackUrl = getEnv("STEAM_CALLBACK_URL");

        if (apiKey == null || callbackUrl == null) {
            throw new ServletException(
                "Missing config: STEAM_API_KEY and STEAM_CALLBACK_URL must be set " +
                "as environment variables or web.xml init-params.");
        }

        authService = new SteamAuthService(apiKey, callbackUrl);
        LOGGER.info("SteamLoginServlet ready. Callback: " + callbackUrl);
    }

    /**
     * Handles GET /auth/steam/login.
     * Builds the Steam OpenID redirect URL and sends the user there.
     * The user authenticates on Steam's own domain — we never see their password.
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String steamLoginUrl = authService.buildLoginUrl();
        LOGGER.info("Redirecting to Steam login.");
        resp.sendRedirect(steamLoginUrl);
    }

    /**
     * Reads a config value, checking web.xml init-params first, then
     * falling back to OS environment variables (set via run.sh / .env).
     * Returns null if the key is not set in either location.
     */
    private String getEnv(String key) {
        String value = getInitParameter(key);
        if (value == null || value.isBlank()) {
            value = System.getenv(key);
        }
        return (value == null || value.isBlank()) ? null : value;
    }
}
