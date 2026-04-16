package edu.isu.gamematch.imports;

import edu.isu.gamematch.steam.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.*;

/**
 * importing data from Steam API
 * Uses existing SteamAPIService for data retrieval
 * 
 */
@Component
public class ImportSteam extends ImportData {
    
    private final SteamAPIService steamAPIService;
    private boolean includeRecentlyPlayed;
    private boolean includeOwnedGames;
    private int timeoutSeconds;
    private int retryCount;
    private int currentRetry;
    
    @Autowired
    public ImportSteam(SteamAPIService steamAPIService) {
        this.steamAPIService = steamAPIService;
        this.includeRecentlyPlayed = true;
        this.includeOwnedGames = true;
        this.timeoutSeconds = 30;
        this.retryCount = 3;
        this.currentRetry = 0;
    }
    
    public void setIncludeRecentlyPlayed(boolean include) {
        this.includeRecentlyPlayed = include;
    }
    
    public void setIncludeOwnedGames(boolean include) {
        this.includeOwnedGames = include;
    }
    
    @Override
    protected void initializeValidationRules() {
        validationRules.add(new ValidationRule("STEAM_ID_FORMAT", "Steam ID must be 17-digit numeric"));
        validationRules.add(new ValidationRule("API_KEY_VALID", "Steam API key must be valid"));
        validationRules.add(new ValidationRule("PROFILE_PUBLIC", "Steam profile must be public"));
        validationRules.add(new ValidationRule("API_RESPONSE", "Steam API must respond within timeout"));
    }
    
    @Override
    protected boolean validateSource(String source) {
        logger.debug("Validating Steam source: {}", source);
        
        // Check Steam ID format (17 digits)
        if (!source.matches("\\d{17}")) {
            logger.error("Invalid Steam ID format: {}", source);
            return false;
        }
        
        // Check API key validity using existing SteamAPIService
        if (!steamAPIService.validateApiKey()) {
            logger.error("Invalid Steam API key");
            return false;
        }
        
        // Verify profile is accessible (try to fetch summary)
        SteamUser testUser = steamAPIService.fetchPlayerSummary(source);
        if (testUser == null) {
            logger.error("Cannot access Steam profile: {} (may be private)", source);
            return false;
        }
        
        logger.info("Steam source validation passed: {}", testUser.getPersonaName());
        return true;
    }
    
    @Override
    protected RawData extractData(String source) {
        logger.debug("Extracting data from Steam API for: {}", source);
        currentRetry = 0;
        
        while (currentRetry < retryCount) {
            try {
                RawData rawData = new RawData();
                rawData.setSource(source);
                rawData.setFormat("STEAM_API");
                
                Map<String, Object> apiData = new HashMap<>();
                
                // Fetch player profile using existing SteamAPIService
                SteamUser user = fetchSteamProfile(source);
                if (user != null) {
                    apiData.put("profile", user);
                    logger.info("Retrieved profile for: {}", user.getPersonaName());
                } else {
                    throw new RuntimeException("Failed to fetch Steam profile");
                }
                
                // Fetch games if configured
                if (includeOwnedGames) {
                    List<SteamGame> ownedGames = fetchSteamGames(source);
                    apiData.put("ownedGames", ownedGames);
                    logger.info("Retrieved {} owned games", ownedGames.size());
                }
                
                // Fetch recently played if configured
                if (includeRecentlyPlayed) {
                    List<SteamGame> recentlyPlayed = steamAPIService.fetchRecentlyPlayedGames(source);
                    apiData.put("recentlyPlayed", recentlyPlayed);
                    logger.info("Retrieved {} recently played games", recentlyPlayed.size());
                }
                
                rawData.setContent(apiData);
                rawData.setMetadata(extractSteamMetadata(user));
                
                return rawData;
                
            } catch (Exception e) {
                currentRetry++;
                logger.warn("Steam API extraction failed (attempt {}/{}): {}", 
                    currentRetry, retryCount, e.getMessage());
                
                if (currentRetry >= retryCount) {
                    logger.error("Steam API extraction failed after {} attempts", retryCount);
                    return null;
                }
                
                handleRateLimit();
            }
        }
        
        return null;
    }
    
