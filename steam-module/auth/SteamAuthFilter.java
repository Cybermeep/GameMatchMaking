package com.gamematchmaker.steam.auth;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Servlet filter that enforces authentication on all protected routes.
 *
 * Applied to {@code /api/*}, {@code /dashboard/*}, {@code /group/*},
 * {@code /profile/*}, and {@code /games/*} — i.e. any route with the
 * SRS pre-condition "User is authenticated."
 *
 * API requests ({@code /api/*}) receive HTTP 401.
 * Page requests receive a redirect to {@code /auth/steam/login}.
 */
@WebFilter(urlPatterns = {"/api/*", "/dashboard/*", "/group/*", "/profile/*", "/games/*"})
public class SteamAuthFilter implements Filter {

    private static final Logger LOGGER = Logger.getLogger(SteamAuthFilter.class.getName());

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest  req  = (HttpServletRequest)  request;
        HttpServletResponse resp = (HttpServletResponse) response;

        if (SteamSessionManager.isAuthenticated(req)) {
            chain.doFilter(request, response);
        } else {
            LOGGER.info("Unauthenticated access to: " + req.getRequestURI());
            if (req.getRequestURI().startsWith(req.getContextPath() + "/api/")) {
                resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication required.");
            } else {
                resp.sendRedirect(req.getContextPath() + "/auth/steam/login");
            }
        }
    }

    @Override public void init(FilterConfig config) {}
    @Override public void destroy() {}
}
