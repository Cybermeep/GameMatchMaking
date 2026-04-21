package edu.isu.gamematch.controller;

import edu.isu.gamematch.GroupVote;
import edu.isu.gamematch.*;
import edu.isu.gamematch.service.GroupService;
import edu.isu.gamematch.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.*;

/**
 * REST + Thymeleaf controller for all group-related SRS features.
 * All endpoints require an authenticated session (db_user attribute).
 */
@Controller
@RequestMapping("/group")
public class GroupController {

    private static final Logger log = LoggerFactory.getLogger(GroupController.class);

    @Autowired private GroupService groupService;
    @Autowired private UserService userService;

    private User currentUser(HttpSession session) {
        return (User) session.getAttribute("db_user");
    }

    private ResponseEntity<?> unauthorized() {
        return ResponseEntity.status(401).body("Not authenticated");
    }

    // FR 3.1.4 – Create Group
    @PostMapping("/create")
    @ResponseBody
    public ResponseEntity<?> createGroup(@RequestParam String name, HttpSession session) {
        User user = currentUser(session);
        if (user == null) return unauthorized();
        Group group = groupService.createGroup(name, user);
        return ResponseEntity.ok(Map.of("groupId", group.getGroupID(), "name", group.getGroupName()));
    }

    // FR 3.1.5 – Delete Group
    @DeleteMapping("/{groupId}")
    @ResponseBody
    public ResponseEntity<?> deleteGroup(@PathVariable int groupId, HttpSession session) {
        User user = currentUser(session);
        if (user == null) return unauthorized();
        boolean ok = groupService.deleteGroup(groupId);
        return ok ? ResponseEntity.ok("Group deleted") : ResponseEntity.notFound().build();
    }

    // FR 3.1.6 – Transfer Ownership
    @PostMapping("/{groupId}/transfer")
    @ResponseBody
    public ResponseEntity<?> transferOwnership(@PathVariable int groupId,
                                               @RequestParam int newOwnerId,
                                               HttpSession session) {
        User user = currentUser(session);
        if (user == null) return unauthorized();
        User newOwner = userService.findOrCreateUser(String.valueOf(newOwnerId), "");
        boolean ok = groupService.transferOwnership(groupId, newOwner);
        return ok ? ResponseEntity.ok("Ownership transferred") : ResponseEntity.badRequest().body("Failed");
    }

    // FR 3.1.7 – Generate Invite Link
    @GetMapping("/{groupId}/invite")
    @ResponseBody
    public ResponseEntity<?> inviteLink(@PathVariable int groupId, HttpSession session) {
        User user = currentUser(session);
        if (user == null) return unauthorized();
        String link = groupService.getInviteLink(groupId);
        return link != null ? ResponseEntity.ok(Map.of("link", link)) : ResponseEntity.notFound().build();
    }

    // FR 3.1.8 – Retrieve Group Members
    @GetMapping("/{groupId}/members")
    @ResponseBody
    public ResponseEntity<?> getMembers(@PathVariable int groupId, HttpSession session) {
        User user = currentUser(session);
        if (user == null) return unauthorized();
        Set<User> members = groupService.getMembers(groupId);
        return ResponseEntity.ok(members);
    }

    // FR 3.1.9 – Join Group via Token
    @GetMapping("/join")
    public String joinByToken(@RequestParam String token, HttpSession session, Model model) {
        User user = currentUser(session);
        if (user == null) return "redirect:/";
        boolean ok = groupService.joinByToken(user, token);
        model.addAttribute("joined", ok);
        model.addAttribute("message", ok ? "Successfully joined group!" : "Invalid or expired invite link.");
        return "join-result";
    }

    // FR 3.1.10 – Leave Group
    @PostMapping("/{groupId}/leave")
    @ResponseBody
    public ResponseEntity<?> leaveGroup(@PathVariable int groupId, HttpSession session) {
        User user = currentUser(session);
        if (user == null) return unauthorized();
        boolean ok = groupService.leaveGroup(user, groupId);
        return ok ? ResponseEntity.ok("Left group") : ResponseEntity.badRequest().body("Cannot leave group");
    }