    @Override
    protected TransformedData transformData(RawData rawData) {
        logger.debug("Transforming Steam API data");
        
        TransformedData transformedData = new TransformedData();
        transformedData.setSourceType("STEAM_API");
        transformedData.setOriginalFormat("JSON");
        
        try {
            Map<String, Object> apiContent = (Map<String, Object>) rawData.getContent();
            List<Object> records = new ArrayList<>();
            
            // Transform profile data
            SteamUser user = (SteamUser) apiContent.get("profile");
            if (user != null) {
                Map<String, Object> profileRecord = new HashMap<>();
                profileRecord.put("type", "STEAM_PROFILE");
                profileRecord.put("steamId", user.getSteamId());
                profileRecord.put("personaName", user.getPersonaName());
                profileRecord.put("avatarUrl", user.getAvatarUrl());
                profileRecord.put("profileUrl", user.getProfileUrl());
                records.add(profileRecord);
            }
            
            // Transform owned games
            if (apiContent.containsKey("ownedGames")) {
                List<SteamGame> games = (List<SteamGame>) apiContent.get("ownedGames");
                for (SteamGame game : games) {
                    Map<String, Object> gameRecord = new HashMap<>();
                    gameRecord.put("type", "STEAM_GAME");
                    gameRecord.put("appId", game.getAppId());
                    gameRecord.put("name", game.getName());
                    gameRecord.put("playtimeForever", game.getPlaytimeForever());
                    gameRecord.put("playtimeHours", game.getPlaytimeHours());
                    records.add(gameRecord);
                }
            }
            
            // Transform recently played
            if (apiContent.containsKey("recentlyPlayed")) {
                List<SteamGame> recent = (List<SteamGame>) apiContent.get("recentlyPlayed");
                for (SteamGame game : recent) {
                    Map<String, Object> recentRecord = new HashMap<>();
                    recentRecord.put("type", "STEAM_RECENT");
                    recentRecord.put("appId", game.getAppId());
                    recentRecord.put("name", game.getName());
                    recentRecord.put("playtimeLastTwoWeeks", game.getPlaytimeLastTwoWeeks());
                    records.add(recentRecord);
                }
            }
            
            transformedData.setRecords(records);
            transformedData.setRecordCount(records.size());
            transformedData.setValidationResults(validateTransformedData(transformedData));
            
            logger.info("Transformed {} records from Steam API", transformedData.getRecordCount());
            return transformedData;
            
        } catch (Exception e) {
            logger.error("Steam data transformation failed", e);
            return null;
        }
    }
    
    @Override
    protected boolean loadData(TransformedData data) {
        logger.debug("Loading Steam data into system");
        
        try {
            //TODO: this needs to actually interact with the database
            
            for (Object record : data.getRecords()) {
                Map<String, Object> recordMap = (Map<String, Object>) record;
                String recordType = (String) recordMap.get("type");
                
                switch (recordType) {
                    case "STEAM_PROFILE":
                        // Save or update user profile
                        logger.debug("Processing Steam profile");
                        break;
                    case "STEAM_GAME":
                        // Save game to user's library
                        logger.debug("Processing Steam game");
                        break;
                    case "STEAM_RECENT":
                        // Update recently played
                        logger.debug("Processing recently played game");
                        break;
                }
                
                importStatus.incrementProcessedRecords();
            }
            
            logger.info("Successfully loaded {} Steam records", data.getRecordCount());
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to load Steam data", e);
            return false;
        }
    }
    
    @Override
    protected void postImportProcessing(TransformedData data) {
        logger.debug("Post-import processing for Steam import");
        
        // Update matchmaking eligibility
        // Generate recommendations based on new games
        // Update user status
        
        importStatus.setPostProcessed(true);
    }
    
    // Private helper methods
    
    private SteamUser fetchSteamProfile(String steamId) {
        return steamAPIService.fetchPlayerSummary(steamId);
    }
    
    private List<SteamGame> fetchSteamGames(String steamId) {
        return steamAPIService.fetchOwnedGames(steamId);
    }
    
    private void handleRateLimit() {
        try {
            // Exponential backoff
            long waitTime = (long) Math.pow(2, currentRetry) * 1000;
            logger.debug("Rate limit backoff: waiting {} ms", waitTime);
            Thread.sleep(waitTime);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private Map<String, Object> extractSteamMetadata(SteamUser user) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("steamId", user.getSteamId());
        metadata.put("personaName", user.getPersonaName());
        metadata.put("importTimestamp", new Date());
        metadata.put("includeOwnedGames", includeOwnedGames);
        metadata.put("includeRecentlyPlayed", includeRecentlyPlayed);
        return metadata;
    }
    
    private List<ValidationResult> validateTransformedData(TransformedData data) {
        List<ValidationResult> results = new ArrayList<>();
        
        if (data.getRecords() == null || data.getRecords().isEmpty()) {
            results.add(new ValidationResult(false, "No records to import"));
        } else {
            results.add(new ValidationResult(true, "Validated " + data.getRecordCount() + " records"));
        }
        
        return results;
    }
}s