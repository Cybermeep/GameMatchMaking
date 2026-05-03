package edu.isu.gamematch.steam;

import edu.isu.gamematch.*;
import edu.isu.gamematch.service.UserService;
import edu.isu.gamematch.service.OnlineUserTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.Hibernate;
import org.hibernate.query.Query;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.*;
import java.util.Objects;
import java.util.stream.Collectors;
import edu.isu.gamematch.steam.SteamAchievement;
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

    @Autowired
    private OnlineUserTracker onlineTracker;

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

   @PostMapping("/resync-achievements")
public String resyncAchievements(HttpSession session, Model model) {
    User dbUser = (User) session.getAttribute(SESSION_ATTR_DB_USER);
    SteamUser steamUser = (SteamUser) session.getAttribute(SESSION_ATTR_STEAM_USER);
    if (dbUser == null || steamUser == null || !steamUser.isAuthenticated()) {
        return "redirect:/";
    }

    Session hs = HibernateUtil.getSessionFactory().openSession();
    Transaction tx = null;
    try {
        tx = hs.beginTransaction();
        User attachedUser = hs.get(User.class, dbUser.getUserID());
        Hibernate.initialize(attachedUser.getAchievementData());

        List<Game> games = attachedUser.getAchievementData().stream()
                .map(GameAchievement::getGame)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        int imported = 0;

        for (Game game : games) {
            String appId = game.getAppId();
            if (appId == null || appId.isEmpty() || !game.isHasAchievements()) continue;

            List<SteamAchievement> steamAchievements =
                    apiService.fetchPlayerAchievements(steamUser.getSteamId(), appId);
            if (steamAchievements.isEmpty()) continue;

            for (SteamAchievement sa : steamAchievements) {
                // Use display name if available; otherwise fallback to apiName
                String achName = sa.getName();
                if (achName == null || achName.trim().isEmpty()) {
                    achName = sa.getApiName();
                }
                if (achName == null || achName.trim().isEmpty()) {
                    logger.warn("Skipping achievement with no name/apiName for game {}", game.getGameName());
                    continue;
                }

                Achievement achievement = hs.createQuery(
                        "FROM Achievement WHERE achievementName = :name", Achievement.class)
                        .setParameter("name", achName)
                        .uniqueResult();
                if (achievement == null) {
                    achievement = new Achievement(achName,
                            sa.getDescription() != null ? sa.getDescription() : "");
                    hs.save(achievement);
                }

                boolean exists = false;
                for (GameAchievement existingGa : attachedUser.getAchievementData()) {
                    if (existingGa.getGame() != null && existingGa.getAchievement() != null
                            && existingGa.getGame().equals(game)
                            && existingGa.getAchievement().equals(achievement)) {
                        exists = true;
                        break;
                    }
                }

                if (!exists) {
                    GameAchievement ga = new GameAchievement(game, achievement, attachedUser);
                    hs.save(ga);
                    imported++;
                }
            }
        }

        tx.commit();
        model.addAttribute("success", "Imported " + imported + " new achievements.");
    } catch (Exception e) {
        if (tx != null) tx.rollback();
        logger.error("Achievement sync failed", e);
        model.addAttribute("error", "Achievement sync failed.");
    } finally {
        hs.close();
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

            SteamUser steamUser = apiService.fetchCompleteUserData(steamId);
            if (steamUser == null) {
                model.addAttribute("error", "Could not retrieve Steam profile. Check privacy settings.");
                return "index";
            }

            User dbUser = userService.findOrCreateUser(steamId, steamUser.getPersonaName());

            String sessionToken = authService.createSession(steamUser);
            session.setAttribute(SESSION_ATTR_STEAM_USER, steamUser);
            session.setAttribute(SESSION_ATTR_DB_USER, dbUser);
            session.setAttribute("sessionToken", sessionToken);

            onlineTracker.userLoggedIn(dbUser.getUserID());

            importFullLibraryToDb(steamId, dbUser);
            importFriendListToDb(steamId, dbUser);

            logger.info("User logged in: {} ({})", steamUser.getPersonaName(), steamId);
            return "redirect:/dashboard";

        } catch (Exception e) {
    e.printStackTrace();   // ADD THIS LINE
    logger.error("Error during Steam callback", e);
    model.addAttribute("error", "An unexpected error occurred. Please try again.");
    return "index";
}
    }

    @GetMapping("/dashboard")
