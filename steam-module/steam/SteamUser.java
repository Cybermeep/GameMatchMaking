/**
 * SteamUser Model Class
 * 
 * Represents a Steam user authenticated through the Steam OpenID/OAuth protocol.
 * This class captures all the essential user information returned from Steam's
 * authentication and API services.
 * 
 * 
 * - Store authenticated user's Steam profile data
 * - Maintain session information for the user
 * - Provide access to user's game library data
 * 

 */
package edu.isu.gamematch.steam;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;

public class SteamUser implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    // Core user identification
    private String steamId;           // Unique 64-bit Steam ID
    private String personaName;       // User's display name on Steam
    private String avatarUrl;         // URL to user's avatar image
    private String profileUrl;        // URL to user's Steam profile
    
    // Session management
    private String sessionToken;       // Token for session validation
    private LocalDateTime loginTime;   // When user logged in
    private LocalDateTime lastActive;  // Last activity timestamp
    private boolean isAuthenticated;   // Authentication status
    
    // User's game library
    private List<SteamGame> ownedGames;        // All games owned by user
    private List<SteamGame> recentlyPlayed;    // Recently played games
    private Map<String, Integer> playtimeByGame; // Playtime per game
    
    /**
     * Default constructor
     */
    public SteamUser() {
        this.ownedGames = new ArrayList<>();
        this.recentlyPlayed = new ArrayList<>();
        this.playtimeByGame = new HashMap<>();
        this.loginTime = LocalDateTime.now();
        this.lastActive = LocalDateTime.now();
        this.isAuthenticated = false;
    }
    
    /**
     * Constructor for creating a new Steam user from authentication
     * 
     * @param steamId User's unique Steam ID
     * @param personaName User's Steam display name
     * @param avatarUrl URL to user's Steam avatar
     */
    public SteamUser(String steamId, String personaName, String avatarUrl) {
        this();
        this.steamId = steamId;
        this.personaName = personaName;
        this.avatarUrl = avatarUrl;
        this.profileUrl = "https://steamcommunity.com/profiles/" + steamId;
    }
    
    //getter&setters
    
    public String getSteamId() { 
        return steamId; 
    }
    
    public void setSteamId(String steamId) { 
        this.steamId = steamId; 
    }
    
    public String getPersonaName() { 
        return personaName; 
    }
    
    public void setPersonaName(String personaName) { 
        this.personaName = personaName; 
    }
    
    public String getAvatarUrl() { 
        return avatarUrl; 
    }
    
    public void setAvatarUrl(String avatarUrl) { 
        this.avatarUrl = avatarUrl; 
    }
    
    public String getProfileUrl() { 
        return profileUrl; 
    }
    
    public void setProfileUrl(String profileUrl) { 
        this.profileUrl = profileUrl; 
    }
    
    public String getSessionToken() { 
        return sessionToken; 
    }
    
    public void setSessionToken(String sessionToken) { 
        this.sessionToken = sessionToken; 
    }
    
    public LocalDateTime getLoginTime() { 
        return loginTime; 
    }
    
    public LocalDateTime getLastActive() { 
        return lastActive; 
    }
    
    public void updateLastActive() { 
        this.lastActive = LocalDateTime.now(); 
    }
    
    public boolean isAuthenticated() { 
        return isAuthenticated; 
    }
    
    public void setAuthenticated(boolean authenticated) { 
        isAuthenticated = authenticated; 
    }
    
    public List<SteamGame> getOwnedGames() { 
        return ownedGames; 
    }
    
    public List<SteamGame> getRecentlyPlayed() { 
        return recentlyPlayed; 
    }
    
    /**
     * Adds a game to user's owned games library
     * 
     * @param game The SteamGame to add
     */
    public void addGame(SteamGame game) {
        if (game != null && !ownedGames.contains(game)) {
            ownedGames.add(game);
            playtimeByGame.put(game.getAppId(), game.getPlaytimeForever());
        }
    }
    
    /**
     * Gets total number of games owned by user
     * 
     * @return Number of games in library
     */
    public int getTotalGameCount() {
        return ownedGames.size();
    }
    
    /**
     * Gets total playtime across all games
     * 
     * @return Total playtime in minutes
     */
    public int getTotalPlaytime() {
        return ownedGames.stream()
            .mapToInt(SteamGame::getPlaytimeForever)
            .sum();
    }
    
    /**
     * Filters games by minimum playtime requirement
     * 
     * @param minPlaytime Minimum playtime in minutes
     * @return List of games meeting the playtime requirement
     */
    public List<SteamGame> getGamesByMinPlaytime(int minPlaytime) {
        return ownedGames.stream()
            .filter(game -> game.getPlaytimeForever() >= minPlaytime)
            .collect(java.util.stream.Collectors.toList());
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SteamUser steamUser = (SteamUser) o;
        return Objects.equals(steamId, steamUser.steamId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(steamId);
    }
    
    @Override
    public String toString() {
        return "SteamUser{" +
            "steamId='" + steamId + '\'' +
            ", personaName='" + personaName + '\'' +
            ", gameCount=" + ownedGames.size() +
            '}';
    }
}