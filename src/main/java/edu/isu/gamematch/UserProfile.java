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
    @CollectionTable(name = "user_favorite_genres", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "genre_name")
    private List<String> favoriteGenres = new ArrayList<>();

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
        this.favoriteGenres = new ArrayList<>();
    }

    public String getProfileName() {
        return profileName;
    }

    public ArrayList<String> getFavoriteGenres() {
        return favoriteGenres;
    }

    public User getUser() {
        return user;
    }
}