package edu.isu.gamematch;
public class Achievement {
    
    private int achievementId;
    private String achievementName;
    private String achievementDescription;

    public Achievement(int achievementId, String achievementName, String achievementDescription) {
        this.achievementId = achievementId;
        this.achievementName = achievementName;
        this.achievementDescription = achievementDescription;
    }

    public int getAchievementId() {
        return achievementId;
    }
    
    public String getAchievementName() {
        return achievementName;
    }

    public String getAchievementDescription() {
        return achievementDescription;
    }

}