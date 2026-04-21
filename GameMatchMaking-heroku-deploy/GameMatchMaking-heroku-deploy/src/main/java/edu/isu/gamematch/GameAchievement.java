package edu.isu.gamematch;
import javax.persistence.*;

@Entity
@Table(name = "game_achievements")
public class GameAchievement {
  @Id
@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "game_ach_seq")
@SequenceGenerator(name = "game_ach_seq", sequenceName = "GAME_ACH_SEQ", allocationSize = 1)
@Column(name = "id")
private int id;

    @ManyToOne
    @JoinColumn(name = "game_id")
    private Game game;

    @ManyToOne
    @JoinColumn(name = "achievement_id")
    private Achievement achievement;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    public GameAchievement() {}

    public GameAchievement(Game game, Achievement achievement, User user) {
        this.game = game;
        this.achievement = achievement;
        this.user = user;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Game getGame() {
        return game;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public Achievement getAchievement() {
        return achievement;
    }

    public void setAchievement(Achievement achievement) {
        this.achievement = achievement;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}