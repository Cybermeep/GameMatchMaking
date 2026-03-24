/**
 * SteamGame Model Class
 * 
 * Represents a game in a user's Steam library. Contains information about
 * playtime, achievements, and game metadata.
 * 
 * 
 * - Store game metadata from Steam API
 * - Track playtime statistics
 * - Provide game identification for matchmaking
 * 

 */
package edu.isu.gamematch.steam;

import java.io.Serializable;
import java.util.*;

public class SteamGame implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    // Game identification
    private String appId;               // Steam Application ID
    private String name;                // Game name
    private String description;         // Game description (if available)
    
    // Playtime statistics (in minutes)
    private int playtimeForever;        
    private int playtimeLastTwoWeeks;   
    
    // Game assets
    private String imgIconUrl;          // Icon image URL
    private String imgLogoUrl;          // Logo image URL
    private String headerImageUrl;      // Header image URL
    
    // Game metadata
    private boolean hasAchievements;    
    private List<String> genres;        
    private String developer;          
    private String publisher;          
    
    // User-specific data
    private List<String> unlockedAchievements; 
    
    /**
     * Default constructor
     */
    public SteamGame() {
        this.genres = new ArrayList<>();
        this.unlockedAchievements = new ArrayList<>();
    }
    
    /**
     * Constructor for creating a basic Steam game
     * 
     * @param appId Steam Application ID
     * @param name Game name
     * @param playtimeForever Total playtime in minutes
     */
    public SteamGame(String appId, String name, int playtimeForever) {
        this();
        this.appId = appId;
        this.name = name;
        this.playtimeForever = playtimeForever;
    }
    
    //getters&setter methods
    
    public String getAppId() { 
        return appId; 
    }
    
    public void setAppId(String appId) { 
        this.appId = appId; 
    }
    
    public String getName() { 
        return name; 
    }
    
    public void setName(String name) { 
        this.name = name; 
    }
    
    public String getDescription() { 
        return description; 
    }
    
    public void setDescription(String description) { 
        this.description = description; 
    }
    
    public int getPlaytimeForever() { 
        return playtimeForever; 
    }
    
    public void setPlaytimeForever(int playtimeForever) { 
        this.playtimeForever = playtimeForever; 
    }
    
    public int getPlaytimeLastTwoWeeks() { 
        return playtimeLastTwoWeeks; 
    }
    
    public void setPlaytimeLastTwoWeeks(int playtimeLastTwoWeeks) { 
        this.playtimeLastTwoWeeks = playtimeLastTwoWeeks; 
    }
    
    public String getImgIconUrl() { 
        return imgIconUrl; 
    }
    
    public void setImgIconUrl(String imgIconUrl) { 
        this.imgIconUrl = imgIconUrl; 
    }
    
    public String getImgLogoUrl() { 
        return imgLogoUrl; 
    }
    
    public void setImgLogoUrl(String imgLogoUrl) { 
        this.imgLogoUrl = imgLogoUrl; 
    }
    
    public String getHeaderImageUrl() { 
        return headerImageUrl; 
    }
    
    public void setHeaderImageUrl(String headerImageUrl) { 
        this.headerImageUrl = headerImageUrl; 
    }
    
    public boolean isHasAchievements() { 
        return hasAchievements; 
    }
    
    public void setHasAchievements(boolean hasAchievements) { 
        this.hasAchievements = hasAchievements; 
    }
    
    public List<String> getGenres() { 
        return genres; 
    }
    
    public void setGenres(List<String> genres) { 
        this.genres = genres; 
    }
    
    public String getDeveloper() { 
        return developer; 
    }
    
    public void setDeveloper(String developer) { 
        this.developer = developer; 
    }
    
    public String getPublisher() { 
        return publisher; 
    }
    
    public void setPublisher(String publisher) { 
        this.publisher = publisher; 
    }
    
    public List<String> getUnlockedAchievements() { 
        return unlockedAchievements; 
    }
    
    public void setUnlockedAchievements(List<String> unlockedAchievements) { 
        this.unlockedAchievements = unlockedAchievements; 
    }
    
    /**
     * Gets playtime in hours (rounded)
     * 
     * @return Playtime in hours
     */
    public int getPlaytimeHours() {
        return playtimeForever / 60;
    }
    
    /**
     * Checks if user has played this game recently
     * 
     * @return True if played in last 2 weeks
     */
    public boolean isRecentlyPlayed() {
        return playtimeLastTwoWeeks > 0;
    }
    
    /**
     * Adds a genre tag to the game
     * 
     * @param genre Genre to add
     */
    public void addGenre(String genre) {
        if (genre != null && !genres.contains(genre)) {
            genres.add(genre);
        }
    }
    
    /**
     * Adds an unlocked achievement
     * 
     * @param achievement Achievement name to add
     */
    public void addUnlockedAchievement(String achievement) {
        if (achievement != null && !unlockedAchievements.contains(achievement)) {
            unlockedAchievements.add(achievement);
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SteamGame steamGame = (SteamGame) o;
        return Objects.equals(appId, steamGame.appId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(appId);
    }
    
    @Override
    public String toString() {
        return "SteamGame{" +
            "appId='" + appId + '\'' +
            ", name='" + name + '\'' +
            ", playtimeForever=" + playtimeForever +
            '}';
    }
}