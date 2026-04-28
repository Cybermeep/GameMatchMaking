package edu.isu.gamematch.controller;

import edu.isu.gamematch.*;
import edu.isu.gamematch.export.ExportCSV;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.hibernate.Session;
import org.hibernate.Hibernate;
import org.hibernate.Transaction;



import javax.servlet.http.HttpSession;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class GameMatchController {

    private static final String SESSION_DB_USER = "db_user";

    @Autowired
    private SQLHandler sqlHandler;

    @Autowired
    private ExportCSV exportCSV;

    // ==================== GROUP DETAIL & GAMES (3.1.3, 3.1.20) ====================
    @GetMapping("/groups/{groupId}/games")
public String getFilteredGames(@PathVariable int groupId,
                               @RequestParam(defaultValue = "0") int minPlaytime,
                               HttpSession session, Model model) {
    User currentUser = (User) session.getAttribute("db_user");
    if (currentUser == null) return "redirect:/";

    // Load group with members and their achievements eagerly
    Session hibernateSession = HibernateUtil.getSessionFactory().openSession();
    Group group = null;
    List<Game> sharedGames = new ArrayList<>();
    
    try {
        group = hibernateSession.createQuery(
            "FROM Group g LEFT JOIN FETCH g.members WHERE g.groupID = :id", Group.class)
            .setParameter("id", groupId)
            .uniqueResult();
        
        if (group == null) {
            hibernateSession.close();
            return "redirect:/dashboard";
        }
        
        // Compute shared games (Fix 5.1)
        GroupOperations ops = new GroupOperations();
        sharedGames = ops.getSharedGames(group, sqlHandler);
        
        // Apply minimum playtime filter
        if (minPlaytime > 0) {
            sharedGames = sharedGames.stream()
                    .filter(g -> g.getPlaytime() >= minPlaytime)
                    .collect(Collectors.toList());
        }
        
        // Rank by playtime + genre (Fix 5.2)
        ops.rankList(group, currentUser, sqlHandler);
        
    } finally {
        hibernateSession.close();
    }

    model.addAttribute("group", group);
    model.addAttribute("games", sharedGames);
    model.addAttribute("minPlaytime", minPlaytime);
    model.addAttribute("currentUser", currentUser);
    return "group-games";
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
        GroupJoinRequest joinReq = sqlHandler.getGroupJoinRequestByToken(inviteToken);
        if (joinReq == null) {
            model.addAttribute("error", "Invalid or expired invite link.");
            return "redirect:/dashboard";
        }
        Group group = joinReq.getGroup();
        if (group.getMembers().contains(currentUser)) {
            model.addAttribute("error", "You are already a member.");
            return "redirect:/dashboard";
        }
        GroupJoinRequest newReq = new GroupJoinRequest(group, currentUser, null);
        sqlHandler.createGroupJoinRequest(newReq);
        model.addAttribute("message", "Join request sent to group owner.");
        return "redirect:/dashboard";
    }

    // ==================== LEAVE GROUP (3.1.10) ====================
    @PostMapping("/groups/{groupId}/leave")
    public String leaveGroup(@PathVariable int groupId, HttpSession session) {
        User currentUser = (User) session.getAttribute(SESSION_DB_USER);
        if (currentUser == null) return "redirect:/";
        Group group = sqlHandler.getGroupById(groupId);
        if (group != null && group.getMembers().contains(currentUser)) {
            group.removeGroupMember(currentUser);
            sqlHandler.updateGroup(group);
        }
        return "redirect:/dashboard";
    }

    // ==================== PENDING JOIN REQUESTS (3.1.19) ====================
    @GetMapping("/groups/{groupId}/requests/pending")
public String getPendingJoinRequests(@PathVariable int groupId, HttpSession session, Model model) {
    User currentUser = (User) session.getAttribute(SESSION_DB_USER);
    if (currentUser == null) return "redirect:/";

    Session hibernateSession = HibernateUtil.getSessionFactory().openSession();
    Group group = null;
    List<GroupJoinRequest> pendingRequests = new ArrayList<>();
    try {
        group = hibernateSession.get(Group.class, groupId);
        if (group == null || group.getGroupOwner().getUserID() != currentUser.getUserID()) {
            return "redirect:/";
        }
        Hibernate.initialize(group.getJoinRequests());
        pendingRequests = group.getJoinRequests().stream()
                .filter(r -> "PENDING".equals(r.getStatus()))
                .collect(Collectors.toList());
    } finally {
        hibernateSession.close();
    }

    model.addAttribute("group", group);
    model.addAttribute("requests", pendingRequests);
    return "pending-requests";
}

    // ==================== ACCEPT JOIN REQUEST (3.1.17) ====================
    @PostMapping("/groups/{groupId}/requests/{requestId}/accept")
    public String acceptJoinRequest(@PathVariable int groupId, @PathVariable int requestId, HttpSession session) {
        User currentUser = (User) session.getAttribute(SESSION_DB_USER);
        Group group = sqlHandler.getGroupById(groupId);
        if (group == null || group.getGroupOwner().getUserID() != currentUser.getUserID()) return "redirect:/";
        GroupJoinRequest req = sqlHandler.getGroupJoinRequestById(requestId);
        if (req != null && req.getGroup().getGroupID() == groupId) {
            req.setStatus("ACCEPTED");
            sqlHandler.updateGroupJoinRequest(req);
            group.addGroupMember(req.getRequestingUser());
            sqlHandler.updateGroup(group);
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

    // ==================== VOTE ON GAME (3.1.12) ====================
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

        GroupOperations ops = new GroupOperations();
        games = ops.getSharedGames(group, sqlHandler);
        int minPlaytime = group.getMinPlaytimeRequirement();
        if (minPlaytime > 0) {
            games = games.stream().filter(g -> g.getPlaytime() >= minPlaytime).collect(Collectors.toList());
        }
    } finally {
        hibernateSession.close();
    }

    model.addAttribute("group", group);
    model.addAttribute("games", games);
    return "vote";
}

    @PostMapping("/groups/{groupId}/vote")
    public String castVote(@PathVariable int groupId, @RequestParam int gameId, HttpSession session) {
        User currentUser = (User) session.getAttribute(SESSION_DB_USER);
        if (currentUser == null) return "redirect:/";
        Group group = sqlHandler.getGroupById(groupId);
        Game game = sqlHandler.getGameById(gameId);
        if (group == null || game == null) return "redirect:/groups/" + groupId;

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

    // ==================== USER PROFILE (3.1.15) ====================
    @GetMapping("/users/{steamId}")
    public String getUserProfile(@PathVariable String steamId, HttpSession session, Model model) {
        User viewedUser = sqlHandler.getUserBySteamId(steamId);
        if (viewedUser == null) return "redirect:/dashboard";
        User currentUser = (User) session.getAttribute(SESSION_DB_USER);
        model.addAttribute("viewedUser", viewedUser);
        if (currentUser != null && !currentUser.equals(viewedUser)) {
            List<User> mutual = new ArrayList<>();
            for (User friend : currentUser.getFriends()) {
                if (viewedUser.getFriends().contains(friend)) mutual.add(friend);
            }
            model.addAttribute("mutualFriends", mutual);
        }
        return "user-profile";
    }

    // ==================== SEARCH USER (3.1.21) ====================
    @GetMapping("/users/search")
    public String searchUsers(@RequestParam String q, Model model) {
        List<User> results = sqlHandler.searchUsersByPersonaName(q);
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
            Hibernate.initialize(member.getAchievementData());
            List<Achievement> achievements = member.getAchievementData().stream()
                    .map(GameAchievement::getAchievement)
                    .collect(Collectors.toList());
            userAchievements.put(member, achievements);
        }
    } finally {
        hibernateSession.close();
    }

    model.addAttribute("group", group);
    model.addAttribute("userAchievements", userAchievements);
    return "achievement-comparison";
}

    // ==================== GENRE TAGS (3.1.24) ====================
    @GetMapping("/profile/genres")
    public String showGenreTags(HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute(SESSION_DB_USER);
        if (currentUser == null) return "redirect:/";
        List<String> available = Arrays.asList("Action","RPG","Strategy","Shooter","Adventure","Puzzle","Simulation","Sports","Racing","Fighting");
        model.addAttribute("availableGenres", available);
        model.addAttribute("userGenres", currentUser.getUserProfile().getFavoriteGenres());
        return "genre-tags";
    }

    @PostMapping("/profile/genres/add")
    public String addGenreTag(@RequestParam String genre, HttpSession session) {
        User currentUser = (User) session.getAttribute(SESSION_DB_USER);
        if (currentUser != null && currentUser.getUserProfile() != null) {
            if (!currentUser.getUserProfile().getFavoriteGenres().contains(genre)) {
                currentUser.getUserProfile().getFavoriteGenres().add(genre);
                sqlHandler.updateUserProfile(currentUser.getUserProfile());
            }
        }
        return "redirect:/profile/genres";
    }

    @PostMapping("/profile/genres/remove")
    public String removeGenreTag(@RequestParam String genre, HttpSession session) {
        User currentUser = (User) session.getAttribute(SESSION_DB_USER);
        if (currentUser != null && currentUser.getUserProfile() != null) {
            currentUser.getUserProfile().getFavoriteGenres().remove(genre);
            sqlHandler.updateUserProfile(currentUser.getUserProfile());
        }
        return "redirect:/profile/genres";
    }

    // ==================== GROUP SESSION SCHEDULING (3.1.30, 3.1.31) ====================
    @GetMapping("/groups/{groupId}/sessions")
