package com.gamematchmaker.steam.api;

import com.gamematchmaker.steam.exception.SteamApiException;
import com.gamematchmaker.steam.model.SteamModels.*;
import com.gamematchmaker.steam.util.JsonParser;
import com.gamematchmaker.steam.util.SteamConstants;
import com.gamematchmaker.steam.util.SteamHttpClient;

import java.util.*;
import java.util.logging.Logger;

/*
 * SteamApiClient.java
 *
 *  One responsibility: translate Steam API HTTP calls into typed Java
 *   objects. It does NOT manage sessions, handle authentication flow, or
 *   persist data — those are other classes' jobs. One reason to change:
 *   if Steam changes their API response structure.
 *

 */
public class SteamApiClient {

    private static final Logger LOGGER = Logger.getLogger(SteamApiClient.class.getName());

    // The Steam Web API key — required for every request.
    // Get yours at: https://steamcommunity.com/dev/apikey
    // Read from environment variable — never hard-code (SRS §3.2.3).
    private final String apiKey;

    /**
     * Constructor.
     * Throws immediately if the key is null/blank — we catch misconfiguration
     * at startup rather than getting cryptic 401 errors at request time.
     */
    public SteamApiClient(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("Steam API key is required.");
        }
        this.apiKey = apiKey;
    }

    // FR 3.1.1 / FR 3.1.15 — Player profiles

    /**
     * Gets public profile data for a list of Steam users.
     *
     * Steam allows up to MAX_IDS_PER_BATCH (100) IDs per request.
     * This method automatically batches larger lists into multiple calls
     * and combines the results — callers never need to think about batching.
     *
     * Returns an empty list if steamIds is null or empty (no crash, no exception).
     */
    public List<SteamProfile> fetchProfiles(List<String> steamIds) throws SteamApiException {
        if (steamIds == null || steamIds.isEmpty()) return Collections.emptyList();

        List<SteamProfile> allProfiles = new ArrayList<>();

        // Batch in groups of 100 to stay within Steam's per-request limit
        for (int i = 0; i < steamIds.size(); i += SteamConstants.MAX_IDS_PER_BATCH) {
            List<String> batch = steamIds.subList(
                    i, Math.min(i + SteamConstants.MAX_IDS_PER_BATCH, steamIds.size()));

            Map<String, String> params = new LinkedHashMap<>();
            params.put("key",      apiKey);
            params.put("steamids", String.join(",", batch));
            params.put("format",   "json");

            String url  = SteamHttpClient.appendParams(SteamConstants.ENDPOINT_PLAYER_SUMMARIES, params);
            String json = SteamHttpClient.get(url);

            // Factory method: parseProfiles decides how to construct SteamProfile objects
            allProfiles.addAll(parseProfiles(json));
        }

        LOGGER.info("Fetched " + allProfiles.size() + " profiles.");
        return allProfiles;
    }

    /**
     * Convenience overload for a single user.
     * Wraps the list version and returns the first result, or null.
     */
    public SteamProfile fetchProfile(String steamId) throws SteamApiException {
        List<SteamProfile> results = fetchProfiles(List.of(steamId));
        return results.isEmpty() ? null : results.get(0);
    }

    // FR 3.1.2 / FR 3.1.13 — Owned and recently played games

    /**
     * Gets the full game library for a user.
     *
     * includeAppInfo: true = response includes game name and icon hash.
     *   Use true for initial import, false if you only need playtime numbers.
     *
     * includeFree: true = include free-to-play titles (Dota 2, CS2, etc.)
     *   that the user has launched even though they didn't buy them.
     *
     * Returns the list sorted by total playtime, most played first.
     * Throws SteamApiException (status 401) if the library is private.
     */
    public List<SteamGame> fetchOwnedGames(String steamId,
                                           boolean includeAppInfo,
                                           boolean includeFree) throws SteamApiException {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("key",                       apiKey);
        params.put("steamid",                   steamId);
        params.put("include_appinfo",           includeAppInfo ? "true" : "false");
        params.put("include_played_free_games", includeFree ? "true" : "false");
        params.put("format",                    "json");

        String url  = SteamHttpClient.appendParams(SteamConstants.ENDPOINT_OWNED_GAMES, params);
        String json = SteamHttpClient.get(url);

        // Factory method: parseGames constructs the right SteamGame objects
        List<SteamGame> games = parseGames(json);

        // Sort descending by playtime — most played game is first in the list
        games.sort(Comparator.comparingInt((SteamGame g) -> g.playtimeForever).reversed());

        LOGGER.info("Fetched " + games.size() + " owned games for: " + steamId);
        return games;
    }

    /**
     * Gets only games played in the last 2 weeks — a lighter sync.
     * Useful for incremental resynchronisation (FR 3.1.13).
     */
    public List<SteamGame> fetchRecentGames(String steamId) throws SteamApiException {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("key",     apiKey);
        params.put("steamid", steamId);
        params.put("format",  "json");

        String url  = SteamHttpClient.appendParams(SteamConstants.ENDPOINT_RECENT_GAMES, params);
        String json = SteamHttpClient.get(url);

        List<SteamGame> games = parseGames(json);
        LOGGER.info("Fetched " + games.size() + " recent games for: " + steamId);
        return games;
    }

    // FR 3.1.22 — Achievements

    /**
     * Gets all achievements for a user in a specific game.
     * gameAppId examples: "570" = Dota 2, "730" = CS2, "440" = TF2.
     */
    public List<SteamAchievement> fetchAchievements(String steamId,
                                                     String gameAppId) throws SteamApiException {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("key",     apiKey);
        params.put("steamid", steamId);
        params.put("appid",   gameAppId);
        params.put("format",  "json");

        String url  = SteamHttpClient.appendParams(SteamConstants.ENDPOINT_ACHIEVEMENTS, params);
        String json = SteamHttpClient.get(url);

        List<SteamAchievement> achievements = parseAchievements(json);
        LOGGER.info("Fetched " + achievements.size() + " achievements for steamId="
                + steamId + " appId=" + gameAppId);
        return achievements;
    }

    // FR 3.1.25 — Friend list

    /**
     * Gets a user's Steam friends as a list of SteamFriend objects.
     * To find mutual friends, call this for two users and intersect the lists.
     * Requires the user's friend list to be public.
     */
    public List<SteamFriend> fetchFriendList(String steamId) throws SteamApiException {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("key",          apiKey);
        params.put("steamid",      steamId);
        params.put("relationship", "friend");
        params.put("format",       "json");

        String url  = SteamHttpClient.appendParams(SteamConstants.ENDPOINT_FRIEND_LIST, params);
        String json = SteamHttpClient.get(url);

        List<SteamFriend> friends = parseFriends(json);
        LOGGER.info("Fetched " + friends.size() + " friends for: " + steamId);
        return friends;
    }

    // Factory / Parser methods
    //
    // Each method is a factory that constructs the right model object from
    // raw JSON. Callers ask for List<SteamGame> — the factory decides how
    // to build each SteamGame from the JSON fields.

    // Parses ISteamUser/GetPlayerSummaries response → List<SteamProfile>
    private List<SteamProfile> parseProfiles(String json) {
        List<SteamProfile> profiles = new ArrayList<>();
        for (String block : JsonParser.objectsContaining(json, "steamid")) {
            profiles.add(new SteamProfile(
                JsonParser.field(block,    "steamid"),
                JsonParser.field(block,    "personaname"),
                JsonParser.field(block,    "avatarfull"),
                JsonParser.field(block,    "profileurl"),
                JsonParser.intField(block, "communityvisibilitystate"),
                JsonParser.longField(block,"lastlogoff")
            ));
        }
        return profiles;
    }

    // Parses GetOwnedGames / GetRecentlyPlayedGames response → List<SteamGame>
    // Both endpoints return the same object structure, so one parser handles both
    private List<SteamGame> parseGames(String json) {
        List<SteamGame> games = new ArrayList<>();
        for (String block : JsonParser.objectsContaining(json, "appid")) {
            String name = JsonParser.field(block, "name");
            games.add(new SteamGame(
                JsonParser.field(block,    "appid"),
                name != null ? name : "Unknown",
                JsonParser.field(block,    "img_icon_url"),
                JsonParser.intField(block, "playtime_forever"),
                JsonParser.intField(block, "playtime_2weeks")
            ));
        }
        return games;
    }

    // Parses ISteamUserStats/GetPlayerAchievements response → List<SteamAchievement>
    private List<SteamAchievement> parseAchievements(String json) {
        List<SteamAchievement> achievements = new ArrayList<>();
        for (String block : JsonParser.objectsContaining(json, "apiname")) {
            achievements.add(new SteamAchievement(
                JsonParser.field(block,    "apiname"),
                JsonParser.field(block,    "name"),
                JsonParser.field(block,    "description"),
                JsonParser.boolField(block,"achieved"),
                JsonParser.longField(block,"unlocktime")
            ));
        }
        return achievements;
    }

    // Parses ISteamUser/GetFriendList response → List<SteamFriend>
    private List<SteamFriend> parseFriends(String json) {
        List<SteamFriend> friends = new ArrayList<>();
        for (String block : JsonParser.objectsContaining(json, "steamid")) {
            friends.add(new SteamFriend(
                JsonParser.field(block,    "steamid"),
                JsonParser.field(block,    "relationship"),
                JsonParser.longField(block,"friend_since")
            ));
        }
        return friends;
    }
}
