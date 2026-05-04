package edu.isu.gamematch.controller;

import edu.isu.gamematch.*;
import edu.isu.gamematch.export.ExportCSV;
import edu.isu.gamematch.service.GraphService;
import edu.isu.gamematch.service.OnlineUserTracker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.hibernate.Session;
import org.hibernate.Hibernate;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import javax.servlet.http.HttpSession;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.DayOfWeek;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.nio.charset.StandardCharsets;


@Controller
public class GameMatchController {

    private static final String SESSION_DB_USER = "db_user";

    @Autowired
    private SQLHandler sqlHandler;

    @Autowired
    private ExportCSV exportCSV;

    @Autowired
    private GraphService graphService;

    @Autowired
    private OnlineUserTracker onlineTracker;

    // ==================== GROUP HOME ====================
    @GetMapping("/groups/{id}/home")
    public String groupHome(@PathVariable int id, HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute(SESSION_DB_USER);
        if (currentUser == null) return "redirect:/";
        Session hs = HibernateUtil.getSessionFactory().openSession();
        Group group = null;
        try {
            group = hs.get(Group.class, id);
            if (group == null) return "redirect:/";
            Hibernate.initialize(group.getMembers());
            Hibernate.initialize(group.getGames());
            for (User member : group.getMembers()) {
                Hibernate.initialize(member.getUserProfile());
                member.setOnline(onlineTracker.isOnline(member.getUserID()));
            }
        } finally {
            hs.close();
        }
        model.addAttribute("group", group);
        model.addAttribute("currentUser", currentUser);
        return "group-home";
    }

    // ==================== GROUP GAMES (3.1.3, 3.1.20) ====================
   @GetMapping("/groups/{groupId}/games")
public String getFilteredGames(@PathVariable int groupId,
                               @RequestParam(defaultValue = "0") int minPlaytime,
                               @RequestParam(defaultValue = "false") boolean random,
                               HttpSession session, Model model) {
    User currentUser = (User) session.getAttribute("db_user");
    if (currentUser == null) return "redirect:/";

    Session hibernateSession = HibernateUtil.getSessionFactory().openSession();
    Group group = null;
    List<Game> sharedGames = new ArrayList<>();
    List<GroupPreference> groupTags = new ArrayList<>();
    try {
        group = hibernateSession.createQuery(
            "FROM Group g LEFT JOIN FETCH g.members WHERE g.groupID = :id", Group.class)
            .setParameter("id", groupId)
            .uniqueResult();
        if (group == null) {
            hibernateSession.close();
            return "redirect:/dashboard";
        }

        // Reload current user within this session and initialize profile + genres
        User freshUser = hibernateSession.get(User.class, currentUser.getUserID());
        Hibernate.initialize(freshUser.getUserProfile());
        if (freshUser.getUserProfile() != null) {
            Hibernate.initialize(freshUser.getUserProfile().getFavoriteGenres());
        }

        if (random) {
            Hibernate.initialize(group.getGames());
            sharedGames = new ArrayList<>(group.getGames());
        } else {
            GroupOperations ops = new GroupOperations();
            sharedGames = ops.getSharedGames(group, sqlHandler);

            int effectiveMinPlaytime = minPlaytime;
            if (effectiveMinPlaytime == 0 && group.getMinPlaytimeRequirement() > 0) {
                effectiveMinPlaytime = group.getMinPlaytimeRequirement();
            }
            if (effectiveMinPlaytime > 0) {
                final int filterMinutes = effectiveMinPlaytime;
                sharedGames = sharedGames.stream()
                        .filter(g -> g.getPlaytime() >= filterMinutes)
                        .collect(Collectors.toList());
            }
            // Use the fresh user (with loaded profile) for ranking
            ops.rankList(group, freshUser, sqlHandler);
        }

        // Remove any null entries
        sharedGames = sharedGames.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // Fetch group tags for genre assignment
        groupTags = hibernateSession.createQuery(
            "FROM GroupPreference WHERE group.groupID = :gid", GroupPreference.class)
            .setParameter("gid", groupId)
            .list();

    } finally {
        hibernateSession.close();
    }

    model.addAttribute("group", group);
    model.addAttribute("games", sharedGames);
    model.addAttribute("minPlaytime", minPlaytime);
    model.addAttribute("currentUser", currentUser);   // the session user is fine for rendering privileges
    model.addAttribute("groupTags", groupTags);
    return "group-games";
}


@PostMapping("/groups/{groupId}/update-game-genre")
public String updateGameGenre(@PathVariable int groupId,
                              @RequestParam int gameId,
                              @RequestParam String genre,
                              HttpSession session) {
    User currentUser = (User) session.getAttribute("db_user");
    if (currentUser == null) return "redirect:/";
    Session hs = HibernateUtil.getSessionFactory().openSession();
    Transaction tx = null;
    try {
        tx = hs.beginTransaction();
        Group group = hs.get(Group.class, groupId);
        if (group == null) { tx.commit(); return "redirect:/dashboard"; }
        Hibernate.initialize(group.getMembers());
        boolean isMember = group.getMembers().stream().anyMatch(m -> m.getUserID() == currentUser.getUserID());
        if (!isMember) { tx.commit(); return "redirect:/dashboard"; }

        Game game = hs.get(Game.class, gameId);
        if (game != null) {
            // Append the tag if not already present (comma separated)
            String currentGenre = game.getGenre();
            if (currentGenre == null || currentGenre.trim().isEmpty()) {
                game.setGenre(genre);
            } else {
                List<String> tags = new ArrayList<>(Arrays.asList(currentGenre.split("\\s*,\\s*")));
                if (!tags.contains(genre)) {
                    tags.add(genre);
                    game.setGenre(String.join(", ", tags));
                }
            }
            game.setGenre(game.getGenre().trim()); // remove accidental spaces
            hs.merge(game);
        }
        tx.commit();
    } catch (Exception e) {
        if (tx != null) tx.rollback();
    } finally { hs.close(); }
    return "redirect:/groups/" + groupId + "/games";
}

// New: remove a single tag from a game's genre
@PostMapping("/groups/{groupId}/remove-game-genre")
public String removeGameGenre(@PathVariable int groupId,
                              @RequestParam int gameId,
                              @RequestParam String genre,
                              HttpSession session) {
    // Similar permission check as above
    User currentUser = (User) session.getAttribute("db_user");
    if (currentUser == null) return "redirect:/";
    Session hs = HibernateUtil.getSessionFactory().openSession();
    Transaction tx = null;
    try {
        tx = hs.beginTransaction();
        Group group = hs.get(Group.class, groupId);
        if (group == null) { tx.commit(); return "redirect:/dashboard"; }
        Hibernate.initialize(group.getMembers());
        boolean isMember = group.getMembers().stream().anyMatch(m -> m.getUserID() == currentUser.getUserID());
        if (!isMember) { tx.commit(); return "redirect:/dashboard"; }

        Game game = hs.get(Game.class, gameId);
        if (game != null && game.getGenre() != null) {
            List<String> tags = new ArrayList<>(Arrays.asList(game.getGenre().split("\\s*,\\s*")));
            if (tags.remove(genre)) {
                game.setGenre(tags.isEmpty() ? "" : String.join(", ", tags));
                hs.merge(game);
            }
        }
        tx.commit();
    } catch (Exception e) {
        if (tx != null) tx.rollback();
    } finally { hs.close(); }
    return "redirect:/groups/" + groupId + "/games";
}

