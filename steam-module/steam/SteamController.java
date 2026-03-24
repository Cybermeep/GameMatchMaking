/**
 * SteamController Class
 * 
 * Handles all web requests related to Steam authentication and user data.
 * This controller manages the OAuth flow, session management, and serves
 * the web interface for the Steam login demo.
 * 
 *
 * - Handle login requests and redirect to Steam
 * - Process OAuth callback from Steam
 * - Manage user sessions
 * - Serve web pages for the demo
 * 
 * @author The Match Makers
 * @version 1.0
 */
package edu.isu.gamematch.steam;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.*;

@Controller
public class SteamController {
    
    private static final Logger logger = LoggerFactory.getLogger(SteamController.class);
    private static final String SESSION_ATTRIBUTE = "steam_user";
    
    @Autowired
    private SteamAuthService authService;
    
    @Autowired
    private SteamAPIService apiService;
    
    /**
     * Home page - displays login option or dashboard if authenticated
     */
    @GetMapping("/")
    public String home(HttpSession session, Model model) {
        SteamUser user = (SteamUser) session.getAttribute(SESSION_ATTRIBUTE);
        
        if (user != null && user.isAuthenticated()) {
            model.addAttribute("user", user);
            model.addAttribute("gameCount", user.getTotalGameCount());
            model.addAttribute("totalPlaytime", user.getTotalPlaytime());
            return "dashboard";
        }
        
        return "index";
    }
    
    /**
     * Initiate Steam login - redirects to Steam OAuth
     */
    @GetMapping("/auth/steam")
    public String initiateSteamLogin(HttpSession session) {
        String authUrl = authService.getSteamAuthUrl();
        logger.info("Initiating Steam login, redirecting to: {}", authUrl);
        return "redirect:" + authUrl;
    }
    
    /**
     * Steam OAuth callback - processes the authentication response
     */
    @GetMapping("/auth/steam/callback")
    public String steamCallback(HttpServletRequest request, HttpSession session, Model model) {
        try {
            Map<String, String[]> params = request.getParameterMap();
            String steamId = authService.processCallback(params);
            
            if (steamId == null) {
                logger.warn("Steam authentication failed - invalid callback");
                model.addAttribute("error", "Steam authentication failed. Please try again.");
                return "index";
            }
            
            // Fetch user data from Steam API
            SteamUser user = apiService.fetchCompleteUserData(steamId);
            
            if (user == null) {
                logger.error("Failed to fetch user data for Steam ID: {}", steamId);
                model.addAttribute("error", "Failed to retrieve your Steam profile. Please check your privacy settings.");
                return "index";
            }
            
            // Create session for the user
            String sessionToken = authService.createSession(user);
            session.setAttribute(SESSION_ATTRIBUTE, user);
            session.setAttribute("sessionToken", sessionToken);
            
            logger.info("User successfully logged in: {} ({})", user.getPersonaName(), steamId);
            return "redirect:/dashboard";
            
        } catch (Exception e) {
            logger.error("Error during Steam callback processing", e);
            model.addAttribute("error", "An unexpected error occurred. Please try again.");
            return "index";
        }
    }
    
    /**
     * Dashboard page - displays user profile and game library
     */
    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        SteamUser user = (SteamUser) session.getAttribute(SESSION_ATTRIBUTE);
        
        if (user == null || !user.isAuthenticated()) {
            return "redirect:/";
        }
        
        model.addAttribute("user", user);
        model.addAttribute("gameCount", user.getTotalGameCount());
        model.addAttribute("totalPlaytime", user.getTotalPlaytime());
        model.addAttribute("recentGames", user.getRecentlyPlayed());
        model.addAttribute("topGames", getTopGames(user.getOwnedGames(), 5));
        
        return "dashboard";
    }
    
    /**
     * Resynchronize user's game library
     */
    @PostMapping("/resync")
    public String resyncGames(HttpSession session, Model model) {
        SteamUser user = (SteamUser) session.getAttribute(SESSION_ATTRIBUTE);
        
        if (user == null || !user.isAuthenticated()) {
            return "redirect:/";
        }
        
        try {
            // Fetch updated game data
            List<SteamGame> updatedGames = apiService.fetchOwnedGames(user.getSteamId());
            user.getOwnedGames().clear();
            for (SteamGame game : updatedGames) {
                user.addGame(game);
            }
            
            // Also update recently played
            List<SteamGame> recentGames = apiService.fetchRecentlyPlayedGames(user.getSteamId());
            user.getRecentlyPlayed().clear();
            user.getRecentlyPlayed().addAll(recentGames);
            
            logger.info("Resynced game library for user: {}", user.getPersonaName());
            model.addAttribute("success", "Game library successfully updated!");
            
        } catch (Exception e) {
            logger.error("Failed to resync games for user: {}", user.getPersonaName(), e);
            model.addAttribute("error", "Failed to sync game library. Please try again.");
        }
        
        model.addAttribute("user", user);
        model.addAttribute("gameCount", user.getTotalGameCount());
        model.addAttribute("totalPlaytime", user.getTotalPlaytime());
        model.addAttribute("recentGames", user.getRecentlyPlayed());
        
        return "dashboard";
    }
    
    /**
     * Logout - invalidates user session
     */
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        SteamUser user = (SteamUser) session.getAttribute(SESSION_ATTRIBUTE);
        
        if (user != null) {
            authService.invalidateSession(user.getSessionToken());
            session.invalidate();
            logger.info("User logged out: {}", user.getPersonaName());
        }
        
        return "redirect:/";
    }
    
    /**
     * Helper method to get top N games by playtime
     */
    private List<SteamGame> getTopGames(List<SteamGame> games, int count) {
        return games.stream()
            .sorted((a, b) -> Integer.compare(b.getPlaytimeForever(), a.getPlaytimeForever()))
            .limit(count)
            .collect(java.util.stream.Collectors.toList());
    }
}