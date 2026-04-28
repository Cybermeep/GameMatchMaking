package edu.isu.gamematch.steam;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.*;

@Service
public class SteamAPIService {

    private static final Logger logger = LoggerFactory.getLogger(SteamAPIService.class);

    @Value("${steam.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper mapper;

    public SteamAPIService() {
        this.restTemplate = new RestTemplate();
        this.mapper = new ObjectMapper();
    }

    // --- Player Summary ---
    public SteamUser fetchPlayerSummary(String steamId) {
        String url = "http://api.steampowered.com/ISteamUser/GetPlayerSummaries/v0002/?key="
                     + apiKey + "&steamids=" + steamId;
        try {
            String json = restTemplate.getForObject(url, String.class);
            JsonNode root = mapper.readTree(json);
            JsonNode players = root.path("response").path("players");
            if (players.isArray() && players.size() > 0) {
                JsonNode player = players.get(0);
                SteamUser user = new SteamUser();
                user.setSteamId(player.path("steamid").asText());
                user.setPersonaName(player.path("personaname").asText());
                user.setAvatarUrl(player.path("avatar").asText());
                user.setProfileUrl(player.path("profileurl").asText());
                return user;
            }
        } catch (Exception e) {
            logger.error("Failed to fetch player summary for {}", steamId, e);
        }
        return null;
    }

    // --- Owned Games ---
    public List<SteamGame> fetchOwnedGames(String steamId) {
        String url = "http://api.steampowered.com/IPlayerService/GetOwnedGames/v0001/?key="
                     + apiKey + "&steamid=" + steamId + "&include_appinfo=true&include_played_free_games=true";
        List<SteamGame> games = new ArrayList<>();
        try {
            String json = restTemplate.getForObject(url, String.class);
            JsonNode root = mapper.readTree(json);
            JsonNode gameNodes = root.path("response").path("games");
            for (JsonNode node : gameNodes) {
                SteamGame sg = new SteamGame();
                sg.setAppId(String.valueOf(node.path("appid").asInt()));

                sg.setName(node.path("name").asText());
                sg.setPlaytimeForever(node.path("playtime_forever").asInt());
                // optional: playtimeLastTwoWeeks = node.path("playtime_2weeks").asInt()
                games.add(sg);
            }
        } catch (Exception e) {
            logger.error("Failed to fetch owned games for {}", steamId, e);
        }
        return games;
    }

    /**
 * Fetches a SteamUser with profile, owned games, and recently played games all populated.
 * Used by the login callback to get everything in one call.
 */
public SteamUser fetchCompleteUserData(String steamId) {
    // Fetch basic profile
    SteamUser user = fetchPlayerSummary(steamId);
    if (user == null) return null;

    // Fetch and attach owned games
    List<SteamGame> ownedGames = fetchOwnedGames(steamId);
    if (ownedGames != null) {
        for (SteamGame game : ownedGames) {
            user.addGame(game);
        }
    }

    // Fetch and attach recently played games
    List<SteamGame> recentGames = fetchRecentlyPlayedGames(steamId);
    if (recentGames != null) {
        user.getRecentlyPlayed().addAll(recentGames);
    }

    return user;
}

    // --- Recently Played Games (for dashboard) ---
    public List<SteamGame> fetchRecentlyPlayedGames(String steamId) {
        String url = "http://api.steampowered.com/IPlayerService/GetRecentlyPlayedGames/v0001/?key="
                     + apiKey + "&steamid=" + steamId;
        List<SteamGame> games = new ArrayList<>();
        try {
            String json = restTemplate.getForObject(url, String.class);
            JsonNode root = mapper.readTree(json);
            JsonNode gameNodes = root.path("response").path("games");
            for (JsonNode node : gameNodes) {
                SteamGame sg = new SteamGame();
                sg.setAppId(String.valueOf(node.path("appid").asInt()));

                sg.setName(node.path("name").asText());
                sg.setPlaytimeLastTwoWeeks(node.path("playtime_2weeks").asInt());
                games.add(sg);
            }
        } catch (Exception e) {
            logger.error("Failed to fetch recently played for {}", steamId, e);
        }
        return games;
    }

    // --- Friend List (NEW  Fix 4.3) ---
    public List<SteamUser> fetchFriendList(String steamId) {
        // Returns friends' basic SteamIds; we then fetch each summary
        List<SteamUser> friends = new ArrayList<>();
        String url = "http://api.steampowered.com/ISteamUser/GetFriendList/v0001/?key="
                     + apiKey + "&steamid=" + steamId + "&relationship=friend";
        try {
            String json = restTemplate.getForObject(url, String.class);
            JsonNode root = mapper.readTree(json);
            JsonNode friendNodes = root.path("friendslist").path("friends");
            if (friendNodes.isArray()) {
                List<String> friendIds = new ArrayList<>();
                for (JsonNode node : friendNodes) {
                    friendIds.add(node.path("steamid").asText());
                }
                // Batch fetch friend summaries (up to 100 at a time)
                for (int i = 0; i < friendIds.size(); i += 100) {
                    int end = Math.min(i + 100, friendIds.size());
                    String batchIds = String.join(",", friendIds.subList(i, end));
                    String summaryUrl = "http://api.steampowered.com/ISteamUser/GetPlayerSummaries/v0002/?key="
                                       + apiKey + "&steamids=" + batchIds;
                    String batchJson = restTemplate.getForObject(summaryUrl, String.class);
                    JsonNode players = mapper.readTree(batchJson).path("response").path("players");
                    for (JsonNode player : players) {
                        SteamUser friend = new SteamUser();
                        friend.setSteamId(player.path("steamid").asText());
                        friend.setPersonaName(player.path("personaname").asText());
                        friend.setAvatarUrl(player.path("avatar").asText());
                        friends.add(friend);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Failed to fetch friend list for {}", steamId, e);
        }
        return friends;
    }
}