    // ==================== GROUP MEMBERS (3.1.8) ====================
    @GetMapping("/groups/{groupId}/members")
    public String getGroupMembers(@PathVariable int groupId, HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute(SESSION_DB_USER);
        if (currentUser == null) return "redirect:/";
        Session hibernateSession = HibernateUtil.getSessionFactory().openSession();
        Group group = null;
        try {
            group = hibernateSession.get(Group.class, groupId);
            if (group == null) return "redirect:/";
            Hibernate.initialize(group.getMembers());
            for (User member : group.getMembers()) {
                Hibernate.initialize(member.getUserProfile());
                member.setOnline(onlineTracker.isOnline(member.getUserID()));
            }
        } finally {
            hibernateSession.close();
        }
        model.addAttribute("group", group);
        model.addAttribute("members", group.getMembers());
        model.addAttribute("currentUser", currentUser);
        return "group-members";
    }

    // ==================== JOIN GROUP VIA INVITE (3.1.9) ====================
    @GetMapping("/groups/join/{inviteToken}")
public String joinGroupViaInvite(@PathVariable String inviteToken, HttpSession session, Model model) {
    User currentUser = (User) session.getAttribute(SESSION_DB_USER);
    if (currentUser == null) return "redirect:/";
    Session hs = HibernateUtil.getSessionFactory().openSession();
    Transaction tx = null;
    try {
        tx = hs.beginTransaction();
        GroupJoinRequest joinReq = hs.createQuery(
            "FROM GroupJoinRequest WHERE inviteToken = :token AND status = 'PENDING'",
            GroupJoinRequest.class)
            .setParameter("token", inviteToken)
            .uniqueResult();
        if (joinReq == null) {
            tx.commit();
            model.addAttribute("error", "Invalid or expired invite link.");
            return "redirect:/dashboard";
        }
        Group group = joinReq.getGroup();
        if (group == null) {
            tx.commit();
            model.addAttribute("error", "Group no longer exists.");
            return "redirect:/dashboard";
        }
        // Get the attached user inside the same Hibernate session
        User attachedUser = hs.get(User.class, currentUser.getUserID());
        if (attachedUser == null) {
            tx.commit();
            model.addAttribute("error", "User not found.");
            return "redirect:/dashboard";
        }
        if (group.getMembers().contains(attachedUser)) {
            tx.commit();
            model.addAttribute("error", "You are already a member.");
            return "redirect:/dashboard";
        }
        GroupJoinRequest newReq = new GroupJoinRequest(group, attachedUser, null);
        newReq.setStatus("PENDING");
        hs.save(newReq);
        tx.commit();
        model.addAttribute("success", "Join request sent to group owner.");
    } catch (Exception e) {
        if (tx != null) tx.rollback();
        model.addAttribute("error", "Something went wrong: " + e.getMessage());
    } finally {
        hs.close();
    }
    return "redirect:/dashboard";
}

@PostMapping("/groups/join")
public String joinGroupWithToken(@RequestParam String token, HttpSession session, Model model) {
    // If the user pastes the full invite URL, extract the actual token
    String cleanToken = token.trim();
    if (cleanToken.startsWith("http://") || cleanToken.startsWith("https://")) {
        int lastSlash = cleanToken.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash < cleanToken.length() - 1) {
            cleanToken = cleanToken.substring(lastSlash + 1);
        }
        // Also remove any query parameters
        int queryStart = cleanToken.indexOf('?');
        if (queryStart >= 0) cleanToken = cleanToken.substring(0, queryStart);
    }

    // Redirect to the existing logic that processes the token
    return "redirect:/groups/join/" + cleanToken;
}

    // ==================== LEAVE GROUP (3.1.10) ====================
    @PostMapping("/groups/{groupId}/leave")
public String leaveGroup(@PathVariable int groupId, HttpSession session) {
    User currentUser = (User) session.getAttribute(SESSION_DB_USER);
    if (currentUser == null) return "redirect:/";

    Session hs = HibernateUtil.getSessionFactory().openSession();
    Transaction tx = null;
    try {
        tx = hs.beginTransaction();
        Group group = hs.get(Group.class, groupId);
        if (group == null) {
            tx.commit();
            return "redirect:/dashboard";
        }
        Hibernate.initialize(group.getMembers());
        if (group.getMembers().removeIf(m -> m.getUserID() == currentUser.getUserID())) {
            hs.merge(group);
        }
        tx.commit();
    } catch (Exception e) {
        if (tx != null) tx.rollback();
    } finally {
        hs.close();
    }
    return "redirect:/dashboard";
}

    private UserProfile getProfileOrNull(User user) {
    if (user.getUserProfile() == null) return null;
    // initialize lazy favoriteGenres inside an open session if needed
    return user.getUserProfile();
}


    // ==================== PENDING JOIN REQUESTS (3.1.19) ====================
    @GetMapping("/groups/{groupId}/requests/pending")
public String getPendingJoinRequests(@PathVariable int groupId, HttpSession session, Model model) {
    User currentUser = (User) session.getAttribute(SESSION_DB_USER);
    if (currentUser == null) return "redirect:/";

    Session hibSession = HibernateUtil.getSessionFactory().openSession();
    try {
        Group group = hibSession.get(Group.class, groupId);
        if (group == null || group.getGroupOwner().getUserID() != currentUser.getUserID()) {
            return "redirect:/";
        }

        Hibernate.initialize(group.getJoinRequests());
        List<GroupJoinRequest> pending = group.getJoinRequests().stream()
                .filter(r -> "PENDING".equals(r.getStatus()) && r.getRequestingUser() != null)
                .collect(Collectors.toList());

        for (GroupJoinRequest req : pending) {
            Hibernate.initialize(req.getRequestingUser());
            Hibernate.initialize(req.getRequestingUser().getUserProfile());
        }

        model.addAttribute("group", group);
        model.addAttribute("requests", pending);
        model.addAttribute("currentUser", currentUser);
        return "pending-requests";
    } finally {
        hibSession.close();
    }
}

    // ==================== ACCEPT JOIN REQUEST (3.1.17) ====================
    @PostMapping("/groups/{groupId}/requests/{requestId}/accept")
