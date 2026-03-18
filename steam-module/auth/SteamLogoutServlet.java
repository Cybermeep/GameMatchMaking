package com.gamematchmaker.steam.auth;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

/*
 * SteamLogoutServlet.java

 *   One responsibility: handle logout. Destroy the session and redirect home.
 *   This is intentionally the smallest possible servlet — it delegates session
 *   destruction to SteamSessionManager rather than touching HttpSession
 *   directly. One reason to change: if the post-logout redirect URL changes.
 *

 */
@WebServlet(name = "SteamLogoutServlet", urlPatterns = "/auth/steam/logout")
public class SteamLogoutServlet extends HttpServlet {

    /**
     * Handles GET /auth/steam/logout.
     * Destroys the HTTP session (removes the stored SteamProfile),
     * then redirects the user to the home page.
     *
     * Note: We don't need to "log out" from Steam itself. OpenID only
     * tells us who someone is during login — there is no ongoing connection
     * to Steam's servers during the user's session. Invalidating our own
     * session is everything that's needed.
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        SteamSessionManager.invalidateSession(req);
        resp.sendRedirect(req.getContextPath() + "/");
    }
}
