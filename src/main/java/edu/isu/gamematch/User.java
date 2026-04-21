package edu.isu.gamematch;

import javax.persistence.*;
import java.util.List;
import java.util.ArrayList;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private int userID;

    @Column(name = "steam_id", unique = true)
    private long steamID;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<UserGame> userGames;

    @ManyToMany
    @JoinTable(
        name = "user_groups",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "group_id")
    )
    private List<Group> groupData;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<GameAchievement> achievementData;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private UserProfile userProfile;

    @ManyToMany
    @JoinTable(
        name = "user_friends",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "friend_id")
    )
    private List<User> friends;

    // Default constructor for JPA
    public User() {
    }

    public User(int userID, long steamID, List<UserGame> userGames, List<Group> groupData, List<GameAchievement> achievementData, UserProfile userProfile) {
        this.userID = userID;
        this.steamID = steamID;
        this.userGames = new ArrayList<>(userGames);
        this.groupData = new ArrayList<>(groupData);
        this.achievementData = new ArrayList<>(achievementData);
        this.friends = new ArrayList<>();
        setUserProfile(userProfile);
    }

    public int getUserID()
    {
        return userID;
    }

    public long getSteamID()
    {
        return steamID;
    }

    public List<UserGame> getUserGames()
    {
        return new ArrayList<>(userGames);
    }
    public List<Group> getGroupData()
    {
        return new ArrayList<>(groupData);
    }
    public List<GameAchievement> getAchievementData()
    {
        return new ArrayList<>(achievementData);
    }
    public List<User> getFriends()
    {
        return new ArrayList<>(friends);
    }

    public void setUserID(int userID)
    {
        this.userID = userID;
    }
    public void setSteamID(long steamID)
    {
        this.steamID = steamID;
    }
    public void setUserGames(List<UserGame> userGames)
    {
        this.userGames = new ArrayList<>(userGames);
    }
    public void setGroupData(List<Group> groupData)
    {
        this.groupData = new ArrayList<>(groupData);
    }
    public void setAchievementData(List<GameAchievement> achievementData)
    {
        this.achievementData = new ArrayList<>(achievementData);
    }
    public void setFriends(List<User> friends)
    {
        this.friends = new ArrayList<>(friends);
    }
    public UserProfile getUserProfile() {
        return userProfile;
    }
    public void setUserProfile(UserProfile userProfile) {
        this.userProfile = userProfile;
        if (userProfile != null) {
            userProfile.setUser(this);
        }
    }
}