public String dashboard(HttpSession session, Model model) {
    SteamUser steamUser = (SteamUser) session.getAttribute(SESSION_ATTR_STEAM_USER);
    User dbUser = (User) session.getAttribute(SESSION_ATTR_DB_USER);

    boolean isSteam = (steamUser != null && steamUser.isAuthenticated());
    boolean isLocal = (dbUser != null && steamUser == null);

    if (!isSteam && !isLocal) {
        return "redirect:/";
    }

    List<Group> userGroups = new ArrayList<>();
    Map<Integer, Integer> groupMemberCounts = new HashMap<>();
    List<User> friends = new ArrayList<>();
    List<Game> manualGames = Collections.emptyList();   // omitted for performance

    // Statistics for the view
    int gameCount = 0;
    int totalPlaytime = 0;
    List<SteamGame> recentGames = Collections.emptyList();
    List<SteamGame> topGames = Collections.emptyList();

    if (dbUser != null) {
        Session hibernateSession = HibernateUtil.getSessionFactory().openSession();
        try {
            // Load user's groups
            Query<Group> query = hibernateSession.createQuery(
                "SELECT g FROM Group g JOIN g.members m WHERE m.userID = :uid", Group.class);
            query.setParameter("uid", dbUser.getUserID());
            userGroups = query.list();
            for (Group g : userGroups) {
                groupMemberCounts.put(g.getGroupID(), g.getMembers().size());
            }

            // Load friends
            User freshUser = hibernateSession.get(User.class, dbUser.getUserID());
            Hibernate.initialize(freshUser.getFriends());
            friends = freshUser.getFriends();
            for (User friend : friends) {
                Hibernate.initialize(friend.getUserProfile());
                friend.setOnline(onlineTracker.isOnline(friend.getUserID()));
            }

            // Lightweight game list & stats (avoids loading all achievements)
            List<Game> allUserGames = sqlHandler.getDistinctGamesByUser(freshUser);
            gameCount = allUserGames.size();
            totalPlaytime = allUserGames.stream().mapToInt(Game::getPlaytime).sum();
        } finally {
            hibernateSession.close();
        }
    }

    // Steam-specific statistics and recent/top games
    if (isSteam && steamUser != null) {
        gameCount = steamUser.getTotalGameCount();          // overrides with Steam count
        totalPlaytime = steamUser.getTotalPlaytime();        // overrides with Steam total
        recentGames = steamUser.getRecentlyPlayed();
        topGames = getTopGames(steamUser.getOwnedGames(), 5);
    }

    // Build the 'user' model attribute
    if (isSteam) {
        model.addAttribute("user", steamUser);
    } else {
        model.addAttribute("user", buildLocalUserModel(dbUser));
    }

    model.addAttribute("gameCount", gameCount);
    model.addAttribute("totalPlaytime", totalPlaytime);
    model.addAttribute("recentGames", recentGames);
    model.addAttribute("topGames", topGames);
    model.addAttribute("manualGames", manualGames);
    model.addAttribute("groups", userGroups);
    model.addAttribute("groupMemberCounts", groupMemberCounts);
    model.addAttribute("friends", friends);
    model.addAttribute("isSteam", isSteam);
    model.addAttribute("isLocal", isLocal);

    return "dashboard";
}

    @PostMapping("/groups/create")
    public String createGroup(@RequestParam String groupName, HttpSession session, Model model) {
        User dbUser = (User) session.getAttribute(SESSION_ATTR_DB_USER);
        if (dbUser == null) return "redirect:/";
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

        boolean isSteam = (steamUser != null && steamUser.isAuthenticated());

        if (!isSteam) return "redirect:/dashboard";
        try {
            List<SteamGame> updatedGames = apiService.fetchOwnedGames(steamUser.getSteamId());
            steamUser.getOwnedGames().clear();
            updatedGames.forEach(steamUser::addGame);
            List<SteamGame> recent = apiService.fetchRecentlyPlayedGames(steamUser.getSteamId());
            steamUser.getRecentlyPlayed().clear();
            steamUser.getRecentlyPlayed().addAll(recent);
            if (dbUser != null) {
                importFullLibraryToDb(steamUser.getSteamId(), dbUser);
                importFriendListToDb(steamUser.getSteamId(), dbUser);
            }
            model.addAttribute("success", "Game library successfully updated!");
        } catch (Exception e) {
            logger.error("Failed to resync games", e);
            model.addAttribute("error", "Sync failed. Please try again.");
        }
        // Rebuild dashboard model
        return dashboard(session, model);
    }

    @PostMapping("/resync-installed")
    public String resyncInstalled(@RequestParam(value = "file", required = false) MultipartFile file,
                                  HttpSession session, Model model) {
        SteamUser steamUser = (SteamUser) session.getAttribute(SESSION_ATTR_STEAM_USER);
        if (steamUser == null || !steamUser.isAuthenticated()) return "redirect:/";
        logger.info("Resync installed requested - falling back to API resync");
        return resyncGames(session, model);
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        SteamUser steamUser = (SteamUser) session.getAttribute(SESSION_ATTR_STEAM_USER);
        if (steamUser != null) {
            authService.invalidateSession(steamUser.getSessionToken());
        }
        User dbUser = (User) session.getAttribute(SESSION_ATTR_DB_USER);
        if (dbUser != null) onlineTracker.userLoggedOut(dbUser.getUserID());
        session.invalidate();
        return "redirect:/";
    }

    // ---------- Private helpers ----------
    private void importFullLibraryToDb(String steamId, User dbUser) {
    Session session = HibernateUtil.getSessionFactory().openSession();
    Transaction tx = null;
    try {
        tx = session.beginTransaction();
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
                game.setHasAchievements(sg.isHasAchievements());
                game.setPlaytime(sg.getPlaytimeForever());
                game.setSteamAppURL("https://store.steampowered.com/app/" + sg.getAppId());
                game.setAppId(sg.getAppId());                     //  store appId
                game.setGenre("Unknown");

                session.save(game);
            } else {
                // Update playtime and ensure appId is present
                game.setPlaytime(sg.getPlaytimeForever());
                if (game.getAppId() == null || game.getAppId().isEmpty()) {
                    game.setAppId(sg.getAppId());                 //  update existing games
                }
                session.update(game);
            }

            if (!existingGameIds.contains(game.getGameID())) {
                Achievement placeholder = session.createQuery(
                        "FROM Achievement WHERE achievementName = 'Game Owned'", Achievement.class)
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
            if (steamFriends == null) { tx.commit(); return; }
            attachedUser.getFriends().clear();
            for (SteamUser sf : steamFriends) {
                User friend = session.createQuery("FROM User WHERE steamID = :sid", User.class)
                        .setParameter("sid", Long.parseLong(sf.getSteamId()))
                        .uniqueResult();
                if (friend == null) {
                    friend = new User();
                    friend.setSteamID(Long.parseLong(sf.getSteamId()));
                    friend.setPersonaName(sf.getPersonaName());
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
        } finally { session.close(); }
    }

    private Map<String, Object> buildLocalUserModel(User dbUser) {
        Map<String, Object> localUser = new HashMap<>();
        localUser.put("personaName", dbUser.getPersonaName());
        localUser.put("steamId", "N/A");
        localUser.put("avatarUrl", "");
        localUser.put("profileUrl", "");
        return localUser;
    }

    private List<SteamGame> getTopGames(List<SteamGame> games, int count) {
        if (games == null) return new ArrayList<>();
        return games.stream()
                .sorted(Comparator.comparingInt(SteamGame::getPlaytimeForever).reversed())
                .limit(count)
                .collect(Collectors.toList());
    }
}