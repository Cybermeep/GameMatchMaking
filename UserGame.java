/**
 * [MAYBE?] Association class to hold a user's data for a particular game.
 */
public class UserGame {
    private User user;
    private Game game;
    private int totalHours;
    private int recentHours;
    private int daysSincePlayed;
    private Set<Tag> tags;
    
    // getters and setters, excluding setters for user and game since those should be immutable after creation
    public User getUser() {
        return user;
    }
    public Game getGame() {
        return game;
    }
    public int getTotalHours() {
        return totalHours;
    }
    public void setTotalHours(int totalHours) {
        this.totalHours = totalHours;
    }
    public int getRecentHours() {
        return recentHours;
    }
    public void setRecentHours(int recentHours) {
        this.recentHours = recentHours;
    }
    public int getDaysSincePlayed() {
        return daysSincePlayed;
    }
    public void setDaysSincePlayed(int daysSincePlayed) {
        this.daysSincePlayed = daysSincePlayed;
    }
    public Set<Tag> getTags() {
        return tags;
    }
    public void setTags(Set<Tag> tags) {
        this.tags = tags;
    }

    
}
