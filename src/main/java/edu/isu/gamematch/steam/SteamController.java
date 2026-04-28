package edu.isu.gamematch.steam;

import edu.isu.gamematch.*;
import edu.isu.gamematch.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.Session;
import org.hibernate.query.Query;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.*;
import java.util.Objects;
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
    private SQLHandler sqlHandler;

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

    @GetMapping("/groups/create")
public String showCreateGroupForm(HttpSession session) {
    SteamUser steamUser = (SteamUser) session.getAttribute(SESSION_ATTR_STEAM_USER);
    if (steamUser == null || !steamUser.isAuthenticated()) {
        return "redirect:/";
    }
    return "redirect:/dashboard";  // Dashboard already has the create form/modal
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

            // FIX 4.2 + 4.3: Import library and friends to database on login
            importFullLibraryToDb(steamId, dbUser);
            importFriendListToDb(steamId, dbUser);

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
    Map<Integer, Integer> groupMemberCounts = new HashMap<>();
    
    if (dbUser != null) {
        Session hibernateSession = HibernateUtil.getSessionFactory().openSession();
        try {
            Query<Group> query = hibernateSession.createQuery(
                "FROM Group g LEFT JOIN FETCH g.members WHERE g.groupOwner = :owner", Group.class);
            query.setParameter("owner", dbUser);
            userGroups = query.list();
            
            // Count members per group
            for (Group g : userGroups) {
                groupMemberCounts.put(g.getGroupID(), g.getMembers().size());
            }
        } finally {
            hibernateSession.close();
        }
    }

    model.addAttribute("user", steamUser);
    model.addAttribute("groups", userGroups);
    model.addAttribute("groupMemberCounts", groupMemberCounts);
    model.addAttribute("gameCount", steamUser.getTotalGameCount());
    model.addAttribute("totalPlaytime", steamUser.getTotalPlaytime());
    model.addAttribute("recentGames", steamUser.getRecentlyPlayed());
    model.addAttribute("topGames", getTopGames(steamUser.getOwnedGames(), 5));

    return "dashboard";
}

    @PostMapping("/groups/create")
    public String createGroup(@RequestParam String groupName, HttpSession session, Model model) {
        SteamUser steamUser = (SteamUser) session.getAttribute(SESSION_ATTR_STEAM_USER);
        User dbUser = (User) session.getAttribute(SESSION_ATTR_DB_USER);
        
        if (steamUser == null || dbUser == null) {
            return "redirect:/";
        }
        
        Group group = new Group();
        group.setGroupName(groupName);
        group.setGroupOwner(dbUser);
        group.addGroupMember(dbUser);
        
        boolean success = sqlHandler.createGroup(group);
        
        if (success) {
            model.addAttribute("success", "Group '" + groupName + "' created successfully!");
        } else {
            model.addAttribute("error", "Failed to create group. Please try again.");
        }
        
        return "redirect:/dashboard";
    }

    @PostMapping("/resync")
    public String resyncGames(HttpSession session, Model model) {
        SteamUser steamUser = (SteamUser) session.getAttribute(SESSION_ATTR_STEAM_USER);
        User dbUser = (User) session.getAttribute(SESSION_ATTR_DB_USER);
        if (steamUser == null || !steamUser.isAuthenticated()) {
            return "redirect:/";
        }

        try {
            // Update in-memory session data
            List<SteamGame> updatedGames = apiService.fetchOwnedGames(steamUser.getSteamId());
            steamUser.getOwnedGames().clear();
            updatedGames.forEach(steamUser::addGame);

            List<SteamGame> recent = apiService.fetchRecentlyPlayedGames(steamUser.getSteamId());
            steamUser.getRecentlyPlayed().clear();
            steamUser.getRecentlyPlayed().addAll(recent);

            // FIX 4.2: Persist to database
            if (dbUser != null) {
                importFullLibraryToDb(steamUser.getSteamId(), dbUser);
                importFriendListToDb(steamUser.getSteamId(), dbUser);
            }

            model.addAttribute("success", "Game library successfully updated!");
        } catch (Exception e) {
            logger.error("Failed to resync games", e);
            model.addAttribute("error", "Sync failed. Please try again.");
        }

        // Refresh dashboard data
        User dbUserRefreshed = (User) session.getAttribute(SESSION_ATTR_DB_USER);
        List<Group> userGroups = new ArrayList<>();
        if (dbUserRefreshed != null) {
            userGroups = sqlHandler.getGroupsByOwner(dbUserRefreshed);
        }

        model.addAttribute("user", steamUser);
        model.addAttribute("groups", userGroups);
        model.addAttribute("gameCount", steamUser.getTotalGameCount());
        model.addAttribute("totalPlaytime", steamUser.getTotalPlaytime());
        model.addAttribute("recentGames", steamUser.getRecentlyPlayed());
        model.addAttribute("topGames", getTopGames(steamUser.getOwnedGames(), 5));

        return "dashboard";
    }

    // FIX 3.1.14: Resync installed games placeholder
    @PostMapping("/resync-installed")
    public String resyncInstalled(@RequestParam(value = "file", required = false) MultipartFile file,
                                  HttpSession session, Model model) {
        SteamUser steamUser = (SteamUser) session.getAttribute(SESSION_ATTR_STEAM_USER);
        if (steamUser == null || !steamUser.isAuthenticated()) {
            return "redirect:/";
        }
        logger.info("Resync installed requested - falling back to API resync");
        return resyncGames(session, model);
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

    // ==================== PRIVATE HELPERS ====================

    /**
     * FIX 4.2: Persists Steam game library to database.
     */
    private void importFullLibraryToDb(String steamId, User dbUser) {
    Session session = HibernateUtil.getSessionFactory().openSession();
    Transaction tx = null;
    try {
        tx = session.beginTransaction();
        
        // Re-attach the user to this session
        User attachedUser = session.get(User.class, dbUser.getUserID());
        if (attachedUser == null) {
            attachedUser = dbUser;
            session.save(attachedUser);
        }
        
        List<SteamGame> steamGames = apiService.fetchOwnedGames(steamId);
        if (steamGames == null || steamGames.isEmpty()) {
            tx.commit();
            return;
        }

        // Pre-load existing achievement game IDs
        Set<Integer> existingGameIds = attachedUser.getAchievementData().stream()
                .map(GameAchievement::getGame)
                .filter(Objects::nonNull)
                .map(Game::getGameID)
                .collect(Collectors.toSet());

        for (SteamGame sg : steamGames) {
            Game game = session.createQuery("FROM Game WHERE gameName = :name", Game.class)
                    .setParameter("name", sg.getName())
                    .uniqueResult();
            
            if (game == null) {
                game = new Game();
                game.setGameName(sg.getName());
                game.setPlaytime(sg.getPlaytimeForever());
                game.setSteamAppURL("https://store.steampowered.com/app/" + sg.getAppId());
                session.save(game);
            } else {
                game.setPlaytime(sg.getPlaytimeForever());
                session.update(game);
            }

            if (!existingGameIds.contains(game.getGameID())) {
                Achievement placeholder = session.createQuery("FROM Achievement WHERE achievementName = 'Game Owned'", Achievement.class)
                        .uniqueResult();
                if (placeholder == null) {
                    placeholder = new Achievement("Game Owned", "Player owns this game on Steam");
                    session.save(placeholder);
                }
                GameAchievement ga = new GameAchievement(game, placeholder, attachedUser);
                session.save(ga);
                existingGameIds.add(game.getGameID());
            }
        }
        
        session.update(attachedUser);
        tx.commit();
        logger.info("Persisted {} games for user {}", steamGames.size(), steamId);
    } catch (Exception e) {
        if (tx != null) tx.rollback();
        logger.error("Error persisting game library for {}", steamId, e);
    } finally {
        session.close();
    }
}

    /**
     * FIX 4.3: Imports Steam friend list to database.
     */
    private void importFriendListToDb(String steamId, User dbUser) {
    Session session = HibernateUtil.getSessionFactory().openSession();
    Transaction tx = null;
    try {
        tx = session.beginTransaction();
        
        User attachedUser = session.get(User.class, dbUser.getUserID());
        if (attachedUser == null) {
            attachedUser = dbUser;
            session.save(attachedUser);
        }
        
        List<SteamUser> steamFriends = apiService.fetchFriendList(steamId);
        if (steamFriends == null) {
            tx.commit();
            return;
        }

        attachedUser.getFriends().clear();
        for (SteamUser sf : steamFriends) {
            User friend = session.createQuery("FROM User WHERE steamID = :sid", User.class)
                    .setParameter("sid", Long.parseLong(sf.getSteamId()))
                    .uniqueResult();
            if (friend == null) {
                friend = new User();
                friend.setSteamID(Long.parseLong(sf.getSteamId()));
                friend.setUserProfile(new UserProfile(sf.getPersonaName(), friend));
                session.save(friend);
            }
            attachedUser.getFriends().add(friend);
        }
        
        session.update(attachedUser);
        tx.commit();
        logger.info("Imported {} friends for user {}", steamFriends.size(), steamId);
    } catch (Exception e) {
        if (tx != null) tx.rollback();
        logger.error("Error importing friend list for {}", steamId, e);
    } finally {
        session.close();
    }
}

    private List<SteamGame> getTopGames(List<SteamGame> games, int count) {
        if (games == null) return new ArrayList<>();
        return games.stream()
                .sorted(Comparator.comparingInt(SteamGame::getPlaytimeForever).reversed())
                .limit(count)
                .collect(Collectors.toList());
    }
}