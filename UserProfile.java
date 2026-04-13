package edu.isu.gamematch;
import java.util.ArrayList;
import java.util.List;

public class UserProfile
{
    public String profileName;
    private List<String> favoriteGenres;
    private User user;

    public UserProfile(String profileName, User user) 
    {
        this.profileName = profileName;
        this.user = user;
        this.favoriteGenres = new ArrayList<>();
    }

    public String getProfileName() {
        return profileName;
    }

    public List<String> getFavoriteGenres() {
        return favoriteGenres;
    }

    public User getUser() {
        return user;
    }

    public void setProfileName(String newName) {
        this.profileName = newName;
    }

    public void setUser(User user) {
        this.user = user;
    }
}