    // FR 3.1.11 – Remove Group Member
    @DeleteMapping("/{groupId}/members/{memberId}")
    @ResponseBody
    public ResponseEntity<?> removeMember(@PathVariable int groupId,
                                          @PathVariable int memberId,
                                          HttpSession session) {
        User user = currentUser(session);
        if (user == null) return unauthorized();
        // Fetch member — reuse searchUserBySteamId as integer DB id lookup via getAllUsers
        User member = groupService.getMembers(groupId).stream()
                .filter(m -> m.getUserID() == memberId).findFirst().orElse(null);
        if (member == null) return ResponseEntity.notFound().build();
        boolean ok = groupService.removeMember(member, groupId);
        return ok ? ResponseEntity.ok("Member removed") : ResponseEntity.badRequest().body("Failed");
    }

    // FR 3.1.17 – Accept Join Request
    @PostMapping("/requests/{requestId}/accept")
    @ResponseBody
    public ResponseEntity<?> acceptRequest(@PathVariable int requestId, HttpSession session) {
        User user = currentUser(session);
        if (user == null) return unauthorized();
        boolean ok = groupService.acceptJoinRequest(requestId);
        return ok ? ResponseEntity.ok("Request accepted") : ResponseEntity.badRequest().body("Failed");
    }

    // FR 3.1.18 – Decline Join Request
    @PostMapping("/requests/{requestId}/decline")
    @ResponseBody
    public ResponseEntity<?> declineRequest(@PathVariable int requestId, HttpSession session) {
        User user = currentUser(session);
        if (user == null) return unauthorized();
        boolean ok = groupService.declineJoinRequest(requestId);
        return ok ? ResponseEntity.ok("Request declined") : ResponseEntity.badRequest().body("Failed");
    }

    // FR 3.1.19 – Retrieve Pending Requests
    @GetMapping("/{groupId}/requests")
    @ResponseBody
    public ResponseEntity<?> pendingRequests(@PathVariable int groupId, HttpSession session) {
        User user = currentUser(session);
        if (user == null) return unauthorized();
        List<JoinRequest> requests = groupService.getPendingRequests(groupId);
        return ResponseEntity.ok(requests);
    }

    // Request to join (creates a JoinRequest for owner review)
    @PostMapping("/{groupId}/request-join")
    @ResponseBody
    public ResponseEntity<?> requestJoin(@PathVariable int groupId, HttpSession session) {
        User user = currentUser(session);
        if (user == null) return unauthorized();
        JoinRequest req = groupService.requestToJoin(user, groupId);
        return req != null ? ResponseEntity.ok("Join request sent")
                           : ResponseEntity.badRequest().body("Could not create request");
    }

    // FR 3.1.3 – Filtered Games List
    @GetMapping("/{groupId}/games/filtered")
    @ResponseBody
    public ResponseEntity<?> filteredGames(@PathVariable int groupId, HttpSession session) {
        User user = currentUser(session);
        if (user == null) return unauthorized();
        List<Game> games = groupService.getFilteredGamesList(groupId);
        return ResponseEntity.ok(games);
    }

    // FR 3.1.20 – Set Playtime Requirement
    @PostMapping("/{groupId}/settings/playtime")
    @ResponseBody
    public ResponseEntity<?> setPlaytime(@PathVariable int groupId,
                                         @RequestParam int minHours,
                                         HttpSession session) {
        User user = currentUser(session);
        if (user == null) return unauthorized();
        boolean ok = groupService.setPlaytimeRequirement(groupId, minHours);
        return ok ? ResponseEntity.ok("Playtime requirement updated") : ResponseEntity.notFound().build();
    }

    // FR 3.1.16 – Session History
    @GetMapping("/{groupId}/sessions")
    @ResponseBody
    public ResponseEntity<?> sessionHistory(@PathVariable int groupId, HttpSession session) {
        User user = currentUser(session);
        if (user == null) return unauthorized();
        return ResponseEntity.ok(groupService.getSessionHistory(groupId));
    }

    // FR 3.1.30 – Schedule Session
    @PostMapping("/{groupId}/sessions/schedule")
    @ResponseBody
    public ResponseEntity<?> scheduleSession(@PathVariable int groupId,
                                             @RequestParam(defaultValue = "0") int gameId,
                                             @RequestParam String scheduledAt,
                                             @RequestParam(defaultValue = "60") int durationMinutes,
                                             HttpSession session) {
        User user = currentUser(session);
        if (user == null) return unauthorized();
        LocalDateTime when = LocalDateTime.parse(scheduledAt);
        GroupSession s = groupService.scheduleSession(groupId, gameId, when, durationMinutes);
        return s != null ? ResponseEntity.ok(Map.of("sessionId", s.getSessionID()))
                         : ResponseEntity.badRequest().body("Failed to schedule session");
    }

