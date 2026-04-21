package edu.isu.gamematch.service;

import edu.isu.gamematch.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Spring service layer for user-profile SRS requirements:
 * FR 3.1.15, 3.1.21, 3.1.22a, 3.1.22b, 3.1.24, 3.1.25, 3.1.26, 3.1.27, 3.1.32, 3.1.33
 */
@Service
public class UserProfileService {

    @Autowired
    private SQLHandler db;

    private UserProfileOperations opsFor(User user) {
        return new UserProfileOperations(user.getUserProfile(), db);
    }

    // FR 3.1.15 – Retrieve another user's profile
    public UserProfile getUserProfile(String profileName) {
        User user = db.searchUser(profileName);
        return user != null ? user.getUserProfile() : null;
    }

    // FR 3.1.21 – Search for a user (by name or steam name)
    public User searchUser(String query) {
        User byProfile = db.searchUser(query);
        if (byProfile != null) return byProfile;
        return db.searchUserBySteamName(query);
    }

    // FR 3.1.22a – Compare achievements in a group for a given game
    public Map<String, List<String>> compareAchievements(int groupId, int gameId) {
        Group group = db.getGroupById(groupId);
        Game game = db.getGameById(gameId);
        if (group == null || game == null) return java.util.Collections.emptyMap();
        // Use any member's ops context — the method only reads group members
        User first = group.getMembers().iterator().next();
        return opsFor(first).compareAchievementsInGroup(group, game);
    }

    // FR 3.1.22b – Delete user data
    public boolean deleteUserData(User user) {
        db.deleteUser(user);
        if (user.getUserProfile() != null) db.deleteUserProfile(user.getUserProfile());
        return true;
    }

    // FR 3.1.24 – Add / remove genre preference
    public boolean addGenrePreference(User user, String genre) {
        String result = opsFor(user).addGenrePreference(genre);
        if (result == null) return false;
        db.updateUserProfile(user.getUserProfile());
        return true;
    }

    public boolean removeGenrePreference(User user, String genre) {
        opsFor(user).removeGenrePreference(genre);
        db.updateUserProfile(user.getUserProfile());
        return true;
    }

    // FR 3.1.25 – Retrieve mutual friends
    public List<User> getMutualFriends(User user, int otherUserId) {
        User other = db.getAllUsers().stream()
                .filter(u -> u.getUserID() == otherUserId)
                .findFirst().orElse(null);
        if (other == null) return java.util.Collections.emptyList();
        return opsFor(user).retrieveMutualFriends(other);
    }

    // FR 3.1.26 – Weekly activity summary
    public String getWeeklySummary(User user) {
        return opsFor(user).generateWeeklyActivitySummary();
    }

    // FR 3.1.27 – Monthly activity summary
    public String getMonthlySummary(User user) {
        return opsFor(user).generateMonthlyActivitySummary();
    }

    // FR 3.1.32 – User profile report
    public String generateProfileReport(User user) {
        return opsFor(user).generateUserProfileReport();
    }
}