public String acceptJoinRequest(@PathVariable int groupId, @PathVariable int requestId, HttpSession session) {
    User currentUser = (User) session.getAttribute(SESSION_DB_USER);
    if (currentUser == null) return "redirect:/";

    Session hibSession = HibernateUtil.getSessionFactory().openSession();
    Transaction tx = null;
    try {
        tx = hibSession.beginTransaction();
        Group group = hibSession.get(Group.class, groupId);
        if (group == null || group.getGroupOwner().getUserID() != currentUser.getUserID()) {
            tx.commit(); // nothing to do
            return "redirect:/";
        }

        GroupJoinRequest req = hibSession.get(GroupJoinRequest.class, requestId);
        if (req != null && req.getGroup().getGroupID() == groupId && "PENDING".equals(req.getStatus())) {
            req.setStatus("ACCEPTED");
            hibSession.merge(req);

            // Add the requesting user to the group
            User newMember = req.getRequestingUser();
            if (newMember != null && !group.getMembers().contains(newMember)) {
                group.getMembers().add(newMember);
            }
            hibSession.merge(group);
            tx.commit();
        } else {
            tx.commit(); // no valid request
        }
    } catch (Exception e) {
        if (tx != null) tx.rollback();
        e.printStackTrace();
    } finally {
        hibSession.close();
    }
    return "redirect:/groups/" + groupId + "/requests/pending";
}

    // ==================== DECLINE JOIN REQUEST (3.1.18) ====================
    @PostMapping("/groups/{groupId}/requests/{requestId}/decline")
    public String declineJoinRequest(@PathVariable int groupId, @PathVariable int requestId, HttpSession session) {
        User currentUser = (User) session.getAttribute(SESSION_DB_USER);
        Group group = sqlHandler.getGroupById(groupId);
        if (group == null || group.getGroupOwner().getUserID() != currentUser.getUserID()) return "redirect:/";
        GroupJoinRequest req = sqlHandler.getGroupJoinRequestById(requestId);
        if (req != null && req.getGroup().getGroupID() == groupId) {
            req.setStatus("DECLINED");
            sqlHandler.updateGroupJoinRequest(req);
        }
        return "redirect:/groups/" + groupId + "/requests/pending";
    }

    // ==================== VOTE PAGE (FIXED lazy init of favoriteGenres) ====================