public String getGroupCalendar(@PathVariable int groupId, Model model) {
    Session hibernateSession = HibernateUtil.getSessionFactory().openSession();
    Group group = null;
    List<GroupSession> sessions = new ArrayList<>();
    try {
        group = hibernateSession.get(Group.class, groupId);
        if (group == null) return "redirect:/";
        Hibernate.initialize(group.getSessions());
        sessions = new ArrayList<>(group.getSessions());
    } finally {
        hibernateSession.close();
    }

    model.addAttribute("group", group);
    model.addAttribute("sessions", sessions);
    return "group-calendar";
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

    // ==================== EXPORT USER REPORT (3.1.32) ====================
    @GetMapping("/profile/export")
    public ResponseEntity<byte[]> exportUserReport(HttpSession session) {
        User user = (User) session.getAttribute(SESSION_DB_USER);
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
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

    // ==================== ACTIVITY SUMMARIES (3.1.26, 3.1.27) ====================
    @GetMapping("/profile/activity/weekly")
    public String getWeeklyActivity(HttpSession session, Model model) {
        User user = (User) session.getAttribute(SESSION_DB_USER);
        if (user == null) return "redirect:/";
        model.addAttribute("summary", generateActivitySummary(user, "Weekly"));
        model.addAttribute("period", "Weekly");
        return "activity-summary";
    }

    @GetMapping("/profile/activity/monthly")
    public String getMonthlyActivity(HttpSession session, Model model) {
        User user = (User) session.getAttribute(SESSION_DB_USER);
        if (user == null) return "redirect:/";
        model.addAttribute("summary", generateActivitySummary(user, "Monthly"));
        model.addAttribute("period", "Monthly");
        return "activity-summary";
    }

    private String generateActivitySummary(User user, String period) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== ").append(period.toUpperCase()).append(" ACTIVITY SUMMARY ===\n");
        sb.append("User: ").append(user.getUserProfile().getProfileName()).append("\n");
        sb.append("Steam ID: ").append(user.getSteamID()).append("\n");
        int totalPlaytime = user.getAchievementData().stream()
                .mapToInt(ga -> ga.getGame().getPlaytime())
                .sum();
        sb.append("Total Playtime: ").append(totalPlaytime / 60).append(" hours\n");
        sb.append("Games Played: ").append(user.getAchievementData().size()).append("\n");
        return sb.toString();
    }

    // ==================== ADDITIONAL ENDPOINTS (from audit) ====================
    @GetMapping("/groups/{id}")
public String viewGroupRedirect(@PathVariable String id) {
    // Skip if it's a reserved word
    if ("create".equals(id)) {
        return "redirect:/dashboard";
    }
    return "redirect:/groups/" + id + "/games";
}

    @PostMapping("/groups/{groupId}/delete")
public String deleteGroup(@PathVariable int groupId, HttpSession session) {
    User currentUser = (User) session.getAttribute(SESSION_DB_USER);
    if (currentUser == null) return "redirect:/";

    Session hibernateSession = HibernateUtil.getSessionFactory().openSession();
    Transaction tx = null;
    try {
        tx = hibernateSession.beginTransaction();
        Group group = hibernateSession.get(Group.class, groupId);
        if (group != null && group.getGroupOwner().getUserID() == currentUser.getUserID()) {
            hibernateSession.remove(group);
            tx.commit();
        }
    } catch (Exception e) {
        if (tx != null) tx.rollback();
    } finally {
        hibernateSession.close();
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
    } finally {
        hibernateSession.close();
    }
    return "redirect:/groups/" + groupId + "/members";
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
    } finally {
        hibernateSession.close();
    }
    return "redirect:/groups/" + groupId + "/members";
}

    @GetMapping("/groups/{id}/randomize")
    public String randomizeGameList(@PathVariable int id, HttpSession session) {
        User currentUser = (User) session.getAttribute(SESSION_DB_USER);
        Group group = sqlHandler.getGroupById(id);
        if (group == null || !group.getMembers().contains(currentUser)) return "redirect:/";
        GroupOperations ops = new GroupOperations();
        group.setGames(ops.randomizeGameList(group.getGames()));
        sqlHandler.updateGroup(group);
        return "redirect:/groups/" + id + "/games";
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
    @ResponseBody
    public String generateInviteLink(@PathVariable int groupId, HttpSession session) {
        User currentUser = (User) session.getAttribute(SESSION_DB_USER);
        if (currentUser == null) return "Unauthorized";
        Group group = sqlHandler.getGroupById(groupId);
        if (group == null || group.getGroupOwner().getUserID() != currentUser.getUserID()) return "Unauthorized";
        String token = UUID.randomUUID().toString();
        GroupJoinRequest req = new GroupJoinRequest(group, null, token);
        sqlHandler.createGroupJoinRequest(req);
        return "/groups/join/" + token;
    }
}