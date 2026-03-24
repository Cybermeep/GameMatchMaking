import java.util.List;
import java.util.ArrayList;

public class User {
    private int userID;
    private long steamID;
    private UserDataAccess userData; // why have a UserDataAccess object per user?
    private List<Group> groupData;
    private List<GameAchievement> achievementData;
    private List<User> friends;

    public User(int userID, int steamID, List<Group> groupData, List<GameAchievement> achievementData) {
        this.userID = userID;
        this.steamID = steamID;
        this.groupData = groupData;
        this.achievementData = achievementData;
        this.friends = new ArrayList<User>();
    }

    public int getUserID() { 
        return userID;
    }
    public long getSteamID() { 
        return steamID;
    }
    public List<Group> getGroupData() { 
        return new ArrayList<>(groupData);
    }
    public List<GameAchievement> getAchievementData() { 
        return new ArrayList<>(achievementData);
    }
    public List<User> getFriends()
    {
        return this.friends;
    }
    
    public void setUserID(int userID) { 
        this.userID = userID;
    }
    public void setSteamID(long steamID) { 
        this.steamID = steamID;
    }
    public void setGroupData(List<Group> groupData) { 
        this.groupData = new ArrayList<>(groupData);
    }
    public void setAchievementData(List<GameAchievement> achievementData) { 
        this.achievementData = new ArrayList<>(achievementData);
    }
    public void addFriend(User newFriend)
    {
        if(!friends.contains(newFriend))
        {
            this.friends.add(newFriend);
        }
    }
    public void removeFriend(User oldFriend)
    {
        this.friends.remove(oldFriend);
    }
}