package edu.isu.gamematch;
import java.util.ArrayList;
import java.util.List;

public class UserProfileOperations
{
    private UserProfile userProfile;
    private DataHandler dataHandler;

    public UserProfileOperations(UserProfile userProfile, DataHandler dataHandler) {
        this.userProfile = userProfile;
        this.dataHandler = dataHandler;
    }

    public List<User> retrieveMutualFriends(User otherUser) {
    List<User> mutual = new ArrayList<>();
    for (User friend : this.userProfile.getUser().getFriends()) {
        if (otherUser.getFriends().contains(friend)) {
            mutual.add(friend);
        }
    }
    return mutual;
    }

    public String addGenrePreference(String genre)
    {
        if (genre == null || this.userProfile.getFavoriteGenres().contains(genre)) 
        {
            return null;
        }
        this.userProfile.getFavoriteGenres().add(genre);
        return genre;
    }

    public String removeGenrePreference(String genre)
    {
        this.userProfile.getFavoriteGenres().remove(genre);
        return genre;
    }

    public boolean setProfileName(String newName) {
        User existing = this.dataHandler.searchUser(newName);
        if (existing != null) {
            return false;
        }
        this.userProfile.setProfileName(newName);
        ((SQLHandler) this.dataHandler).updateUserProfile(this.userProfile);
        return true;
    }

    public void setUser(User newUser)
    {
        this.userProfile.setUser(newUser);
    }

    public UserProfile getUserProfile() {
        return this.userProfile;
    }

    public User searchUserProfile(String profileName) {
        return this.dataHandler.searchUser(profileName);
    }

    public String generateActivitySummary() {
       return this.dataHandler.generateActivitySummary();
    }
}