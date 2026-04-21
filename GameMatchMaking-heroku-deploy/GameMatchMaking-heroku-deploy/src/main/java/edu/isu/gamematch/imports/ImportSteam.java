     package edu.isu.gamematch.imports;

import edu.isu.gamematch.steam.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class ImportSteam extends ImportData {
    
    @Autowired
    private SteamAPIService steamAPIService;
    
    private boolean includeRecentlyPlayed;
    private boolean includeOwnedGames;
    private int retryCount;
    private int currentRetry;
    
    public ImportSteam() {
        this.includeRecentlyPlayed = true;
        this.includeOwnedGames = true;
        this.retryCount = 3;
        this.currentRetry = 0;
    }
    
    @Override
    protected void initializeValidationRules() {
        validationRules.add(new ValidationRule("STEAM_ID_FORMAT", "Steam ID must be 17-digit numeric"));
        validationRules.add(new ValidationRule("API_KEY_VALID", "Steam API key must be valid"));
        validationRules.add(new ValidationRule("PROFILE_PUBLIC", "Steam profile must be public"));
    }
    
    @Override
    protected boolean validateSource(String source) {
        logger.debug("Validating Steam source: {}", source);
        
        if (!source.matches("\\d{17}")) {
            logger.error("Invalid Steam ID format: {}", source);
            return false;
        }
        
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
                
                // Fetch player profile
                SteamUser user = steamAPIService.fetchPlayerSummary(source);
                if (user != null) {
                    apiData.put("profile", user);
                    logger.info("Retrieved profile for: {}", user.getPersonaName());
                } else {
                    throw new RuntimeException("Failed to fetch Steam profile");
                }
                
                // Fetch owned games
                if (includeOwnedGames && steamAPIService != null) {
                    List<SteamGame> ownedGames = steamAPIService.fetchOwnedGames(source);
                    apiData.put("ownedGames", ownedGames);
                    logger.info("Retrieved {} owned games", ownedGames.size());
                }
                
                // Fetch recently played
                if (includeRecentlyPlayed && steamAPIService != null) {
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
    @SuppressWarnings("unchecked")
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
            for (Object record : data.getRecords()) {
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
        importStatus.setStatus(ImportStatus.Status.SUCCESS);
    }
    
    // Private helper methods
    
    private void handleRateLimit() {
        try {
            long waitTime = (long) Math.pow(2, currentRetry) * 1000;
            logger.debug("Rate limit backoff: waiting {} ms", waitTime);
            Thread.sleep(waitTime);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private Map<String, Object> extractSteamMetadata(SteamUser user) {
        Map<String, Object> metadata = new HashMap<>();
        if (user != null) {
            metadata.put("steamId", user.getSteamId());
            metadata.put("personaName", user.getPersonaName());
        }
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
    
    // Setters for configuration
    public void setIncludeRecentlyPlayed(boolean include) { this.includeRecentlyPlayed = include; }
    public void setIncludeOwnedGames(boolean include) { this.includeOwnedGames = include; }
}