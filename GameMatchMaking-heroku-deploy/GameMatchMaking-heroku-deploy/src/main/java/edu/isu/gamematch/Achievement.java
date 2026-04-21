

package edu.isu.gamematch;
import javax.persistence.*;
import java.util.List;
import java.util.ArrayList;

@Entity
@Table(name = "achievements")
public class Achievement {

   @Id
@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "achievement_seq")
@SequenceGenerator(name = "achievement_seq", sequenceName = "ACHIEVEMENT_SEQ", allocationSize = 1)
@Column(name = "achievement_id")
private int achievementId;

    @Column(name = "achievement_name", nullable = false)
    private String achievementName;

    @Column(name = "achievement_description")
    private String achievementDescription;

    @OneToMany(mappedBy = "achievement", cascade = CascadeType.ALL)
    private List<GameAchievement> gameAchievements = new ArrayList<>();

    public Achievement() {
        // Default constructor required by JPA
    }

    public Achievement(String achievementName, String achievementDescription) {
        this.achievementName = achievementName;
        this.achievementDescription = achievementDescription;
    }

    public int getAchievementId() {
        return achievementId;
    }

    public void setAchievementId(int achievementId) {
        this.achievementId = achievementId;
    }

    public String getAchievementName() {
        return achievementName;
    }

    public void setAchievementName(String achievementName) {
        this.achievementName = achievementName;
    }

    public String getAchievementDescription() {
        return achievementDescription;
    }

    public void setAchievementDescription(String achievementDescription) {
        this.achievementDescription = achievementDescription;
    }

    public List<GameAchievement> getGameAchievements() {
        return gameAchievements;
    }

    public void setGameAchievements(List<GameAchievement> gameAchievements) {
        this.gameAchievements = gameAchievements;
    }

    public void addGameAchievement(GameAchievement gameAchievement) {
        gameAchievements.add(gameAchievement);
        gameAchievement.setAchievement(this);
    }

    public void removeGameAchievement(GameAchievement gameAchievement) {
        gameAchievements.remove(gameAchievement);
        gameAchievement.setAchievement(null);
    }

}