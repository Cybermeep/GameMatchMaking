package com.gamematchmaker.steam.auth;

import com.gamematchmaker.steam.model.SteamModels.SteamProfile;
import com.gamematchmaker.steam.util.SteamConstants;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * Manages the authenticated user's HTTP session after successful Steam login.
 *
 * Used by {@link SteamCallbackServlet} to store session data and by
 * {@link SteamAuthFilter} to read and validate it on each request.
 *
 * Only the SteamID string and the public SteamProfile are stored —
 * no credentials are persisted in the session (SRS §3.2.3).
 */
public final class SteamSessionManager {

    /** Session idle timeout in seconds (1 hour). */
    public static final int SESSION_TIMEOUT_SECONDS = 3600;

    private SteamSessionManager() {}

    // =========================================================================
    // Session creation (called after successful login)
    // =========================================================================

    /**
     * Creates a new authenticated session for the given Steam user.
     * Invalidates any existing session first to prevent session fixation.
     *
     * @param req     The current HTTP request.
     * @param profile The verified {@link SteamProfile} from the Steam API.
     */
    public static void createSession(HttpServletRequest req, SteamProfile profile) {
        // Invalidate old session to prevent session fixation attacks
        HttpSession old = req.getSession(false);
        if (old != null) old.invalidate();

        HttpSession session = req.getSession(true);
        session.setAttribute(SteamConstants.SESSION_STEAM_ID, profile.steamId);
        session.setAttribute(SteamConstants.SESSION_USER, profile);
        session.setMaxInactiveInterval(SESSION_TIMEOUT_SECONDS);
    }

    // =========================================================================
    // Session reading (called on every protected request)
    // =========================================================================

    /**
     * Returns true if the request has a valid authenticated session.
     */
    public static boolean isAuthenticated(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        return session != null
                && session.getAttribute(SteamConstants.SESSION_STEAM_ID) != null
                && session.getAttribute(SteamConstants.SESSION_USER) instanceof SteamProfile;
    }

    /**
     * Returns the authenticated user's SteamID from the session,
     * or {@code null} if not authenticated.
     */
    public static String getCurrentSteamId(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) return null;
        Object id = session.getAttribute(SteamConstants.SESSION_STEAM_ID);
        return id instanceof String ? (String) id : null;
    }

    /**
     * Returns the authenticated user's {@link SteamProfile} from the session,
     * or {@code null} if not authenticated.
     */
    public static SteamProfile getCurrentProfile(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) return null;
        Object profile = session.getAttribute(SteamConstants.SESSION_USER);
        return profile instanceof SteamProfile ? (SteamProfile) profile : null;
    }

    // =========================================================================
    // Session destruction (logout)
    // =========================================================================

    /**
     * Invalidates the current session (logout).
     * Safe to call even if no session exists.
     */
    public static void invalidateSession(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session != null) session.invalidate();
    }
}
