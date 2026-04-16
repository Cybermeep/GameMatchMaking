// implement in user class

@Entity
@Table(name = "user_games")
/**
 * Association class to hold a user's data for a particular game.
 */
public class UserGame {
    // how to define composite key with hibernate?
    private User user;
    private Game game;

    @Column(name = "total_hours")
    private int totalHours;

    @Column(name = "recent_hours")
    private int recentHours;

    @Column(name = "days_since_played")
    private int daysSincePlayed;

    // double check
    @ManyToMany
    @JoinTable(
        name = "user_game_tags",
        joinColumns = {
            @JoinColumn(name = "user_id", referencedColumnName = "user_id"),
            @JoinColumn(name = "game_id", referencedColumnName = "game_id")
        },
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags;
    
    // default constructor
    public UserGame() {
    }

    // constructor
    public UserGame(User user, Game game, int totalHours, int recentHours, int daysSincePlayed, Set<Tag> tags) {
        this.user = user;
        this.game = game;
        this.totalHours = totalHours;
        this.recentHours = recentHours;
        this.daysSincePlayed = daysSincePlayed;
        this.tags = tags;
    }

    // getters and setters
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
