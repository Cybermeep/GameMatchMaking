package edu.isu.gamematch.steam;

public class SteamAchievement {
    private String apiName;      // internal API name
    private String name;         // display name
    private String description;
    private boolean achieved;

    public String getApiName() { return apiName; }
    public void setApiName(String apiName) { this.apiName = apiName; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isAchieved() { return achieved; }
    public void setAchieved(boolean achieved) { this.achieved = achieved; }

}