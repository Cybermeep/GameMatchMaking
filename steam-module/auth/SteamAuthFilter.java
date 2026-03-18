package com.gamematchmaker.steam.auth;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.logging.Logger;

/*
 * SteamAuthFilter.java
 *   One responsibility: intercept every incoming request to a protected URL
 *   and decide whether to let it through or block it. It does NOT handle
 *   login logic, does NOT generate responses, does NOT manage sessions
 *   directly — it delegates that to SteamSessionManager.

 */
@WebFilter(urlPatterns = {"/api/*", "/dashboard/*", "/group/*", "/profile/*", "/games/*"})
public class SteamAuthFilter implements Filter {

    private static final Logger LOGGER = Logger.getLogger(SteamAuthFilter.class.getName());

    /**
     * Called by Tomcat for every request matching the urlPatterns above.
     *
     * Chain of Responsibility: this is the first handler. If auth passes,
     * chain.doFilter() passes the request to the next handler (the servlet).
     * If auth fails, we respond immediately and the chain stops here.
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest  req  = (HttpServletRequest)  request;
        HttpServletResponse resp = (HttpServletResponse) response;

        if (SteamSessionManager.isAuthenticated(req)) {
            // Authenticated — pass to the next handler in the chain
            chain.doFilter(request, response);
        } else {
            LOGGER.info("Blocked unauthenticated request to: " + req.getRequestURI());

            // API calls (/api/*) get HTTP 401 — the frontend JS expects a status code
            if (req.getRequestURI().startsWith(req.getContextPath() + "/api/")) {
                resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Login required.");
            } else {
                // Page requests get redirected to the login page
                resp.sendRedirect(req.getContextPath() + "/auth/steam/login");
            }
        }
    }

    // Required by the Filter interface — no setup or teardown needed here
    @Override public void init(FilterConfig config) {}
    @Override public void destroy() {}
}
