package edu.isu.gamematch;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Covers FR 3.1.15, 3.1.21, 3.1.22 (achievements), 3.1.24, 3.1.25,
 *          3.1.26, 3.1.27, 3.1.32, 3.1.33
 */
public class UserProfileOperations {
    private UserProfile userProfile;
    private DataHandler dataHandler;

    public UserProfileOperations(UserProfile userProfile, DataHandler dataHandler) {
        this.userProfile = userProfile;
        this.dataHandler = dataHandler;
    }

    // ==================================================
    //  FR 3.1.33 – Create User Profile
    // ==================================================
    public UserProfile createProfile(String profileName, User user) {
        UserProfile profile = new UserProfile(profileName, user);
        user.setUserProfile(profile);
        return profile;
    }

    // ==================================================
    //  FR 3.1.15 – Retrieve Another User's Profile
    // ==================================================
    public UserProfile retrieveUserProfile(String profileName) {
        User found = dataHandler.searchUser(profileName);
        return found != null ? found.getUserProfile() : null;
    }

    // ==================================================
    //  FR 3.1.21 – Search For a User
    // ==================================================
    public User searchUserProfile(String profileName) {
        return dataHandler.searchUser(profileName);
    }

    // ==================================================
    //  FR 3.1.24 – Tag Favorite Game Genres
    // ==================================================
    public String addGenrePreference(String genre) {
        if (genre == null || this.userProfile.getFavoriteGenres().contains(genre)) return null;
        this.userProfile.getFavoriteGenres().add(genre);
        return genre;
    }

    public String removeGenrePreference(String genre) {
        this.userProfile.getFavoriteGenres().remove(genre);
        return genre;
    }

    public List<String> getFavoriteGenres() {
        return userProfile.getFavoriteGenres();
    }

    // ==================================================
    //  FR 3.1.25 – Retrieve Mutual Friends
    // ==================================================
    public List<User> retrieveMutualFriends(User otherUser) {
        List<User> mutual = new ArrayList<>();
        for (User friend : this.userProfile.getUser().getFriends()) {
            if (otherUser.getFriends().contains(friend)) {
                mutual.add(friend);
            }
        }
        return mutual;
    }

    // ==================================================
    //  FR 3.1.22a – Compare Achievements in a Group
    // ==================================================
    /**
     * Returns a map of username -> list of achievement names for a given game
     * so the caller can display a comparison across group members.
     */
    public Map<String, List<String>> compareAchievementsInGroup(Group group, Game game) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        if (group == null || game == null) return result;

        for (User member : group.getMembers()) {
            List<String> achieved = member.getAchievementData().stream()
                    .filter(ga -> ga.getGame() != null && ga.getGame().equals(game))
                    .map(ga -> ga.getAchievement().getAchievementName())
                    .collect(Collectors.toList());
            String name = member.getUserProfile() != null
                    ? member.getUserProfile().getProfileName()
                    : "User#" + member.getUserID();
            result.put(name, achieved);
        }
        return result;
    }

    // ==================================================
    //  FR 3.1.26 – Share Weekly Activity Summary
    //  FR 3.1.27 – Share Monthly Activity Summary
    // ==================================================

    /**
     * Generates an activity summary for the user covering [from, to].
     * Uses the user's GameAchievement and Group history stored in the DB.
     */
    private String buildActivitySummary(LocalDateTime from, LocalDateTime to, String periodLabel) {
        User user = userProfile.getUser();
        int achievementCount = (int) user.getAchievementData().stream()
                .filter(Objects::nonNull)
                .count();

        int groupCount = user.getGroupData() != null ? user.getGroupData().size() : 0;

        int sessionCount = 0;
        if (user.getGroupData() != null) {
            for (Group g : user.getGroupData()) {
                sessionCount += g.getSessions().stream()
                        .filter(s -> s.getScheduledDate() != null
                                && !s.getScheduledDate().isBefore(from)
                                && !s.getScheduledDate().isAfter(to))
                        .count();
            }
        }

        return String.format(
            "%s Activity Summary for %s:\n" +
            "  Period: %s – %s\n" +
            "  Groups joined: %d\n" +
            "  Sessions played: %d\n" +
            "  Achievements recorded: %d\n" +
            "  Favorite genres: %s",
            periodLabel,
            userProfile.getProfileName(),
            from.toLocalDate(), to.toLocalDate(),
            groupCount, sessionCount, achievementCount,
            String.join(", ", userProfile.getFavoriteGenres())
        );
    }

    /** FR 3.1.26 */
    public String generateWeeklyActivitySummary() {
        LocalDateTime now = LocalDateTime.now();
        return buildActivitySummary(now.minusDays(7), now, "Weekly");
    }

    /** FR 3.1.27 */
    public String generateMonthlyActivitySummary() {
        LocalDateTime now = LocalDateTime.now();
        return buildActivitySummary(now.minusDays(30), now, "Monthly");
    }

    // ==================================================
    //  FR 3.1.32 – Generate User Profile Report
    // ==================================================
    public String generateUserProfileReport() {
        User user = userProfile.getUser();
        StringBuilder sb = new StringBuilder();
        sb.append("=== User Profile Report ===\n");
        sb.append("Name       : ").append(userProfile.getProfileName()).append("\n");
        sb.append("Steam ID   : ").append(user.getSteamID()).append("\n");
        sb.append("Genres     : ").append(String.join(", ", userProfile.getFavoriteGenres())).append("\n");

        if (user.getGroupData() != null && !user.getGroupData().isEmpty()) {
            sb.append("Groups     :\n");
            for (Group g : user.getGroupData()) {
                sb.append("  - ").append(g.getGroupName() != null ? g.getGroupName() : "Group#" + g.getGroupID())
                  .append(" (").append(g.getMembers().size()).append(" members)\n");
            }
        }

        int totalAchievements = user.getAchievementData() != null ? user.getAchievementData().size() : 0;
        sb.append("Achievements recorded: ").append(totalAchievements).append("\n");
        sb.append("Generated  : ").append(LocalDateTime.now()).append("\n");
        return sb.toString();
    }

    // ==================================================
    //  Misc helpers
    // ==================================================
    public void setProfileName(String newName) { this.userProfile.setProfileName(newName); }
    public void setUser(User newUser) { this.userProfile.setUser(newUser); }
    public UserProfile getUserProfile() { return this.userProfile; }

    /** FR 3.1.22b – Delete user data (delegate to DB layer via DataHandler) */
    public boolean deleteUserData() {
        return dataHandler.removeUsers() == null ? true : false;
    }
}