@GetMapping("/groups/{groupId}/vote")
public String showVotePage(@PathVariable int groupId, HttpSession session, Model model) {
    User currentUser = (User) session.getAttribute(SESSION_DB_USER);
    if (currentUser == null) return "redirect:/";
    Session hibernateSession = HibernateUtil.getSessionFactory().openSession();
    Group group = null;
    List<Game> games = new ArrayList<>();
    try {
        group = hibernateSession.get(Group.class, groupId);
        if (group == null) return "redirect:/";
        Hibernate.initialize(group.getMembers());
        Hibernate.initialize(group.getGames());
        for (User member : group.getMembers()) {
            Hibernate.initialize(member.getAchievementData());
        }
        // Refresh current user from session so its profile/genres are loaded
        User freshCurrent = hibernateSession.get(User.class, currentUser.getUserID());
        Hibernate.initialize(freshCurrent.getUserProfile());
        if (freshCurrent.getUserProfile() != null) {
            Hibernate.initialize(freshCurrent.getUserProfile().getFavoriteGenres());
        }
        session.setAttribute(SESSION_DB_USER, freshCurrent);
        currentUser = freshCurrent;

        if (group.isVotingClosed()) {
            model.addAttribute("error", "Voting is closed.");
            return "redirect:/groups/" + groupId + "/vote/results";
        }
        GroupOperations ops = new GroupOperations();
        games = ops.getSharedGames(group, sqlHandler);
        int minPlaytime = group.getMinPlaytimeRequirement();
        if (minPlaytime > 0) {
            games = games.stream().filter(g -> g.getPlaytime() >= minPlaytime).collect(Collectors.toList());
        }
        UserProfile profile = currentUser.getUserProfile();
        List<String> favoriteGenres = (profile != null) ? profile.getFavoriteGenres() : new ArrayList<>();
        games.sort(Comparator.<Game>comparingDouble(game -> {
            double base = game.getPlaytime() / 60.0;
            double bonus = favoriteGenres.contains(game.getGenre()) ? 10.0 : 0.0;
            return base + bonus;
        }).reversed().thenComparing(Game::getGameName));
    } finally {
        hibernateSession.close();
    }
    model.addAttribute("group", group);
    model.addAttribute("games", games);
    model.addAttribute("currentUser", currentUser);
    return "vote";
}

    @PostMapping("/groups/{groupId}/vote")
    public String castVote(@PathVariable int groupId, @RequestParam int gameId, HttpSession session) {
        User currentUser = (User) session.getAttribute(SESSION_DB_USER);
        if (currentUser == null) return "redirect:/";
        Group group = sqlHandler.getGroupById(groupId);
        Game game = sqlHandler.getGameById(gameId);
        if (group == null || game == null || group.isVotingClosed()) return "redirect:/groups/" + groupId;
        List<GroupVote> existingVotes = sqlHandler.getVotesByUser(currentUser);
        GroupVote existing = existingVotes.stream()
                .filter(v -> v.getGroup().getGroupID() == groupId)
                .findFirst().orElse(null);
        if (existing == null) {
            GroupVote vote = new GroupVote(group, currentUser);
            vote.castVote(game);
            sqlHandler.createGroupVote(vote);
        } else {
            existing.updateVote(game);
            sqlHandler.updateGroupVote(existing);
        }
        return "redirect:/groups/" + groupId + "/vote/results";
    }

    @GetMapping("/groups/{groupId}/vote/results")
    public String voteResults(@PathVariable int groupId, Model model) {
        Group group = sqlHandler.getGroupById(groupId);
        if (group == null) return "redirect:/";
        List<GroupVote> votes = sqlHandler.getGroupVotesByGroup(group);
        Map<Game, Integer> tally = GroupVote.tallyVotes(votes);
        Game winner = GroupVote.getWinner(tally);
        model.addAttribute("group", group);
        model.addAttribute("tally", tally);
        model.addAttribute("winner", winner);
        return "vote-results";
    }

    @PostMapping("/groups/{groupId}/decide-vote")
    public String decideVote(@PathVariable int groupId, HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute("db_user");
        if (currentUser == null) return "redirect:/";
        Session hs = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = null;
        try {
            tx = hs.beginTransaction();
            Group group = hs.get(Group.class, groupId);
            if (group == null || group.getGroupOwner().getUserID() != currentUser.getUserID())
                return "redirect:/";
            List<GroupVote> votes = group.getVotes();
            Map<Game, Integer> tally = GroupVote.tallyVotes(votes);
            Game winner = GroupVote.getWinner(tally);
            group.setVotingClosed(true);
            hs.merge(group);
            tx.commit();
            model.addAttribute("group", group);
            model.addAttribute("tally", tally);
            model.addAttribute("winner", winner);
        } catch (Exception e) {
            if (tx != null) tx.rollback();
        } finally { hs.close(); }
        return "vote-results";
    }

    // ==================== USER PROFILE (3.1.15) ====================
    @GetMapping("/users/{steamId}")
    public String getUserProfile(@PathVariable String steamId, HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute(SESSION_DB_USER);
        Session hs = HibernateUtil.getSessionFactory().openSession();
        try {
            User viewedUser = hs.createQuery("FROM User WHERE steamID = :sid", User.class)
                    .setParameter("sid", Long.parseLong(steamId))
                    .uniqueResult();
            if (viewedUser == null) {
                return "redirect:/dashboard";
            }
            Hibernate.initialize(viewedUser.getUserProfile());
            Hibernate.initialize(viewedUser.getAchievementData());
           
            model.addAttribute("viewedUser", viewedUser);
            if (currentUser != null && !currentUser.equals(viewedUser)) {
                List<User> mutual = new ArrayList<>();
                for (User friend : currentUser.getFriends()) {
                    if (viewedUser.getFriends().contains(friend)) mutual.add(friend);
                }
                model.addAttribute("mutualFriends", mutual);
            }
        } finally {
            hs.close();
        }
        return "user-profile";
    }

    @GetMapping("/users/by-id")
    public String getUserByLocalId(@RequestParam int userId, Model model) {
        Session hs = HibernateUtil.getSessionFactory().openSession();
        try {
            User user = hs.get(User.class, userId);
            if (user == null) return "redirect:/dashboard";
            Hibernate.initialize(user.getUserProfile());
            Hibernate.initialize(user.getAchievementData());
            for (GameAchievement ga : user.getAchievementData()) {
                Hibernate.initialize(ga.getGame());
            }
            model.addAttribute("viewedUser", user);
            return "user-profile";
        } finally {
            hs.close();
        }
    }

    // ==================== SEARCH USER (3.1.21) ====================
    @GetMapping("/users/search")
    public String searchUsers(@RequestParam String q, Model model) {
        List<User> results = sqlHandler.searchUsersByPersonaName(q);
        Session hs = HibernateUtil.getSessionFactory().openSession();
        try {
            for (User u : results) {
                Hibernate.initialize(u.getUserProfile());
            }
        } finally { hs.close(); }
        model.addAttribute("results", results);
        model.addAttribute("query", q);
        return "search-results";
    }

    // ==================== COMPARE ACHIEVEMENTS (3.1.22a) ====================
    @GetMapping("/groups/{groupId}/achievements")
    public String compareAchievements(@PathVariable int groupId, Model model) {
        Session hibernateSession = HibernateUtil.getSessionFactory().openSession();
        Group group = null;
        Map<User, List<Achievement>> userAchievements = new HashMap<>();
        try {
            group = hibernateSession.get(Group.class, groupId);
            if (group == null) return "redirect:/";
            Hibernate.initialize(group.getMembers());
            for (User member : group.getMembers()) {
                Hibernate.initialize(member.getUserProfile());
                Hibernate.initialize(member.getAchievementData());
                List<Achievement> achievements = member.getAchievementData().stream()
                        .map(GameAchievement::getAchievement)
                        .collect(Collectors.toList());
                userAchievements.put(member, achievements);
            }
        } finally { hibernateSession.close(); }
        model.addAttribute("group", group);
        model.addAttribute("userAchievements", userAchievements);
        return "achievement-comparison";
    }

    @GetMapping("/groups/join")
public String joinGroupByToken(@RequestParam String token) {
    return "redirect:/groups/join/" + token;
}

    // ==================== GENRE TAGS (Fixed) ====================
@GetMapping("/profile/genres")
public String showGenreTags(HttpSession session, Model model) {
    User sessionUser = (User) session.getAttribute(SESSION_DB_USER);
    if (sessionUser == null) return "redirect:/";
    Session hs = HibernateUtil.getSessionFactory().openSession();
    try {
        User user = hs.get(User.class, sessionUser.getUserID());
        Hibernate.initialize(user.getUserProfile());
        if (user.getUserProfile() != null) {
            Hibernate.initialize(user.getUserProfile().getFavoriteGenres());
        }
        session.setAttribute(SESSION_DB_USER, user);
        if (user.getUserProfile() == null) {
            model.addAttribute("error", "Profile not set up yet.");
            return "redirect:/dashboard";
        }
        List<String> available = Arrays.asList("Action","RPG","Strategy","Shooter","Adventure",
                                               "Puzzle","Simulation","Sports","Racing","Fighting");
        model.addAttribute("availableGenres", available);
        model.addAttribute("userGenres", user.getUserProfile().getFavoriteGenres());
        return "genre-tags";
    } finally { hs.close(); }
}

    // ==================== FRIEND MANAGEMENT ====================
@PostMapping("/users/add-friend")
public String addFriend(@RequestParam int friendUserId, HttpSession session, Model model) {
    User currentUser = (User) session.getAttribute(SESSION_DB_USER);
    if (currentUser == null) return "redirect:/";
    Session hs = HibernateUtil.getSessionFactory().openSession();
    Transaction tx = null;
    try {
        tx = hs.beginTransaction();
        User me = hs.get(User.class, currentUser.getUserID());
        User friend = hs.get(User.class, friendUserId);
        if (friend != null && !me.getFriends().contains(friend) && me.getUserID() != friend.getUserID()) {
            me.addFriend(friend);
            friend.addFriend(me);
            hs.merge(me);
            hs.merge(friend);
            tx.commit();
            model.addAttribute("success", "Friend added!");
        } else {
            model.addAttribute("error", "Already friends or invalid user.");
        }
    } catch (Exception e) {
        if (tx != null) tx.rollback();
    } finally { hs.close(); }
    return "redirect:/dashboard";
}

   @PostMapping("/profile/genres/add")
