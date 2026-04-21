/**
 * SteamAPIService Class
 * 
 * Handles all HTTP communications with the Steam Web API. This service
 * is responsible for fetching player summaries, owned games, and
 * recently played games from Steam.
 * 
 * 
 * - Make HTTP requests to Steam API endpoints
 * - Handle API errors and timeouts
 * - Coordinate with SteamDataParser for JSON parsing
 * - Manage API rate limiting

 */
package edu.isu.gamematch.steam;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class SteamAPIService {
    
    private static final Logger logger = LoggerFactory.getLogger(SteamAPIService.class);
    
    @Value("${steam.api.key}")
    private String apiKey;
    
    @Value("${steam.api.url}")
    private String apiUrl;
    
    private final RestTemplate restTemplate;
    private final SteamDataParser dataParser;
    
    public SteamAPIService(RestTemplate restTemplate, SteamDataParser dataParser) {
        this.restTemplate = restTemplate;
        this.dataParser = dataParser;
    }
    
    
    public SteamUser fetchPlayerSummary(String steamId) {
        try {
            String endpoint = apiUrl + "/ISteamUser/GetPlayerSummaries/v0002/";
            String url = endpoint + "?key=" + apiKey + "&steamids=" + steamId;
            
            logger.debug("Fetching player summary for Steam ID: {}", steamId);
            
            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                String.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                SteamUser user = dataParser.parsePlayerSummary(response.getBody());
                if (user != null) {
                    logger.info("Successfully fetched player summary for: {}", user.getPersonaName());
                    return user;
                }
            }
        } catch (RestClientException e) {
            logger.error("Failed to fetch player summary for Steam ID: {}", steamId, e);
        }
        return null;
    }
    
    public List<SteamGame> fetchOwnedGames(String steamId) {
        try {
            String endpoint = apiUrl + "/IPlayerService/GetOwnedGames/v0001/";
            String url = endpoint + "?key=" + apiKey + "&steamid=" + steamId + "&include_appinfo=true&include_played_free_games=true";
            
            logger.debug("Fetching owned games for Steam ID: {}", steamId);
            
            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                String.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<SteamGame> games = dataParser.parseOwnedGames(response.getBody());
                logger.info("Successfully fetched {} owned games for user", games.size());
                return games;
            }
        } catch (RestClientException e) {
            logger.error("Failed to fetch owned games for Steam ID: {}", steamId, e);
        }
        return new ArrayList<>();
    }
    
    public List<SteamGame> fetchRecentlyPlayedGames(String steamId) {
        try {
            String endpoint = apiUrl + "/IPlayerService/GetRecentlyPlayedGames/v0001/";
            String url = endpoint + "?key=" + apiKey + "&steamid=" + steamId + "&count=10";
            
            logger.debug("Fetching recently played games for Steam ID: {}", steamId);
            
            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                String.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<SteamGame> games = dataParser.parseRecentlyPlayedGames(response.getBody());
                logger.info("Successfully fetched {} recently played games", games.size());
                return games;
            }
        } catch (RestClientException e) {
            logger.error("Failed to fetch recently played games for Steam ID: {}", steamId, e);
        }
        return new ArrayList<>();
    }
    
    public SteamUser fetchCompleteUserData(String steamId) {
        SteamUser user = fetchPlayerSummary(steamId);
        
        if (user != null) {
            List<SteamGame> ownedGames = fetchOwnedGames(steamId);
            List<SteamGame> recentlyPlayed = fetchRecentlyPlayedGames(steamId);
            
            for (SteamGame game : ownedGames) {
                user.addGame(game);
            }
            
            user.getRecentlyPlayed().addAll(recentlyPlayed);
            
            logger.info("Complete data fetched for user: {} ({} games)", 
                user.getPersonaName(), user.getTotalGameCount());
        }
        
        return user;
    }
    
    public boolean validateApiKey() {
        try {
            String testSteamId = "76561197960287930";
            String endpoint = apiUrl + "/ISteamUser/GetPlayerSummaries/v0002/";
            String url = endpoint + "?key=" + apiKey + "&steamids=" + testSteamId;
            
            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                String.class
            );
            
            boolean isValid = response.getStatusCode() == HttpStatus.OK;
            if (isValid) {
                logger.info("Steam API key validation successful");
            } else {
                logger.warn("Steam API key validation failed with status: {}", response.getStatusCode());
            }
            return isValid;
            
        } catch (RestClientException e) {
            logger.error("Steam API key validation failed", e);
            return false;
        }
    }
}