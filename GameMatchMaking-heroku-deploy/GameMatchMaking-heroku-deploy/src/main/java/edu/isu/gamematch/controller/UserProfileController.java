package edu.isu.gamematch.controller;

import edu.isu.gamematch.*;
import edu.isu.gamematch.service.UserProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;

/**
 * HTTP endpoints for user-profile SRS requirements:
 * FR 3.1.15, 3.1.21, 3.1.22a, 3.1.22b, 3.1.24, 3.1.25, 3.1.26, 3.1.27, 3.1.32
 */
@Controller
public class UserProfileController {

    @Autowired private UserProfileService profileService;

    private User currentUser(HttpSession session) {
        return (User) session.getAttribute("db_user");
    }

    private ResponseEntity<?> unauthorized() {
        return ResponseEntity.status(401).body("Not authenticated");
    }

    // FR 3.1.15 – View another user's profile
    @GetMapping("/profile/{profileName}")
    public String viewProfile(@PathVariable String profileName, Model model, HttpSession session) {
        if (currentUser(session) == null) return "redirect:/";
        UserProfile profile = profileService.getUserProfile(profileName);
        model.addAttribute("profile", profile);
        return "user-profile";
    }

    // FR 3.1.21 – Search for a user
    @GetMapping("/users/search")
    @ResponseBody
    public ResponseEntity<?> searchUser(@RequestParam String query, HttpSession session) {
        if (currentUser(session) == null) return unauthorized();
        User found = profileService.searchUser(query);
        return found != null ? ResponseEntity.ok(Map.of(
                "userId", found.getUserID(),
                "profileName", found.getUserProfile() != null ? found.getUserProfile().getProfileName() : ""
        )) : ResponseEntity.notFound().build();
    }

    // FR 3.1.22a – Compare achievements in group
    @GetMapping("/group/{groupId}/achievements")
    @ResponseBody
    public ResponseEntity<?> compareAchievements(@PathVariable int groupId,
                                                  @RequestParam int gameId,
                                                  HttpSession session) {
        if (currentUser(session) == null) return unauthorized();
        Map<String, List<String>> comparison = profileService.compareAchievements(groupId, gameId);
        return ResponseEntity.ok(comparison);
    }

    // FR 3.1.22b – Delete user data
    @DeleteMapping("/profile/data")
    @ResponseBody
    public ResponseEntity<?> deleteUserData(HttpSession session) {
        User user = currentUser(session);
        if (user == null) return unauthorized();
        profileService.deleteUserData(user);
        session.invalidate();
        return ResponseEntity.ok("Your data has been deleted.");
    }

    // FR 3.1.24 – Add genre preference
    @PostMapping("/profile/genres")
    @ResponseBody
    public ResponseEntity<?> addGenre(@RequestParam String genre, HttpSession session) {
        User user = currentUser(session);
        if (user == null) return unauthorized();
        boolean ok = profileService.addGenrePreference(user, genre);
        return ok ? ResponseEntity.ok("Genre added") : ResponseEntity.badRequest().body("Already added or invalid");
    }

    @DeleteMapping("/profile/genres")
    @ResponseBody
    public ResponseEntity<?> removeGenre(@RequestParam String genre, HttpSession session) {
        User user = currentUser(session);
        if (user == null) return unauthorized();
        profileService.removeGenrePreference(user, genre);
        return ResponseEntity.ok("Genre removed");
    }

    // FR 3.1.25 – Retrieve mutual friends
    @GetMapping("/users/{otherUserId}/mutual-friends")
    @ResponseBody
    public ResponseEntity<?> mutualFriends(@PathVariable int otherUserId, HttpSession session) {
        User user = currentUser(session);
        if (user == null) return unauthorized();
        List<User> mutual = profileService.getMutualFriends(user, otherUserId);
        return ResponseEntity.ok(mutual);
    }

    // FR 3.1.26 – Weekly activity summary
    @GetMapping("/profile/activity/weekly")
    @ResponseBody
    public ResponseEntity<?> weeklySummary(HttpSession session) {
        User user = currentUser(session);
        if (user == null) return unauthorized();
        return ResponseEntity.ok(Map.of("summary", profileService.getWeeklySummary(user)));
    }

    // FR 3.1.27 – Monthly activity summary
    @GetMapping("/profile/activity/monthly")
    @ResponseBody
    public ResponseEntity<?> monthlySummary(HttpSession session) {
        User user = currentUser(session);
        if (user == null) return unauthorized();
        return ResponseEntity.ok(Map.of("summary", profileService.getMonthlySummary(user)));
    }

    // FR 3.1.32 – Generate profile report
    @GetMapping("/profile/report")
    @ResponseBody
    public ResponseEntity<?> profileReport(HttpSession session) {
        User user = currentUser(session);
        if (user == null) return unauthorized();
        return ResponseEntity.ok(Map.of("report", profileService.generateProfileReport(user)));
    }
}
