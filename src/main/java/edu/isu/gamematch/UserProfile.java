package edu.isu.gamematch;
import java.util.ArrayList;
import java.util.List;

public class UserProfile {
    public String profileName;
    private ArrayList<String> favoriteGenres;
    private User user;

    public UserProfile(String profileName, User user)
    {
        this.profileName = profileName;
        this.user = user;
        this.favoriteGenres = new ArrayList<>();
    }

public List<User> retrieveMutualFriends(User otherUser) {
    List<User> mutual = new ArrayList<>();
    for (User friend : this.user.getFriends()) {
        if (otherUser.getFriends().contains(friend)) {
            mutual.add(friend);
        }
    }
    return mutual;
}

public boolean addGenrePreference(String genre)
{
    if (genre == null || favoriteGenres.contains(genre)) 
    {
        return false;
    }
    favoriteGenres.add(genre);
    return true;
}

public boolean removeGenrePreference(String genre)
{
    return favoriteGenres.remove(genre);
}

public void setProfileName( String newName)
{
    this.profileName = newName;
}

public String getProfileName()
{
    return this.profileName;
}

    public User getUser()
    {
        return this.user;
    }
}