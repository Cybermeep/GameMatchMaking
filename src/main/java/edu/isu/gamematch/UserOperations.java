package edu.isu.gamematch;

import java.util.List;
import java.util.ArrayList;

public class UserOperations {

    public UserOperations() {
       
    }

    //compares user's achievements with the rest of the groups'
    //returns the achievements they all have
    public List<GameAchievement> compareAchievementsSame(User user, Group group) {
        List<GameAchievement> userAchievements = user.getAchievementData();
        List<GameAchievement> sameAchievements = new ArrayList<>(userAchievements);

        if (group.getMembers().contains(user)) {
            for (User member : group.getMembers()) {
                if (member.equals(user)) continue;
                
                List<GameAchievement> memberAchievements = member.getAchievementData();
                List<GameAchievement> toRemove = new ArrayList<>();

                for (GameAchievement userAchievement : sameAchievements) {
                    boolean memberHasIt = false;
                    for (GameAchievement memberAchievement : memberAchievements) {
                        if (userAchievement.getAchievement().getAchievementId() == memberAchievement.getAchievement().getAchievementId()) {
                            memberHasIt = true;
                            break;
                        }
                    }
                    if (!memberHasIt) {
                        toRemove.add(userAchievement);
                    }
                }
                sameAchievements.removeAll(toRemove);
            }
        }
        return sameAchievements;
    }

    //compares user's achievements with the rest of the groups'
    //returns the achievements they all have that the user does not have
    public List<GameAchievement> compareAchievementsDiff(User user, Group group) {
        List<GameAchievement> userAchievements = user.getAchievementData();
        List<GameAchievement> diffAchievements = new ArrayList<>();

        if (group.getMembers().contains(user)) {
            for (User member : group.getMembers()) {
                if (member.equals(user)) continue;

                for (GameAchievement memberAchievement : member.getAchievementData()) {
                    boolean userHasIt = false;
                    for (GameAchievement userAchievement : userAchievements) {
                        if (memberAchievement.getAchievement().getAchievementId() == userAchievement.getAchievement().getAchievementId()) {
                            userHasIt = true;
                            break;
                        }
                    }

                    boolean alreadyAdded = false;
                    for (GameAchievement diff : diffAchievements) {
                        if (diff.getAchievement().getAchievementId() == memberAchievement.getAchievement().getAchievementId()) {
                            alreadyAdded = true;
                            break;
                        }
                    }

                    if (!userHasIt && !alreadyAdded) {
                        diffAchievements.add(memberAchievement);
                    }
                }
            }
        }
        return diffAchievements;
    }

    //still needs implementation
    private boolean updateGroups(List<String> groupData) {
        return true;
    }

    public ExportData.ExportResult exportData(User user) {
        ExportCSV exporter = new ExportCSV();
        return exporter.exportData(user);
    }

    public int getSteamID(User user) {
        return (int) user.getSteamID();
    }
    
    //still needs implementation
    public boolean resynchronizeUserData(User user) {
       return true;
    }

    //still needs implementation
    public boolean resynchronizeInstalledGames(User user) {
       return true;
    }

    public void addFriend(User user, User newFriend) {
        List<User> friends = user.getFriends();
        if (!friends.contains(newFriend)) {
            friends.add(newFriend);
            user.setFriends(friends);
        }
    }

    public void removeFriend(User user, User oldFriend) {
        List<User> friends = user.getFriends();
        friends.remove(oldFriend);
        user.setFriends(friends);
    }
}
