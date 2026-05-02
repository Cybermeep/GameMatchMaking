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

    @Column(name = "persona_name", nullable = false)
    private String personaName;   // Added required by dashboard

    @Column(name = "local_username", unique = true)
    private String localUsername; // null for Steam only users

    @Column(name = "password_hash")
    private String passwordHash;  // SHA-256 for local accounts

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

    @Transient
    private boolean online;       // set by controller / tracker


    @Override
public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof User)) return false;
    return this.userID == ((User) o).userID;
}

@Override
public int hashCode() {
    return Integer.hashCode(userID);
}

    // Constructors
    public User() {
        this.groupData = new ArrayList<>();
        this.achievementData = new ArrayList<>();
        this.friends = new ArrayList<>();
        this.online = false;
    }

    public User(int userID, Long steamID, String personaName,
                List<Group> groupData, List<GameAchievement> achievementData) {
        this();
        this.userID = userID;
        this.steamID = steamID;
        this.personaName = personaName;
        this.groupData = groupData;
        this.achievementData = achievementData;
    }

    // Getters and setters existing
    public int getUserID() { return userID; }
    public void setUserID(int userID) { this.userID = userID; }
    public Long getSteamID() { return steamID; }
    public void setSteamID(Long steamID) { this.steamID = steamID; }
    public String getPersonaName() { return personaName; }
    public void setPersonaName(String personaName) { this.personaName = personaName; }
    public List<Group> getGroupData() { return new ArrayList<>(groupData); }
    public void setGroupData(List<Group> groupData) { this.groupData = new ArrayList<>(groupData); }
    public List<GameAchievement> getAchievementData() { return new ArrayList<>(achievementData); }
    public void setAchievementData(List<GameAchievement> achievementData) { this.achievementData = new ArrayList<>(achievementData); }
    public List<User> getFriends() { return this.friends; }
    public void addFriend(User newFriend) {
        if (!friends.contains(newFriend)) this.friends.add(newFriend);
    }
    public void removeFriend(User oldFriend) { this.friends.remove(oldFriend); }
    public UserProfile getUserProfile() { return userProfile; }
    public void setUserProfile(UserProfile userProfile) {
        this.userProfile = userProfile;
        if (userProfile != null) userProfile.setUser(this);
    }

    // New fields getters/setters
    public String getLocalUsername() { return localUsername; }
    public void setLocalUsername(String localUsername) { this.localUsername = localUsername; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public boolean isOnline() { return online; }
    public void setOnline(boolean online) { this.online = online; }
}