    // FR 3.1.31 – Group Calendar
    @GetMapping("/{groupId}/calendar")
    @ResponseBody
    public ResponseEntity<?> getCalendar(@PathVariable int groupId, HttpSession session) {
        User user = currentUser(session);
        if (user == null) return unauthorized();
        return ResponseEntity.ok(groupService.getCalendar(groupId));
    }

    // FR 3.1.23 – Randomize Games
    @GetMapping("/{groupId}/games/random")
    @ResponseBody
    public ResponseEntity<?> randomize(@PathVariable int groupId, HttpSession session) {
        User user = currentUser(session);
        if (user == null) return unauthorized();
        return ResponseEntity.ok(groupService.randomize(groupId));
    }

    // FR 3.1.28 – Add Group Preference
    @PostMapping("/{groupId}/preferences")
    @ResponseBody
    public ResponseEntity<?> addPreference(@PathVariable int groupId,
                                           @RequestParam String genre,
                                           HttpSession session) {
        User user = currentUser(session);
        if (user == null) return unauthorized();
        boolean ok = groupService.applyPreference(groupId, genre);
        return ok ? ResponseEntity.ok("Preference applied") : ResponseEntity.notFound().build();
    }

    // FR 3.1.29 – Share Final Games List
    @PostMapping("/{groupId}/games/share")
    @ResponseBody
    public ResponseEntity<?> shareGames(@PathVariable int groupId, HttpSession session) {
        User user = currentUser(session);
        if (user == null) return unauthorized();
        String link = groupService.shareGamesList(groupId);
        return link != null ? ResponseEntity.ok(Map.of("shareLink", link))
                            : ResponseEntity.notFound().build();
    }

    // Public shared list (no auth required) – FR 3.1.29
    @GetMapping("/shared/games")
    @ResponseBody
    public ResponseEntity<?> viewSharedGames(@RequestParam String token) {
        List<Game> games = groupService.getSharedGamesList(token);
        return games != null ? ResponseEntity.ok(games)
                             : ResponseEntity.notFound().build();
    }

    // FR 3.1.12 – Vote
@PostMapping("/{groupId}/vote")
@ResponseBody
public ResponseEntity<?> castVote(@PathVariable int groupId,
                                  @RequestParam int voteId,
                                  @RequestParam int gameId,
                                  HttpSession session) {
    User user = currentUser(session);
    if (user == null) return unauthorized();

    // If voteId is 0, create a new vote record for this user in this group
    if (voteId == 0) {
        Group group = groupService.getGroupById(groupId);
        if (group == null) return ResponseEntity.notFound().build();
        GroupVote newVote = groupService.createVote(group, user);
        voteId = newVote.getVoteID();
    }

    boolean ok = groupService.castVote(voteId, gameId);
    return ok ? ResponseEntity.ok("Vote cast") : ResponseEntity.badRequest().body("Vote failed");
}

    @GetMapping("/{groupId}/vote/results")
    @ResponseBody
    public ResponseEntity<?> voteResults(@PathVariable int groupId, HttpSession session) {
        User user = currentUser(session);
        if (user == null) return unauthorized();
        Map<Game, Integer> tally = groupService.tallyVotes(groupId);
        return ResponseEntity.ok(tally);
    }

    // My groups endpoint (for dashboard JS)
    @GetMapping("/my-groups")
    @ResponseBody
    public ResponseEntity<?> myGroups(HttpSession session) {
        User user = currentUser(session);
        if (user == null) return unauthorized();
        if (user.getGroupData() == null) return ResponseEntity.ok(Collections.emptyList());
        List<Map<String, Object>> result = new ArrayList<>();
        for (Group g : user.getGroupData()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("groupId", g.getGroupID());
            entry.put("name", g.getGroupName() != null ? g.getGroupName() : "Group#" + g.getGroupID());
            entry.put("memberCount", g.getMembers().size());
            result.add(entry);
        }
        return ResponseEntity.ok(result);
    }
}
