package edu.isu.gamematch.steam;

import edu.isu.gamematch.Group;
import edu.isu.gamematch.SQLHandler;
import edu.isu.gamematch.User;
import edu.isu.gamematch.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class SteamController {

    private static final Logger logger = LoggerFactory.getLogger(SteamController.class);
    private static final String SESSION_ATTR_STEAM_USER = "steam_user";
    private static final String SESSION_ATTR_DB_USER = "db_user";

    @Autowired
    private SteamAuthService authService;

    @Autowired
    private SteamAPIService apiService;

    @Autowired
    private UserService userService;
    
    @Autowired
    private SQLHandler sqlHandler;  // FIX #2: Added missing SQLHandler

    @GetMapping("/")
    public String home(HttpSession session, Model model) {
        SteamUser steamUser = (SteamUser) session.getAttribute(SESSION_ATTR_STEAM_USER);
        if (steamUser != null && steamUser.isAuthenticated()) {
            return "redirect:/dashboard";
        }
        return "index";
    }

    @GetMapping("/auth/steam")
    public String initiateSteamLogin() {
        String authUrl = authService.getSteamAuthUrl();
        logger.info("Redirecting to Steam OAuth: {}", authUrl);
        return "redirect:" + authUrl;
    }

    @PostMapping("/groups/create")
    public String createGroup(@RequestParam String groupName, HttpSession session, Model model) {
        SteamUser steamUser = (SteamUser) session.getAttribute(SESSION_ATTR_STEAM_USER);
        User dbUser = (User) session.getAttribute(SESSION_ATTR_DB_USER);
        
        if (steamUser == null || dbUser == null) {
            return "redirect:/";
        }
        
        Group group = new Group();
        group.setGroupName(groupName);  // FIX #1: groupName field must exist in Group.java
        group.setGroupOwner(dbUser);
        group.addGroupMember(dbUser);
        
        boolean success = sqlHandler.createGroup(group);  // FIX #3: Use createGroup instead of writeEntity
        
        if (success) {
            model.addAttribute("success", "Group '" + groupName + "' created successfully!");
        } else {
            model.addAttribute("error", "Failed to create group. Please try again.");
        }
        
        return "redirect:/dashboard";
    }

    @GetMapping("/auth/steam/callback")
    public String steamCallback(HttpServletRequest request, HttpSession session, Model model) {
        try {
            Map<String, String[]> params = request.getParameterMap();
            String steamId = authService.processCallback(params);

            if (steamId == null) {
                model.addAttribute("error", "Steam authentication failed. Please try again.");
                return "index";
            }

            // Fetch user profile and game data from Steam API
            SteamUser steamUser = apiService.fetchCompleteUserData(steamId);
            if (steamUser == null) {
                model.addAttribute("error", "Could not retrieve Steam profile. Check privacy settings.");
                return "index";
            }

            // Find or create local user record
            User dbUser = userService.findOrCreateUser(steamId, steamUser.getPersonaName());

            // Create session
            String sessionToken = authService.createSession(steamUser);
            session.setAttribute(SESSION_ATTR_STEAM_USER, steamUser);
            session.setAttribute(SESSION_ATTR_DB_USER, dbUser);
            session.setAttribute("sessionToken", sessionToken);

            logger.info("User logged in: {} ({})", steamUser.getPersonaName(), steamId);
            return "redirect:/dashboard";

        } catch (Exception e) {
            logger.error("Error during Steam callback", e);
            model.addAttribute("error", "An unexpected error occurred. Please try again.");
            return "index";
        }
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        SteamUser steamUser = (SteamUser) session.getAttribute(SESSION_ATTR_STEAM_USER);
        if (steamUser == null || !steamUser.isAuthenticated()) {
            return "redirect:/";
        }

        User dbUser = (User) session.getAttribute(SESSION_ATTR_DB_USER);
        List<Group> userGroups = new ArrayList<>();
        if (dbUser != null) {
            userGroups = sqlHandler.getGroupsByOwner(dbUser);
        }

        model.addAttribute("user", steamUser);
        model.addAttribute("groups", userGroups);
        model.addAttribute("gameCount", steamUser.getTotalGameCount());
        model.addAttribute("totalPlaytime", steamUser.getTotalPlaytime());
        model.addAttribute("recentGames", steamUser.getRecentlyPlayed());
        model.addAttribute("topGames", getTopGames(steamUser.getOwnedGames(), 5));

        return "dashboard";
    }

    @PostMapping("/resync")
    public String resyncGames(HttpSession session, Model model) {
        SteamUser steamUser = (SteamUser) session.getAttribute(SESSION_ATTR_STEAM_USER);
        if (steamUser == null || !steamUser.isAuthenticated()) {
            return "redirect:/";
        }

        try {
            List<SteamGame> updatedGames = apiService.fetchOwnedGames(steamUser.getSteamId());
            steamUser.getOwnedGames().clear();
            updatedGames.forEach(steamUser::addGame);

            List<SteamGame> recent = apiService.fetchRecentlyPlayedGames(steamUser.getSteamId());
            steamUser.getRecentlyPlayed().clear();
            steamUser.getRecentlyPlayed().addAll(recent);

            model.addAttribute("success", "Game library successfully updated!");
        } catch (Exception e) {
            logger.error("Failed to resync games", e);
            model.addAttribute("error", "Sync failed. Please try again.");
        }

        User dbUser = (User) session.getAttribute(SESSION_ATTR_DB_USER);
        List<Group> userGroups = new ArrayList<>();
        if (dbUser != null) {
            userGroups = sqlHandler.getGroupsByOwner(dbUser);
        }

        model.addAttribute("user", steamUser);
        model.addAttribute("groups", userGroups);
        model.addAttribute("gameCount", steamUser.getTotalGameCount());
        model.addAttribute("totalPlaytime", steamUser.getTotalPlaytime());
        model.addAttribute("recentGames", steamUser.getRecentlyPlayed());
        model.addAttribute("topGames", getTopGames(steamUser.getOwnedGames(), 5));

        return "dashboard";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        SteamUser steamUser = (SteamUser) session.getAttribute(SESSION_ATTR_STEAM_USER);
        if (steamUser != null) {
            authService.invalidateSession(steamUser.getSessionToken());
        }
        session.invalidate();
        return "redirect:/";
    }

    private List<SteamGame> getTopGames(List<SteamGame> games, int count) {
        if (games == null) return new ArrayList<>();
        return games.stream()
                .sorted(Comparator.comparingInt(SteamGame::getPlaytimeForever).reversed())
                .limit(count)
                .collect(Collectors.toList());
    }
}