public String addGenreTag(@RequestParam String genre, HttpSession session) {
    User sessionUser = (User) session.getAttribute(SESSION_DB_USER);
    if (sessionUser == null) return "redirect:/";
    Session hs = HibernateUtil.getSessionFactory().openSession();
    Transaction tx = null;
    try {
        tx = hs.beginTransaction();
        User user = hs.get(User.class, sessionUser.getUserID());
        Hibernate.initialize(user.getUserProfile());
        if (user.getUserProfile() != null &&
            !user.getUserProfile().getFavoriteGenres().contains(genre)) {
            user.getUserProfile().getFavoriteGenres().add(genre);
            hs.merge(user);
        }
        tx.commit();
    } catch (Exception e) { if (tx != null) tx.rollback(); }
    finally { hs.close(); }
    return "redirect:/profile/genres";
}

@PostMapping("/profile/genres/remove")
public String removeGenreTag(@RequestParam String genre, HttpSession session) {
    User sessionUser = (User) session.getAttribute(SESSION_DB_USER);
    if (sessionUser == null) return "redirect:/";
    Session hs = HibernateUtil.getSessionFactory().openSession();
    Transaction tx = null;
    try {
        tx = hs.beginTransaction();
        User user = hs.get(User.class, sessionUser.getUserID());
        Hibernate.initialize(user.getUserProfile());
        if (user.getUserProfile() != null) {
            user.getUserProfile().getFavoriteGenres().remove(genre);
            hs.merge(user);
        }
        tx.commit();
    } catch (Exception e) { if (tx != null) tx.rollback(); }
    finally { hs.close(); }
    return "redirect:/profile/genres";
}

    // ==================== GROUP CALENDAR LEGACY (FIXED null weekOffset) ====================
