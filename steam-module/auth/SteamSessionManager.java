package com.gamematchmaker.steam.auth;

import com.gamematchmaker.steam.model.SteamModels.SteamProfile;
import com.gamematchmaker.steam.util.SteamConstants;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/*
 * SteamSessionManager.java

 *   One responsibility: manage the HTTP session that remembers a logged-in
 *   Steam user between requests. It does NOT validate login, does NOT call
 *   Steam's API, does NOT make servlet responses — those are SteamAuthService
 *   and the servlet classes' jobs.

 */
public final class SteamSessionManager {

    // Sessions expire after 1 hour of inactivity (3600 seconds)
    public static final int SESSION_TIMEOUT_SECONDS = 3600;

    // Private constructor — all methods are static, no instance needed
    private SteamSessionManager() {}

    // Creating a session (called by SteamCallbackServlet after login)

    /**
     * Stores the logged-in user's Steam profile in a new HTTP session.
     *
     * Security note: we invalidate any existing session FIRST before
     * creating a new one. This is "session fixation prevention" — if an
     * attacker obtained a session ID before login, destroying it here
     * prevents them from using it to impersonate the user after login.
     *
     * We store only the SteamID and the public SteamProfile — never any
     * Steam credentials or passwords (SRS §3.2.3).
     */
    public static void createSession(HttpServletRequest req, SteamProfile profile) {
        // Destroy any existing session before creating a fresh one
        HttpSession existing = req.getSession(false); // false = don't create if missing
        if (existing != null) {
            existing.invalidate();
        }

        // Create a brand new session and store the user's data
        HttpSession session = req.getSession(true); // true = create if it doesn't exist
        session.setAttribute(SteamConstants.SESSION_STEAM_ID, profile.steamId);
        session.setAttribute(SteamConstants.SESSION_USER, profile);
        session.setMaxInactiveInterval(SESSION_TIMEOUT_SECONDS);
    }

    // Reading the session (called on every protected request)

    /**
     * Returns true if this request has a valid authenticated Steam session.
     *
     * Three conditions must ALL be true:
     *   1. A session exists
     *   2. A SteamID string is stored in it
     *   3. A SteamProfile object is stored in it
     *
     * If any condition fails, the user is not properly authenticated.
     */
    public static boolean isAuthenticated(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        return session != null
                && session.getAttribute(SteamConstants.SESSION_STEAM_ID) != null
                && session.getAttribute(SteamConstants.SESSION_USER) instanceof SteamProfile;
    }

    /**
     * Returns the current user's SteamID string from the session.
     * Returns null if nobody is logged in.
     */
    public static String getCurrentSteamId(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) return null;
        Object steamId = session.getAttribute(SteamConstants.SESSION_STEAM_ID);
        return steamId instanceof String ? (String) steamId : null;
    }

    /**
     * Returns the current user's full SteamProfile from the session.
     * Returns null if nobody is logged in.
     *
     * Example use in a servlet:
     *   SteamProfile user = SteamSessionManager.getCurrentProfile(request);
     *   System.out.println("Hello, " + user.personaName);
     */
    public static SteamProfile getCurrentProfile(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) return null;
        Object profile = session.getAttribute(SteamConstants.SESSION_USER);
        return profile instanceof SteamProfile ? (SteamProfile) profile : null;
    }

    // Destroying the session (called by SteamLogoutServlet)

    /**
     * Logs the user out by destroying their session.
     * Safe to call even if no session currently exists.
     */
    public static void invalidateSession(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}
