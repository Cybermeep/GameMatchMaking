package edu.isu.gamematch;

import javax.persistence.*;
import java.util.Set;

@Entity
@Table(name = "user_games")
public class UserGame {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_game_seq")
    @SequenceGenerator(name = "user_game_seq", sequenceName = "USER_GAME_SEQ", allocationSize = 1)
    @Column(name = "user_game_id")
    private int id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @Column(name = "total_hours")
    private int totalHours;

    @Column(name = "recent_hours")
    private int recentHours;

    @Column(name = "days_since_played")
    private int daysSincePlayed;

    public UserGame() {}

    public UserGame(User user, Game game, int totalHours, int recentHours, int daysSincePlayed) {
        this.user = user;
        this.game = game;
        this.totalHours = totalHours;
        this.recentHours = recentHours;
        this.daysSincePlayed = daysSincePlayed;
    }

    public int getId() { return id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Game getGame() { return game; }
    public void setGame(Game game) { this.game = game; }

    public int getTotalHours() { return totalHours; }
    public void setTotalHours(int totalHours) { this.totalHours = totalHours; }

    public int getRecentHours() { return recentHours; }
    public void setRecentHours(int recentHours) { this.recentHours = recentHours; }

    public int getDaysSincePlayed() { return daysSincePlayed; }
    public void setDaysSincePlayed(int daysSincePlayed) { this.daysSincePlayed = daysSincePlayed; }
}