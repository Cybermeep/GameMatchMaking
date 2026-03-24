/**
 * SteamDataParser Class
 * 
 * Responsible for parsing JSON responses from the Steam Web API into
 * Java objects. Implements parsing logic for player summaries and
 * game library data.
 * 
 * 
 * - Parse player summary JSON to SteamUser object
 * - Parse owned games JSON to list of SteamGame objects
 * - Handle API response errors gracefully
 * 
 */
package edu.isu.gamematch.steam;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class SteamDataParser {
    
    private static final Logger logger = LoggerFactory.getLogger(SteamDataParser.class);
    private final ObjectMapper objectMapper;
    
    public SteamDataParser() {
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Parses player summary JSON into a SteamUser object
     * 
     * @param jsonResponse JSON response from Steam API
     * @return SteamUser object with populated data, or null if parsing fails
     */
    public SteamUser parsePlayerSummary(String jsonResponse) {
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode players = root.path("response").path("players");
            
            if (players.isArray() && players.size() > 0) {
                JsonNode player = players.get(0);
                
                String steamId = player.path("steamid").asText();
                String personaName = player.path("personaname").asText();
                String avatarUrl = player.path("avatar").asText();
                
                SteamUser user = new SteamUser(steamId, personaName, avatarUrl);
                
                // Additional profile data
                user.setProfileUrl(player.path("profileurl").asText());
                
                logger.info("Successfully parsed player summary for: {}", personaName);
                return user;
            }
        } catch (Exception e) {
            logger.error("Failed to parse player summary JSON", e);
        }
        return null;
    }
    
    /**
     * Parses owned games JSON into a list of SteamGame objects
     * 
     * @param jsonResponse JSON response from Steam API
     * @return List of SteamGame objects, or empty list if parsing fails
     */
    public List<SteamGame> parseOwnedGames(String jsonResponse) {
        List<SteamGame> games = new ArrayList<>();
        
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode gamesNode = root.path("response").path("games");
            
            if (gamesNode.isArray()) {
                for (JsonNode gameNode : gamesNode) {
                    SteamGame game = new SteamGame();
                    game.setAppId(gameNode.path("appid").asText());
                    game.setName(gameNode.path("name").asText());
                    game.setPlaytimeForever(gameNode.path("playtime_forever").asInt());
                    game.setPlaytimeLastTwoWeeks(gameNode.path("playtime_2weeks").asInt());
                    game.setImgIconUrl(gameNode.path("img_icon_url").asText());
                    game.setImgLogoUrl(gameNode.path("img_logo_url").asText());
                    game.setHasAchievements(gameNode.path("has_community_visible_stats").asBoolean());
                    
                    games.add(game);
                }
            }
            
            logger.info("Successfully parsed {} owned games", games.size());
        } catch (Exception e) {
            logger.error("Failed to parse owned games JSON", e);
        }
        
        return games;
    }
    
    /**
     * Parses recently played games JSON into a list of SteamGame objects
     * 
     * @param jsonResponse JSON response from Steam API
     * @return List of recently played SteamGame objects
     */
    public List<SteamGame> parseRecentlyPlayedGames(String jsonResponse) {
        List<SteamGame> games = new ArrayList<>();
        
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode gamesNode = root.path("response").path("games");
            
            if (gamesNode.isArray()) {
                for (JsonNode gameNode : gamesNode) {
                    SteamGame game = new SteamGame();
                    game.setAppId(gameNode.path("appid").asText());
                    game.setName(gameNode.path("name").asText());
                    game.setPlaytimeLastTwoWeeks(gameNode.path("playtime_2weeks").asInt());
                    game.setImgIconUrl(gameNode.path("img_icon_url").asText());
                    game.setImgLogoUrl(gameNode.path("img_logo_url").asText());
                    
                    games.add(game);
                }
            }
            
            logger.info("Successfully parsed {} recently played games", games.size());
        } catch (Exception e) {
            logger.error("Failed to parse recently played games JSON", e);
        }
        
        return games;
    }
    
    /**
     * Extracts Steam ID from OpenID response
     * 
     * @param openIdResponse The OpenID response string
     * @return The extracted Steam ID, or null if extraction fails
     */
    public String extractSteamIdFromOpenId(String openIdResponse) {
        try {
            // Steam OpenID returns a URL: https://steamcommunity.com/openid/id/76561197960435530
            String[] parts = openIdResponse.split("/");
            String steamId = parts[parts.length - 1];
            
            logger.debug("Extracted Steam ID: {}", steamId);
            return steamId;
        } catch (Exception e) {
            logger.error("Failed to extract Steam ID from OpenID response", e);
            return null;
        }
    }
    
    /**
     * Validates and parses the Steam API key response
     * 
     * @param jsonResponse JSON response from Steam API validation
     * @return True if API key is valid, false otherwise
     */
    public boolean validateApiKeyResponse(String jsonResponse) {
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            // Check if response contains expected structure
            return root.has("response") || root.has("players");
        } catch (Exception e) {
            logger.error("Failed to validate API key response", e);
            return false;
        }
    }
}