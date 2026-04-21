package edu.isu.gamematch.controller;

import edu.isu.gamematch.*;
import edu.isu.gamematch.export.ExportCSV;
import edu.isu.gamematch.steam.SteamAPIService;
import edu.isu.gamematch.steam.SteamUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class GameMatchController {

    private static final String SESSION_STEAM_USER = "steam_user";
    private static final String SESSION_DB_USER = "db_user";

    @Autowired
    private SQLHandler sqlHandler;

    @Autowired
    private ExportCSV exportCSV;

    @Autowired
    private SteamAPIService steamAPIService;

    // ==================== GROUP MEMBERS (3.1.8) ====================
    
    @GetMapping("/groups/{groupId}/members")
    public String getGroupMembers(@PathVariable int groupId, HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute(SESSION_DB_USER);
        if (currentUser == null) return "redirect:/";
        
        Group group = sqlHandler.getGroupById(groupId);
        if (group == null) return "redirect:/groups";
        
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
        
        GroupJoinRequest joinRequest = sqlHandler.getGroupJoinRequestByToken(inviteToken);
        
        if (joinRequest == null) {
            model.addAttribute("error", "Invalid or expired invite link");
            return "redirect:/groups";
        }
        
        Group group = joinRequest.getGroup();
        
        // Check if already a member
        if (group.getMembers().contains(currentUser)) {
            model.addAttribute("error", "You are already a member of this group");
            return "redirect:/groups";
        }
        
        // Create new join request for this user
        GroupJoinRequest newRequest = new GroupJoinRequest(group, currentUser, null);
        sqlHandler.createGroupJoinRequest(newRequest);
        
        model.addAttribute("message", "Join request sent to group owner");
        return "redirect:/groups";
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
        
        return "redirect:/groups";
    }

    // ==================== PENDING JOIN REQUESTS (3.1.19) ====================
    
    @GetMapping("/groups/{groupId}/requests/pending")
    public String getPendingJoinRequests(@PathVariable int groupId, HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute(SESSION_DB_USER);
        if (currentUser == null) return "redirect:/";
        
        Group group = sqlHandler.getGroupById(groupId);
        
        // Only owner can view pending requests
        if (group == null || group.getGroupOwner().getUserID() != currentUser.getUserID()) {
            return "redirect:/groups";
        }
        
        List<GroupJoinRequest> pendingRequests = sqlHandler.getPendingJoinRequests(group);
        model.addAttribute("requests", pendingRequests);
        model.addAttribute("group", group);
        return "pending-requests";
    }

    // ==================== ACCEPT JOIN REQUEST (3.1.17) ====================
    
    @PostMapping("/groups/{groupId}/requests/{requestId}/accept")
    public String acceptJoinRequest(@PathVariable int groupId, @PathVariable int requestId, HttpSession session) {
        User currentUser = (User) session.getAttribute(SESSION_DB_USER);
        if (currentUser == null) return "redirect:/";
        
        Group group = sqlHandler.getGroupById(groupId);
        
        if (group == null || group.getGroupOwner().getUserID() != currentUser.getUserID()) {
            return "redirect:/groups";
        }
        
        GroupJoinRequest joinRequest = sqlHandler.getGroupJoinRequestById(requestId);
        if (joinRequest != null && joinRequest.getGroup().getGroupID() == groupId) {
            joinRequest.setStatus("ACCEPTED");
            sqlHandler.updateGroupJoinRequest(joinRequest);
            
            // Add user to group
            group.addGroupMember(joinRequest.getRequestingUser());
            sqlHandler.updateGroup(group);
        }
        
        return "redirect:/groups/" + groupId + "/requests/pending";
    }

    // ==================== DECLINE JOIN REQUEST (3.1.18) ====================
    
    @PostMapping("/groups/{groupId}/requests/{requestId}/decline")
    public String declineJoinRequest(@PathVariable int groupId, @PathVariable int requestId, HttpSession session) {
        User currentUser = (User) session.getAttribute(SESSION_DB_USER);
        if (currentUser == null) return "redirect:/";
        
        Group group = sqlHandler.getGroupById(groupId);
        
        if (group == null || group.getGroupOwner().getUserID() != currentUser.getUserID()) {
            return "redirect:/groups";
        }
        
        GroupJoinRequest joinRequest = sqlHandler.getGroupJoinRequestById(requestId);
        if (joinRequest != null && joinRequest.getGroup().getGroupID() == groupId) {
            joinRequest.setStatus("DECLINED");
            sqlHandler.updateGroupJoinRequest(joinRequest);
        }
        
        return "redirect:/groups/" + groupId + "/requests/pending";
    }

    // ==================== FILTERED GAMES LIST (3.1.3) ====================
    
    @GetMapping("/groups/{groupId}/games")
    public String getFilteredGames(@PathVariable int groupId,
                                   @RequestParam(defaultValue = "0") int minPlaytime,
                                   HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute(SESSION_DB_USER);
        if (currentUser == null) return "redirect:/";
        
        Group group = sqlHandler.getGroupById(groupId);
        if (group == null) return "redirect:/groups";
        
        List<Game> games = group.getGames();
        
        // Filter by minimum playtime
        if (minPlaytime > 0) {
            games = games.stream()
                .filter(g -> g.getPlaytime() >= minPlaytime)
                .collect(Collectors.toList());
        }
        
        // Sort using existing rankList method
        GroupOperations groupOps = new GroupOperations(games);
        groupOps.rankList(group);
        
        model.addAttribute("games", games);
        model.addAttribute("group", group);
        model.addAttribute("minPlaytime", minPlaytime);
        return "group-games";
    }

    // ==================== SET PLAYTIME REQUIREMENT (3.1.20) ====================
    
    @PostMapping("/groups/{groupId}/playtime-requirement")
    public String setPlaytimeRequirement(@PathVariable int groupId,
                                         @RequestParam int minPlaytimeMinutes,
                                         HttpSession session) {
        User currentUser = (User) session.getAttribute(SESSION_DB_USER);
        if (currentUser == null) return "redirect:/";
        
        Group group = sqlHandler.getGroupById(groupId);
        
        // Only owner can set requirements
        if (group != null && group.getGroupOwner().getUserID() == currentUser.getUserID()) {
            group.setMinPlaytimeRequirement(minPlaytimeMinutes);
            sqlHandler.updateGroup(group);
        }
        
        return "redirect:/groups/" + groupId + "/games";
    }

    // ==================== VOTE ON GAME (3.1.12) ====================
    
    @GetMapping("/groups/{groupId}/vote")
    public String showVotePage(@PathVariable int groupId, HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute(SESSION_DB_USER);
        if (currentUser == null) return "redirect:/";
        
        Group group = sqlHandler.getGroupById(groupId);
        if (group == null) return "redirect:/groups";
        
        List<Game> games = group.getGames();
        
        // Apply playtime requirement filter if set
        int minPlaytime = group.getMinPlaytimeRequirement();
        if (minPlaytime > 0) {
            games = games.stream()
                .filter(g -> g.getPlaytime() >= minPlaytime)
                .collect(Collectors.toList());
        }
        
        model.addAttribute("games", games);
        model.addAttribute("group", group);
        return "vote";
    }
    
    @PostMapping("/groups/{groupId}/vote")
    public String castVote(@PathVariable int groupId, @RequestParam int gameId, HttpSession session) {
        User currentUser = (User) session.getAttribute(SESSION_DB_USER);
        if (currentUser == null) return "redirect:/";
        
        Group group = sqlHandler.getGroupById(groupId);
        Game game = sqlHandler.getGameById(gameId);
        
        if (group == null || game == null) return "redirect:/groups/" + groupId;
        
        // Check if user already voted
        List<GroupVote> existingVotes = sqlHandler.getVotesByUser(currentUser);
        GroupVote existingVote = existingVotes.stream()
            .filter(v -> v.getGroup().getGroupID() == groupId)
            .findFirst()
            .orElse(null);
        
        if (existingVote == null) {
            GroupVote vote = new GroupVote(group, currentUser);
            vote.castVote(game);
            sqlHandler.createGroupVote(vote);
        } else {
            existingVote.updateVote(game);
            sqlHandler.updateGroupVote(existingVote);
        }
        
        return "redirect:/groups/" + groupId + "/vote/results";
    }
    
    @GetMapping("/groups/{groupId}/vote/results")
    public String voteResults(@PathVariable int groupId, Model model) {
        Group group = sqlHandler.getGroupById(groupId);
        if (group == null) return "redirect:/groups";
        
        List<GroupVote> votes = sqlHandler.getGroupVotesByGroup(group);
        Map<Game, Integer> tally = GroupVote.tallyVotes(votes);
        Game winner = GroupVote.getWinner(tally);
        
        model.addAttribute("tally", tally);
        model.addAttribute("winner", winner);
        model.addAttribute("group", group);
        return "vote-results";
    }

    // ==================== USER PROFILE (3.1.15) ====================
    
    @GetMapping("/users/{steamId}")
    public String getUserProfile(@PathVariable String steamId, HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute(SESSION_DB_USER);
        
        User viewedUser = sqlHandler.getUserBySteamId(steamId);
        if (viewedUser == null) {
            return "redirect:/dashboard";
        }
        
        model.addAttribute("viewedUser", viewedUser);
        model.addAttribute("games", viewedUser.getAchievementData());
        
        // Calculate mutual friends if current user is logged in
        if (currentUser != null) {
            List<User> mutualFriends = getMutualFriends(currentUser, viewedUser);
            model.addAttribute("mutualFriends", mutualFriends);
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

    // ==================== MUTUAL FRIENDS (3.1.25) ====================
    
    @GetMapping("/users/{steamId}/mutual-friends")
    @ResponseBody
    public List<User> getMutualFriendsEndpoint(@PathVariable String steamId, HttpSession session) {
        User currentUser = (User) session.getAttribute(SESSION_DB_USER);
        User viewedUser = sqlHandler.getUserBySteamId(steamId);
        
        if (currentUser == null || viewedUser == null) {
            return new ArrayList<>();
        }
        
        return getMutualFriends(currentUser, viewedUser);
    }
    
    private List<User> getMutualFriends(User user1, User user2) {
        List<User> mutual = new ArrayList<>();
        for (User friend : user1.getFriends()) {
            if (user2.getFriends().contains(friend)) {
                mutual.add(friend);
            }
        }
        return mutual;
    }

    // ==================== COMPARE ACHIEVEMENTS (3.1.22) ====================
    
    @GetMapping("/groups/{groupId}/achievements")
    public String compareAchievements(@PathVariable int groupId, Model model) {
        Group group = sqlHandler.getGroupById(groupId);
        if (group == null) return "redirect:/groups";
        
        Map<User, List<Achievement>> userAchievements = new HashMap<>();
        Map<User, Integer> achievementCounts = new HashMap<>();
        
        for (User member : group.getMembers()) {
            List<Achievement> achievements = member.getAchievementData().stream()
                .map(GameAchievement::getAchievement)
                .collect(Collectors.toList());
            userAchievements.put(member, achievements);
            achievementCounts.put(member, achievements.size());
        }
        
        model.addAttribute("userAchievements", userAchievements);
        model.addAttribute("achievementCounts", achievementCounts);
        model.addAttribute("group", group);
        return "achievement-comparison";
    }

    // ==================== TAG FAVORITE GENRES (3.1.24) ====================
    
    @GetMapping("/profile/genres")
    public String showGenreTags(HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute(SESSION_DB_USER);
        if (currentUser == null) return "redirect:/";
        
        List<String> availableGenres = Arrays.asList(
            "Action", "RPG", "Strategy", "Shooter", "Adventure", 
            "Puzzle", "Simulation", "Sports", "Racing", "Fighting"
        );
        
        model.addAttribute("availableGenres", availableGenres);
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
        Group group = sqlHandler.getGroupById(groupId);
        if (group == null) return "redirect:/groups";
        
        List<GroupSession> sessions = sqlHandler.getGroupSessionsByGroup(group);
        model.addAttribute("sessions", sessions);
        model.addAttribute("group", group);
        return "group-calendar";
    }
    
    @PostMapping("/groups/{groupId}/sessions/schedule")
    public String scheduleGameSession(@PathVariable int groupId,
                                      @RequestParam int gameId,
                                      @RequestParam String scheduledDateTime,
                                      @RequestParam int durationMinutes,
                                      HttpSession session) {
        User currentUser = (User) session.getAttribute(SESSION_DB_USER);
        if (currentUser == null) return "redirect:/";
        
        Group group = sqlHandler.getGroupById(groupId);
        Game game = sqlHandler.getGameById(gameId);
        
        if (group == null || game == null) return "redirect:/groups/" + groupId;
        
        GroupSession groupSession = new GroupSession(group);
        groupSession.setGame(game);
        groupSession.setScheduledDate(LocalDateTime.parse(scheduledDateTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        groupSession.setDuration(durationMinutes);
        groupSession.setActive(true);
        
        sqlHandler.createGroupSession(groupSession);
        return "redirect:/groups/" + groupId + "/sessions";
    }

    // ==================== EXPORT USER REPORT (3.1.32) ====================
    
    @GetMapping("/profile/export")
    public ResponseEntity<byte[]> exportUserReport(HttpSession session) {
        User currentUser = (User) session.getAttribute(SESSION_DB_USER);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        var result = exportCSV.exportData(currentUser);
        
        if (result.isSuccess() && result.getOutputFilePath() != null) {
            try {
                File file = new File(result.getOutputFilePath());
                byte[] contents = Files.readAllBytes(Paths.get(file.getAbsolutePath()));
                
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.TEXT_PLAIN);
                headers.setContentDispositionFormData("attachment", "user_report_" + currentUser.getUserID() + ".csv");
                
                return new ResponseEntity<>(contents, headers, HttpStatus.OK);
            } catch (IOException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        }
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    @GetMapping("/groups/{groupId}/generate-invite")
@ResponseBody
public String generateInviteLink(@PathVariable int groupId, HttpSession session) {
    User currentUser = (User) session.getAttribute(SESSION_DB_USER);
    if (currentUser == null) return "Unauthorized";
    
    Group group = sqlHandler.getGroupById(groupId);
    if (group == null || group.getGroupOwner().getUserID() != currentUser.getUserID()) {
        return "Unauthorized";
    }
    
    // Generate a unique token
    String token = UUID.randomUUID().toString();
    
    // Create a join request with the token (optional: store it)
    GroupJoinRequest request = new GroupJoinRequest(group, null, token);
    sqlHandler.createGroupJoinRequest(request);
    
    // Return the full invite URL
    String inviteUrl = "/groups/join/" + token;
    return inviteUrl;
}

    // ==================== ACTIVITY SUMMARIES (3.1.26, 3.1.27) ====================
    
    @GetMapping("/profile/activity/weekly")
    public String getWeeklyActivity(HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute(SESSION_DB_USER);
        if (currentUser == null) return "redirect:/";
        
        String summary = generateActivitySummary(currentUser, "weekly");
        model.addAttribute("summary", summary);
        model.addAttribute("period", "Weekly");
        return "activity-summary";
    }
    
    @GetMapping("/profile/activity/monthly")
    public String getMonthlyActivity(HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute(SESSION_DB_USER);
        if (currentUser == null) return "redirect:/";
        
        String summary = generateActivitySummary(currentUser, "monthly");
        model.addAttribute("summary", summary);
        model.addAttribute("period", "Monthly");
        return "activity-summary";
    }
    
    private String generateActivitySummary(User user, String period) {
        StringBuilder summary = new StringBuilder();
        summary.append("=== ").append(period.toUpperCase()).append(" ACTIVITY SUMMARY ===\n");
        summary.append("User: ").append(user.getUserProfile().getProfileName()).append("\n");
        summary.append("Steam ID: ").append(user.getSteamID()).append("\n\n");
        
        List<GameAchievement> achievements = user.getAchievementData();
        if (achievements != null) {
            int totalPlaytime = achievements.stream()
                .mapToInt(ga -> ga.getGame().getPlaytime())
                .sum();
            
            summary.append("Total Playtime: ").append(totalPlaytime / 60).append(" hours\n");
            summary.append("Games Played: ").append(achievements.size()).append("\n");
            summary.append("Achievements Unlocked: ").append(achievements.size()).append("\n");
        }
        
        return summary.toString();
    }


}

// Add this method to SQLHandler if missing
// public GroupJoinRequest getGroupJoinRequestById(int requestId) { ... }