package com.gamematchmaker.steam.servlet;

import com.gamematchmaker.steam.auth.SteamSessionManager;
import com.gamematchmaker.steam.exception.SteamApiException;
import com.gamematchmaker.steam.importer.ImportSteam;
import com.gamematchmaker.steam.model.SteamModels.SteamGame;
import com.gamematchmaker.steam.model.SteamModels.SteamProfile;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * DashboardServlet.java
 *
 *
 *   This servlet has a focused scope: serve the post-login UI and two JSON
 *   API endpoints. It does NOT validate authentication (SteamAuthFilter's
 *   job), does NOT manage sessions (SteamSessionManager's job), does NOT
 *   make raw HTTP calls to Steam (ImportSteam's job). Each of the three
 *   handler methods (serveDashboardPage, serveProfileAsJson, serveGamesAsJson)
 *   is kept private and handles only its own URL 
 *
 * Endpoints served:
 *   GET /dashboard      — Post-login dashboard HTML 
 *   GET /api/profile    — Current user's SteamProfile as JSON
 *   GET /api/games      — Current user's owned games as JSON array 
 */
@WebServlet(urlPatterns = {"/dashboard", "/api/profile", "/api/games"})
public class DashboardServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(DashboardServlet.class.getName());

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // SteamAuthFilter already verified the session — getCurrentProfile() is safe here
        SteamProfile profile = SteamSessionManager.getCurrentProfile(req);

        // Route to the correct handler based on which URL was requested
        switch (req.getServletPath()) {
            case "/dashboard"   -> serveDashboardPage(resp, profile);
            case "/api/profile" -> serveProfileAsJson(resp, profile);
            case "/api/games"   -> serveGamesAsJson(resp, profile);
            default             -> resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }


    /**
     * Writes the dashboard HTML page.
     *
     * Injects the user's SteamProfile as a JavaScript constant so that
     * dashboard.js can display name and avatar immediately, without making
     * a second round-trip to /api/profile on page load.
     *
     */
    private void serveDashboardPage(HttpServletResponse resp, SteamProfile profile)
            throws IOException {

        resp.setContentType("text/html;charset=UTF-8");

        try (PrintWriter out = resp.getWriter()) {
            out.println("<!DOCTYPE html>");
            out.println("<html lang='en'>");
            out.println("<head>");
            out.println("  <meta charset='UTF-8'>");
            out.println("  <meta name='viewport' content='width=device-width, initial-scale=1.0'>");
            out.println("  <title>Dashboard — Game Match Maker</title>");
            out.println("  <link rel='stylesheet' href='/css/dashboard.css'>");
            out.println("</head>");
            out.println("<body>");

            // Inject the user profile as a JS constant.
            // dashboard.js reads window.CURRENT_USER to know who is logged in.
            // profileToJson() escapes special characters so this is injection-safe.
            out.println("<script>");
            out.println("  const CURRENT_USER = " + profileToJson(profile) + ";");
            out.println("</script>");

            out.println("<div id='app'></div>");
            out.println("<script src='/js/dashboard.js'></script>");
            out.println("</body>");
            out.println("</html>");
        }
    }


    // GET /api/profile — returns current user's profile as JSON
    /**
     * Returns the logged-in user's SteamProfile as a JSON object.
     * Other team members can call this endpoint to get who is currently logged in.
     *
     * Response shape:
     * {
     *   "steamId": "76561198000000001",
     *   "personaName": "Jeff",
     *   "avatarFull": "https://...",
     *   "profileUrl": "https://...",
     *   "isPublic": true
     * }
     */
    private void serveProfileAsJson(HttpServletResponse resp, SteamProfile profile)
            throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        // HTTP 200 is the default — we explicitly set it for clarity
        resp.setStatus(HttpServletResponse.SC_OK);
        try (PrintWriter out = resp.getWriter()) {
            out.print(profileToJson(profile));
        }
    }

    // GET /api/games — returns the user's game library as a JSON array

    /**
     * Fetches the user's owned Steam games and returns them as a JSON array.
     * Games are sorted by total playtime, most played first.
     *
     * Uses HTTP status codes to communicate specific failure reasons:
     *   503 = STEAM_API_KEY is not configured on the server
     *   403 = user's Steam library is Private (they need to change it)
     *   502 = Steam's servers returned an error
     */
    private void serveGamesAsJson(HttpServletResponse resp, SteamProfile profile)
            throws IOException {

        resp.setContentType("application/json;charset=UTF-8");

        String apiKey = System.getenv("STEAM_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            // 503 Service Unavailable — server is misconfigured
            resp.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            try (PrintWriter out = resp.getWriter()) {
                out.print("{\"error\":\"Server misconfiguration: STEAM_API_KEY not set.\"}");
            }
            return;
        }

        List<SteamGame> games;
        try {
            ImportSteam importer = new ImportSteam(profile.steamId, apiKey);
            games = importer.fetchOwnedGames(true, true);
        } catch (SteamApiException e) {
            LOGGER.log(Level.WARNING, "Could not fetch games for " + profile.steamId, e);

            if (e.isUnauthorized()) {
                // 403 Forbidden — user's library is private
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                try (PrintWriter out = resp.getWriter()) {
                    out.print("{\"error\":\"Your Steam game library is set to Private. " +
                              "Please change it to Public in Steam Privacy Settings.\"}");
                }
            } else {
                // 502 Bad Gateway — Steam is unavailable
                resp.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
                try (PrintWriter out = resp.getWriter()) {
                    out.print("{\"error\":\"Steam is temporarily unavailable. Try again shortly.\"}");
                }
            }
            return;
        }

        // Build the JSON array response
        // 200 OK — return the games list
        resp.setStatus(HttpServletResponse.SC_OK);
        try (PrintWriter out = resp.getWriter()) {
            out.print("[");
            for (int i = 0; i < games.size(); i++) {
                if (i > 0) out.print(",");
                out.print(gameToJson(games.get(i)));
            }
            out.print("]");
        }
    }

    // 
    // Private JSON helpers 
    // 

    // Converts a SteamProfile to a safe JSON object string
    private String profileToJson(SteamProfile p) {
        return "{"
            + "\"steamId\":"     + quote(p.steamId)    + ","
            + "\"personaName\":" + quote(p.personaName) + ","
            + "\"avatarFull\":"  + quote(p.avatarFull)  + ","
            + "\"profileUrl\":"  + quote(p.profileUrl)  + ","
            + "\"isPublic\":"    + p.isPublic()         
            + "}";
    }

    // Converts a SteamGame to a safe JSON object string
    private String gameToJson(SteamGame g) {
        return "{"
            + "\"appId\":"           + quote(g.appId)       + ","
            + "\"name\":"            + quote(g.name)         + ","
            + "\"imgIconUrl\":"      + quote(g.imgIconUrl)   + ","
            + "\"playtimeForever\":" + g.playtimeForever     + ","  
            + "\"playtime2Weeks\":"  + g.playtime2Weeks      + ","
            + "\"playtimeHours\":"   + g.playtimeHours()
            + "}";
    }

    /**
     * Wraps a String in JSON double-quotes and escapes characters that
     * would break JSON format or allow XSS injection.
     * Returns JSON null (unquoted) if the input is null.
     */
    private String quote(String s) {
        if (s == null) return "null";
        return "\""
            + s.replace("\\", "\\\\")
               .replace("\"", "\\\"")
               .replace("\n", "\\n")
               .replace("\r", "\\r")
            + "\"";
    }
}