@GetMapping("/groups/{groupId}/sessions")
public String getGroupCalendarLegacy(@PathVariable int groupId, Model model) {
    // Delegate to weekly calendar with default weekOffset=0
    return "redirect:/groups/" + groupId + "/calendar?weekOffset=0";
}

    @PostMapping("/groups/{groupId}/sessions/schedule")
    public String scheduleSession(@PathVariable int groupId,
                                  @RequestParam int gameId,
                                  @RequestParam String scheduledDateTime,
                                  @RequestParam int durationMinutes,
                                  HttpSession session) {
        User currentUser = (User) session.getAttribute(SESSION_DB_USER);
        if (currentUser == null) return "redirect:/";
        Group group = sqlHandler.getGroupById(groupId);
        Game game = sqlHandler.getGameById(gameId);
        if (group == null || game == null) return "redirect:/";
        GroupSession gs = new GroupSession(group);
        gs.setGame(game);
        gs.setScheduledDate(LocalDateTime.parse(scheduledDateTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        gs.setDuration(durationMinutes);
        gs.setActive(true);
        sqlHandler.createGroupSession(gs);
        return "redirect:/groups/" + groupId + "/sessions";
    }

    @GetMapping("/groups/{id}/calendar")
public String groupCalendarWeekly(@PathVariable int id,
                                  @RequestParam(defaultValue = "0") int weekOffset,
                                  HttpSession session, Model model) {
    User currentUser = (User) session.getAttribute("db_user");
    if (currentUser == null) return "redirect:/";
    Session hs = HibernateUtil.getSessionFactory().openSession();
    Group group = null;
    List<Game> scheduleGames = new ArrayList<>();
    try {
        group = hs.get(Group.class, id);
        if (group == null) return "redirect:/";
        Hibernate.initialize(group.getSessions());
        Hibernate.initialize(group.getGames());
        for (GroupSession gs : group.getSessions()) {
            Hibernate.initialize(gs.getGame());
        }
        // Compute the shared games (same as the games page)
        GroupOperations ops = new GroupOperations();
        scheduleGames = ops.getSharedGames(group, sqlHandler);
        // Optionally apply min playtime filter
        int min = group.getMinPlaytimeRequirement();
        if (min > 0) {
            scheduleGames = scheduleGames.stream()
                    .filter(g -> g.getPlaytime() >= min)
                    .collect(Collectors.toList());
        }
    } finally {
        hs.close();
    }

    // Week calculation (was missing)
    LocalDate start = LocalDate.now().plusWeeks(weekOffset).with(DayOfWeek.MONDAY);
    LocalDate end = start.plusDays(6);
    List<GroupSession> weekSessions = group.getSessions().stream()
            .filter(gs -> gs.getScheduledDate() != null &&
                   !gs.getScheduledDate().toLocalDate().isBefore(start) &&
                   !gs.getScheduledDate().toLocalDate().isAfter(end))
            .collect(Collectors.toList());

    model.addAttribute("schedulableGames", scheduleGames);
    model.addAttribute("group", group);
    model.addAttribute("currentUser", currentUser);
    model.addAttribute("weekSessions", weekSessions);
    model.addAttribute("startDate", start);
    model.addAttribute("endDate", end);
    model.addAttribute("weekOffset", weekOffset);
    model.addAttribute("games", group.getGames());  // keep existing line
    return "group-calendar";
}

@GetMapping("/groups/{groupId}/games/csv")
public ResponseEntity<byte[]> exportGroupGamesCSV(@PathVariable int groupId,
                                                  @RequestParam(defaultValue = "0") int minPlaytime,
                                                  HttpSession session) {
    User currentUser = (User) session.getAttribute("db_user");
    if (currentUser == null) return ResponseEntity.status(401).build();
    Session hs = HibernateUtil.getSessionFactory().openSession();
    try {
        Group group = hs.get(Group.class, groupId);
        if (group == null) return ResponseEntity.notFound().build();
        Hibernate.initialize(group.getMembers());
        if (!group.getMembers().contains(currentUser)) return ResponseEntity.status(403).build();
        GroupOperations ops = new GroupOperations();
        List<Game> games = ops.getSharedGames(group, sqlHandler);
        if (minPlaytime == 0) minPlaytime = group.getMinPlaytimeRequirement();
        if (minPlaytime > 0) {
            final int f = minPlaytime;
            games = games.stream().filter(g -> g.getPlaytime() >= f).collect(Collectors.toList());
        }
        // Build CSV
        StringBuilder csv = new StringBuilder("Game,Genre,Playtime (min)\n");
        for (Game g : games) {
            csv.append(escapeCsv(g.getGameName())).append(",")
               .append(escapeCsv(g.getGenre())).append(",")
               .append(g.getPlaytime()).append("\n");
        }
        byte[] bytes = csv.toString().getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        headers.setContentDispositionFormData("attachment", "group_games.csv");
        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    } finally { hs.close(); }
}

private String escapeCsv(String value) {
    if (value == null) return "";
    if (value.contains(",") || value.contains("\"")) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
    return value;
}


    // ==================== EXPORT USER REPORT (3.1.32) ====================
    @GetMapping("/profile/export")
    public ResponseEntity<byte[]> exportUserReport(HttpSession session) {
        User sessionUser = (User) session.getAttribute(SESSION_DB_USER);
        if (sessionUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Session hibernateSession = HibernateUtil.getSessionFactory().openSession();
        User user = null;
        try {
            user = hibernateSession.get(User.class, sessionUser.getUserID());
            if (user != null) {
                Hibernate.initialize(user.getAchievementData());
                for (GameAchievement ga : user.getAchievementData()) {
                    Hibernate.initialize(ga.getGame());
                }
            }
        } finally { hibernateSession.close(); }
        if (user == null) return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        ExportCSV.ExportResult result = exportCSV.exportData(user);
        if (result.isSuccess() && result.getOutputFilePath() != null) {
            try {
                File file = new File(result.getOutputFilePath());
                byte[] contents = Files.readAllBytes(Paths.get(file.getAbsolutePath()));
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.TEXT_PLAIN);
                headers.setContentDispositionFormData("attachment", "user_report_" + user.getUserID() + ".csv");
                return new ResponseEntity<>(contents, headers, HttpStatus.OK);
            } catch (IOException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    @GetMapping("/profile/export/chart")
public ResponseEntity<byte[]> exportChart(HttpSession session) {
    User dbUser = (User) session.getAttribute("db_user");
    if (dbUser == null) return ResponseEntity.status(401).build();
    Session hs = HibernateUtil.getSessionFactory().openSession();
    try {
        User user = hs.get(User.class, dbUser.getUserID());
        List<Game> games = sqlHandler.getDistinctGamesByUser(user);
        byte[] png = graphService.generatePlaytimeChart(games);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        return new ResponseEntity<>(png, headers, HttpStatus.OK);
    } finally { hs.close(); }
}

    // ==================== ACTIVITY SUMMARIES (3.1.26, 3.1.27) ====================
    @GetMapping("/profile/activity/weekly")
public String getWeeklyActivity(HttpSession session, Model model) {
    return buildActivitySummary(session, model, "Weekly");
}

@GetMapping("/profile/activity/monthly")
public String getMonthlyActivity(HttpSession session, Model model) {
    return buildActivitySummary(session, model, "Monthly");
}

private String buildActivitySummary(HttpSession session, Model model, String period) {
    User sessionUser = (User) session.getAttribute(SESSION_DB_USER);
    if (sessionUser == null) return "redirect:/";
    Session hs = HibernateUtil.getSessionFactory().openSession();
    try {
        User user = hs.get(User.class, sessionUser.getUserID());
        Hibernate.initialize(user.getUserProfile());
        
        // Compute the text summary (as before, but using the lightweight query)
        String summary = generateActivitySummary(user, period);
        model.addAttribute("summary", summary);
        model.addAttribute("period", period);

        // Gather sessions from all groups this user belongs to
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate start, end;
        if ("Weekly".equals(period)) {
            start = today.with(java.time.DayOfWeek.MONDAY);
            end = start.plusDays(6);
        } else { // Monthly
            start = today.withDayOfMonth(1);
            end = start.plusMonths(1).minusDays(1);
        }

        List<Group> groups = hs.createQuery(
            "SELECT g FROM Group g JOIN g.members m WHERE m.userID = :uid", Group.class)
            .setParameter("uid", user.getUserID())
            .list();
        for (Group g : groups) {
            Hibernate.initialize(g.getSessions());
            Hibernate.initialize(g.getGames());  // for session game names
            for (GroupSession gs : g.getSessions()) {
                Hibernate.initialize(gs.getGame());
            }
        }

        List<GroupSession> upcomingSessions = new ArrayList<>();
        for (Group g : groups) {
            for (GroupSession gs : g.getSessions()) {
                if (gs.getScheduledDate() != null) {
                    java.time.LocalDate sessDate = gs.getScheduledDate().toLocalDate();
                    if (!sessDate.isBefore(start) && !sessDate.isAfter(end)) {
                        upcomingSessions.add(gs);
                    }
                }
            }
        }
        model.addAttribute("sessions", upcomingSessions);
        return "activity-summary";
    } finally { hs.close(); }
}


    private String generateActivitySummary(User user, String period) {
    StringBuilder sb = new StringBuilder();
    sb.append("=== ").append(period.toUpperCase()).append(" ACTIVITY SUMMARY ===\n");
    String profileName = (user.getUserProfile() != null) 
            ? user.getUserProfile().getProfileName() : "Unknown";
    sb.append("User: ").append(profileName).append("\n");
    sb.append("Steam ID: ").append(user.getSteamID() != null ? user.getSteamID() : "N/A").append("\n");

    List<Game> games = sqlHandler.getDistinctGamesByUser(user);
    int totalPlaytime = games.stream().mapToInt(Game::getPlaytime).sum();
    sb.append("Total Playtime: ").append(totalPlaytime / 60).append(" hours\n");
    sb.append("Games Played: ").append(games.size()).append("\n");
    return sb.toString();
}

    // ==================== MIN PLAYTIME REQUIREMENT (3.1.20) ====================
    @PostMapping("/groups/{groupId}/playtime-requirement")
    public String setMinPlaytime(@PathVariable int groupId,
                                 @RequestParam int minPlaytimeMinutes,
                                 HttpSession session) {
        User currentUser = (User) session.getAttribute("db_user");
        if (currentUser == null) return "redirect:/";
        Session hibernateSession = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = null;
        try {
            tx = hibernateSession.beginTransaction();
            Group group = hibernateSession.get(Group.class, groupId);
            if (group != null && group.getGroupOwner().getUserID() == currentUser.getUserID()) {
                group.setMinPlaytimeRequirement(minPlaytimeMinutes);
                hibernateSession.merge(group);
                tx.commit();
            }
        } catch (Exception e) {
            if (tx != null) tx.rollback();
        } finally { hibernateSession.close(); }
        return "redirect:/groups/" + groupId + "/games";
    }

    // ==================== ADD MANUAL GAME (NON-STEAM) ====================
    @GetMapping("/profile/add-game")
    public String showAddGameForm() { return "add-game"; }

    @PostMapping("/profile/add-game")
    public String addGame(@RequestParam String gameName,
                          @RequestParam(defaultValue = "0") int playtimeMinutes,
                          HttpSession session) {
        User currentUser = (User) session.getAttribute("db_user");
        if (currentUser == null) return "redirect:/";
        Session hs = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = null;
        try {
            tx = hs.beginTransaction();
            Game game = new Game();
            game.setGameName(gameName);
            game.setPlaytime(playtimeMinutes);
            hs.save(game);
            Achievement ach = hs.createQuery("FROM Achievement WHERE achievementName = 'Manual'", Achievement.class)
                .uniqueResult();
            if (ach == null) {
                ach = new Achievement("Manual", "Manually added game");
                hs.save(ach);
            }
            GameAchievement ga = new GameAchievement(game, ach, currentUser);
            hs.save(ga);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
        } finally { hs.close(); }
        return "redirect:/dashboard";
    }

    // ==================== GROUP PREFERENCES (FIXED tally + template name) ====================
@GetMapping("/groups/{groupId}/preferences")
public String listPreferences(@PathVariable int groupId, HttpSession session, Model model) {
    User currentUser = (User) session.getAttribute("db_user");
    if (currentUser == null) return "redirect:/";
    Session hs = HibernateUtil.getSessionFactory().openSession();
    try {
        Group group = hs.get(Group.class, groupId);
        if (group == null) return "redirect:/";
        Hibernate.initialize(group.getMembers());
        List<GroupPreference> prefs = hs.createQuery(
            "FROM GroupPreference gp WHERE gp.group.groupID = :gid", GroupPreference.class)
            .setParameter("gid", groupId)
            .list();
        
        Map<Integer, Long[]> prefVoteCounts = new HashMap<>();
        for (GroupPreference gp : prefs) {
            Hibernate.initialize(gp.getVotes());
            long yesCount = gp.getVotes().stream().filter(v -> v.isApproved()).count();
            long noCount = gp.getVotes().stream().filter(v -> !v.isApproved()).count();
            prefVoteCounts.put(gp.getPreferenceId(), new Long[]{yesCount, noCount});
        }
        
        model.addAttribute("group", group);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("preferences", prefs);
        model.addAttribute("prefVoteCounts", prefVoteCounts);
        return "group-preference";
    } finally { hs.close(); }
}

    @PostMapping("/groups/{groupId}/preferences/create")
    public String createPreference(@PathVariable int groupId,
                                   @RequestParam String preferenceName,
                                   HttpSession session) {
        User currentUser = (User) session.getAttribute("db_user");
        if (currentUser == null) return "redirect:/";
        Group group = sqlHandler.getGroupById(groupId);
        if (group == null || group.getGroupOwner().getUserID() != currentUser.getUserID())
            return "redirect:/";
        GroupPreference gp = new GroupPreference(group, preferenceName);
        sqlHandler.createGroupPreference(gp);
        return "redirect:/groups/" + groupId + "/preferences";
    }

    @PostMapping("/groups/{groupId}/preferences/{prefId}/vote")
    public String voteOnPreference(@PathVariable int groupId,
                                   @PathVariable int prefId,
                                   @RequestParam boolean approved,
                                   HttpSession session) {
        User currentUser = (User) session.getAttribute("db_user");
        if (currentUser == null) return "redirect:/";
        GroupPreference pref = sqlHandler.getGroupPreferenceById(prefId);
        if (pref == null || pref.isVotingClosed()) return "redirect:/groups/" + groupId + "/preferences";
        GroupPreferenceVote vote = new GroupPreferenceVote(pref, currentUser, approved);
        sqlHandler.createGroupPreferenceVote(vote);
        return "redirect:/groups/" + groupId + "/preferences";
    }

    @PostMapping("/groups/{groupId}/preferences/{prefId}/decide")
    public String decidePreference(@PathVariable int groupId,
                                   @PathVariable int prefId,
                                   HttpSession session) {
        User currentUser = (User) session.getAttribute("db_user");
        if (currentUser == null) return "redirect:/";
        GroupPreference pref = sqlHandler.getGroupPreferenceById(prefId);
        if (pref == null || pref.getGroup().getGroupOwner().getUserID() != currentUser.getUserID())
            return "redirect:/";
        pref.setVotingClosed(true);
        sqlHandler.updateGroupPreference(pref);
        return "redirect:/groups/" + groupId + "/preferences";
    }

    // ==================== SHARABLE GAME LIST (FIXED) ====================
@PostMapping("/groups/{groupId}/share-games")
@ResponseBody
public String shareGameList(@PathVariable int groupId, HttpSession session) {
    User currentUser = (User) session.getAttribute("db_user");
    if (currentUser == null) return "Unauthorized";
    Session hs = HibernateUtil.getSessionFactory().openSession();
    Transaction tx = null;
    try {
        tx = hs.beginTransaction();
        Group group = hs.get(Group.class, groupId);
        if (group == null) {
            tx.commit();
            return "Group not found";
        }
        Hibernate.initialize(group.getMembers());
        boolean isMember = group.getMembers().stream().anyMatch(m -> m.getUserID() == currentUser.getUserID());
        if (!isMember) {
            tx.commit();
            return "Unauthorized";
        }
        String token = UUID.randomUUID().toString().substring(0, 8);
        group.setShareToken(token);
        hs.merge(group);
        tx.commit();
        return "/groups/shared/" + token;
    } catch (Exception e) {
        if (tx != null) tx.rollback();
        return "Error";
    } finally { hs.close(); }
}

@PostMapping("/groups/{groupId}/reset-filters")
public String resetFilters(@PathVariable int groupId, HttpSession session) {
    User currentUser = (User) session.getAttribute("db_user");
    if (currentUser == null) return "redirect:/";
    Session hs = HibernateUtil.getSessionFactory().openSession();
    Transaction tx = null;
    try {
        tx = hs.beginTransaction();
        Group group = hs.get(Group.class, groupId);
        if (group == null) {
            tx.commit();
            return "redirect:/dashboard";
        }
        // verify membership
        Hibernate.initialize(group.getMembers());
        boolean isMember = group.getMembers().stream().anyMatch(m -> m.getUserID() == currentUser.getUserID());
        if (!isMember) {
            tx.commit();
            return "redirect:/dashboard";
        }
        // Reset the stored min playtime requirement
        group.setMinPlaytimeRequirement(0);
        // Clear the stored game list (so it returns to shared games)
        group.getGames().clear();
        hs.merge(group);
        tx.commit();
    } catch (Exception e) {
        if (tx != null) tx.rollback();
    } finally { hs.close(); }
    return "redirect:/groups/" + groupId + "/games";
}

    @GetMapping("/groups/shared/{token}")
public String viewSharedGames(@PathVariable String token, Model model) {
    Session hs = HibernateUtil.getSessionFactory().openSession();
    try {
        Group group = hs.createQuery(
            "FROM Group WHERE shareToken = :token", Group.class)
            .setParameter("token", token)
            .uniqueResult();
            if (group == null) {
            return "redirect:/";
        }
        Hibernate.initialize(group.getGames());
        model.addAttribute("group", group);
        model.addAttribute("games", group.getGames());
        return "shared-games";
    } finally { hs.close(); }
}

    // ==================== ADDITIONAL ENDPOINTS ====================
    @GetMapping("/groups/{id}")
    public String viewGroupRedirect(@PathVariable String id) {
        if ("create".equals(id)) return "redirect:/dashboard";
        return "redirect:/groups/" + id + "/games";
    }

    @PostMapping("/groups/{groupId}/delete")
public String deleteGroup(@PathVariable int groupId, HttpSession session) {
    User currentUser = (User) session.getAttribute(SESSION_DB_USER);
    if (currentUser == null) return "redirect:/";
    Group group = sqlHandler.getGroupById(groupId);
    if (group != null && group.getGroupOwner().getUserID() == currentUser.getUserID()) {
        sqlHandler.deleteGroupAndDependencies(group);
    }
    return "redirect:/dashboard";
}


    @PostMapping("/groups/{groupId}/transfer-ownership")
    public String transferOwnership(@PathVariable int groupId, @RequestParam int newOwnerId, HttpSession session) {
        User currentUser = (User) session.getAttribute(SESSION_DB_USER);
        if (currentUser == null) return "redirect:/";
        Session hibernateSession = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = null;
        try {
            tx = hibernateSession.beginTransaction();
            Group group = hibernateSession.get(Group.class, groupId);
            if (group != null && group.getGroupOwner().getUserID() == currentUser.getUserID()) {
                Hibernate.initialize(group.getMembers());
                User newOwner = hibernateSession.get(User.class, newOwnerId);
                if (newOwner != null && group.getMembers().contains(newOwner)) {
                    group.setGroupOwner(newOwner);
                    hibernateSession.merge(group);
                    tx.commit();
                }
            }
        } catch (Exception e) {
            if (tx != null) tx.rollback();
        } finally { hibernateSession.close(); }
        return "redirect:/groups/" + groupId + "/members";
    }

    @PostMapping("/groups/{groupId}/preferences/{prefId}/delete")
public String deletePreference(@PathVariable int groupId, @PathVariable int prefId, HttpSession session) {
    User currentUser = (User) session.getAttribute("db_user");
    GroupPreference pref = sqlHandler.getGroupPreferenceById(prefId);
    if (pref != null && pref.getGroup().getGroupOwner().getUserID() == currentUser.getUserID()) {
        sqlHandler.deleteGroupPreference(pref);
    }
    return "redirect:/groups/" + groupId + "/preferences";
}


    @PostMapping("/groups/{groupId}/members/{userId}/remove")
    public String removeMember(@PathVariable int groupId, @PathVariable int userId, HttpSession session) {
        User currentUser = (User) session.getAttribute(SESSION_DB_USER);
        if (currentUser == null) return "redirect:/";
        Session hibernateSession = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = null;
        try {
            tx = hibernateSession.beginTransaction();
            Group group = hibernateSession.get(Group.class, groupId);
            if (group != null && group.getGroupOwner().getUserID() == currentUser.getUserID()) {
                Hibernate.initialize(group.getMembers());
                User toRemove = group.getMembers().stream()
                        .filter(u -> u.getUserID() == userId)
                        .findFirst().orElse(null);
                if (toRemove != null) {
                    group.removeGroupMember(toRemove);
                    hibernateSession.merge(group);
                    tx.commit();
                }
            }
        } catch (Exception e) {
            if (tx != null) tx.rollback();
        } finally { hibernateSession.close(); }
        return "redirect:/groups/" + groupId + "/members";
    }

    // ==================== RANDOMIZE (FIXED) ====================
@GetMapping("/groups/{id}/randomize")
public String randomizeGameList(@PathVariable int id, 
                                @RequestParam(required = false) Integer count,
                                HttpSession session) {
    User currentUser = (User) session.getAttribute(SESSION_DB_USER);
    if (currentUser == null) return "redirect:/";
    Session hs = HibernateUtil.getSessionFactory().openSession();
    Transaction tx = null;
    try {
        tx = hs.beginTransaction();
        Group group = hs.get(Group.class, id);
        if (group == null) { tx.commit(); return "redirect:/"; }
        Hibernate.initialize(group.getMembers());
        boolean isMember = group.getMembers().stream().anyMatch(m -> m.getUserID() == currentUser.getUserID());
        if (!isMember) { tx.commit(); return "redirect:/dashboard"; }

        // Get shared games, then randomize a subset
        GroupOperations ops = new GroupOperations();
        List<Game> shared = ops.getSharedGames(group, sqlHandler);
        if (shared.isEmpty()) { tx.commit(); return "redirect:/groups/" + id + "/games?random=true"; }

        Collections.shuffle(shared);
        if (count != null && count > 0 && count < shared.size()) {
            shared = shared.subList(0, count);
        }
        group.setGames(new ArrayList<>(shared));  // store the subset
        hs.merge(group);
        tx.commit();
    } catch (Exception e) {
        if (tx != null) tx.rollback();
    } finally { hs.close(); }
    return "redirect:/groups/" + id + "/games?random=true";
}

    @PostMapping("/profile/delete")
    public String deleteUserAccount(HttpSession session) {
        User currentUser = (User) session.getAttribute(SESSION_DB_USER);
        if (currentUser != null) {
            sqlHandler.deleteUser(currentUser);
            session.invalidate();
        }
        return "redirect:/";
    }

    @GetMapping("/groups/{groupId}/generate-invite")
public ResponseEntity<String> generateInviteLink(@PathVariable int groupId, HttpSession session) {
    User currentUser = (User) session.getAttribute(SESSION_DB_USER);
    if (currentUser == null) return ResponseEntity.status(401).body("Not logged in");
    Group group = sqlHandler.getGroupById(groupId);
    if (group == null || group.getGroupOwner().getUserID() != currentUser.getUserID())
        return ResponseEntity.status(403).body("Only the group owner can generate an invite");
    String token = UUID.randomUUID().toString();
    GroupJoinRequest req = new GroupJoinRequest(group, null, token);
    sqlHandler.createGroupJoinRequest(req);
    return ResponseEntity.ok("/groups/join/" + token);
}

    @GetMapping("/groups")
    public String listGroups(HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute(SESSION_DB_USER);
        if (currentUser == null) return "redirect:/";
        return "redirect:/dashboard";
    }
}