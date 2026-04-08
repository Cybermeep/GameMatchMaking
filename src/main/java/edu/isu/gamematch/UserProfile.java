package edu.isu.gamematch;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "user_profiles")
public class UserProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "profile_id")
    private int profileId;

    @Column(name = "profile_name", nullable = false)
    private String profileName;

    @ElementCollection
    @CollectionTable(name = "user_favorite_games", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "game_name")
    private List<String> favoriteGames = new ArrayList<>();

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    // Default constructor for JPA
    public UserProfile() {
    }

    public UserProfile(String profileName, User user)
    {
        this.profileName = profileName;
        this.user = user;
    }

    // Getters and setters
    public int getProfileId() {
        return profileId;
    }

    public void setProfileId(int profileId) {
        this.profileId = profileId;
    }

    public String getProfileName() {
        return profileName;
    }

    public void setProfileName(String newName) {
        this.profileName = newName;
    }

    public List<String> getFavoriteGames() {
        return favoriteGames;
    }

    public void setFavoriteGames(List<String> favoriteGames) {
        this.favoriteGames = favoriteGames;
    }

    public boolean addFavoriteGame(String game)
    {
        if (game == null || favoriteGames.contains(game))
        {
            return false;
        }
        favoriteGames.add(game);
        return true;
    }

    public boolean removeFavoriteGame(String game)
    {
        return favoriteGames.remove(game);
    }

    public User getUser()
    {
        return this.user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    // Additional methods
    public List<User> retrieveMutualFriends(User otherUser) {
        List<User> mutual = new ArrayList<>();
        for (User friend : this.user.getFriends()) {
            if (otherUser.getFriends().contains(friend)) {
                mutual.add(friend);
            }
        }
        return mutual;
    }
}