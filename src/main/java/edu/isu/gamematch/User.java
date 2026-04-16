package edu.isu.gamematch;
import javax.persistence.*;
import java.util.List;
import java.util.ArrayList;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_seq")
    @SequenceGenerator(name = "user_seq", sequenceName = "USER_SEQ", allocationSize = 1)
    @Column(name = "user_id")
    private int userID;

    @Column(name = "steam_id", unique = true)
    private Long steamID;

    @ManyToMany
    @JoinTable(
        name = "user_groups",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "group_id")
    )
    private List<Group> groupData;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<GameAchievement> achievementData;

    @ManyToMany
    @JoinTable(
        name = "user_friends",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "friend_id")
    )
    private List<User> friends;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private UserProfile userProfile;

    // Default constructor required by JPA
    public User() {
        this.groupData = new ArrayList<>();
        this.achievementData = new ArrayList<>();
        this.friends = new ArrayList<>();
    }

    public User(int userID, Long steamID, List<Group> groupData, List<GameAchievement> achievementData) {
        this();
        this.userID = userID;
        this.steamID = steamID;
        this.groupData = groupData;
        this.achievementData = achievementData;
    }

    public int getUserID() { 
        return userID;
    }
    public Long getSteamID() { 
        return steamID;
    }
    public List<Group> getGroupData() { 
        return new ArrayList<>(groupData);
    }
    public List<GameAchievement> getAchievementData() { 
        return new ArrayList<>(achievementData);
    }
    public List<User> getFriends() {
        return this.friends;
    }
    
    public void setUserID(int userID) { 
        this.userID = userID;
    }
    public void setSteamID(Long steamID) { 
        this.steamID = steamID;
    }
    public void setGroupData(List<Group> groupData) { 
        this.groupData = new ArrayList<>(groupData);
    }
    public void setAchievementData(List<GameAchievement> achievementData) { 
        this.achievementData = new ArrayList<>(achievementData);
    }
    public void addFriend(User newFriend) {
        if(!friends.contains(newFriend)) {
            this.friends.add(newFriend);
        }
    }
    public void removeFriend(User oldFriend) {
        this.friends.remove(oldFriend);
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