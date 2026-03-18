package com.gamematchmaker.steam.api;

import com.gamematchmaker.steam.exception.SteamApiException;
import com.gamematchmaker.steam.model.SteamModels.*;
import com.gamematchmaker.steam.util.JsonParser;
import com.gamematchmaker.steam.util.SteamConstants;
import com.gamematchmaker.steam.util.SteamHttpClient;

import java.util.*;
import java.util.logging.Logger;

/**
 * Single point of contact for all Steam Web API calls.
 *
 * Holds the API key and translates raw HTTP responses into typed
 * {@link com.gamematchmaker.steam.model.SteamModels} objects.
 * All parsing is delegated to {@link JsonParser}; all networking is
 * delegated to {@link SteamHttpClient}.
 *
 * This class is intentionally free of business logic — it only knows
 * how to call Steam and parse what comes back.
 *
 * Supports:
 *   FR 3.1.2  — Import Steam Game Library      (fetchOwnedGames)
 *   FR 3.1.13 — Resynchronize Game Library     (fetchRecentlyPlayedGames)
 *   FR 3.1.15 — Retrieve another user's profile (fetchProfiles)
 *   FR 3.1.22 — Compare Achievements           (fetchAchievements)
 *   FR 3.1.25 — Retrieve Mutual Friends         (fetchFriendList)
 */
public class SteamApiClient {

    private static final Logger LOGGER = Logger.getLogger(SteamApiClient.class.getName());

    private final String apiKey;

    /**
     * @param apiKey Your Steam Web API key.
     *               Obtain one at https://steamcommunity.com/dev/apikey
     *               Never hard-code this — read from an environment variable.
     */
    public SteamApiClient(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("Steam API key must not be null or blank.");
        }
        this.apiKey = apiKey;
    }

    // =========================================================================
    // Player Summaries  (FR 3.1.1, FR 3.1.15)
    // =========================================================================

    /**
     * Fetches public profile data for one or more Steam users.
     *
     * Steam accepts up to {@value SteamConstants#MAX_IDS_PER_BATCH} SteamIDs
     * per request. This method handles batching automatically.
     *
     * @param steamIds One or more 64-bit SteamID strings.
     * @return List of {@link SteamProfile} objects (may be shorter than input if
     *         some profiles are private or do not exist).
     */
    public List<SteamProfile> fetchProfiles(List<String> steamIds) throws SteamApiException {
        if (steamIds == null || steamIds.isEmpty()) return Collections.emptyList();

        List<SteamProfile> all = new ArrayList<>();

        // Batch in groups of MAX_IDS_PER_BATCH
        for (int i = 0; i < steamIds.size(); i += SteamConstants.MAX_IDS_PER_BATCH) {
            List<String> batch = steamIds.subList(i,
                    Math.min(i + SteamConstants.MAX_IDS_PER_BATCH, steamIds.size()));

            Map<String, String> params = new LinkedHashMap<>();
            params.put("key",      apiKey);
            params.put("steamids", String.join(",", batch));
            params.put("format",   "json");

            String url  = SteamHttpClient.appendParams(SteamConstants.ENDPOINT_PLAYER_SUMMARIES, params);
            String json = SteamHttpClient.get(url);
            all.addAll(parseProfiles(json));
        }

        LOGGER.info("Fetched " + all.size() + " profiles.");
        return all;
    }

    /**
     * Convenience overload for a single SteamID.
     */
    public SteamProfile fetchProfile(String steamId) throws SteamApiException {
        List<SteamProfile> profiles = fetchProfiles(List.of(steamId));
        return profiles.isEmpty() ? null : profiles.get(0);
    }

    // =========================================================================
    // Owned Games  (FR 3.1.2, FR 3.1.13)
    // =========================================================================

    /**
     * Fetches the full owned game library for a Steam user.
     *
     * @param steamId         The target user's 64-bit SteamID.
     * @param includeAppInfo  When true, includes game title and icon hash.
     *                        Should be true for initial import (FR 3.1.2).
     * @param includeFree     When true, includes free-to-play games the user
     *                        has ever launched.
     * @return List of {@link SteamGame} objects sorted by playtime descending.
     * @throws SteamApiException If the user's profile is private (HTTP 401)
     *                           or Steam is unavailable.
     */
    public List<SteamGame> fetchOwnedGames(String steamId,
                                           boolean includeAppInfo,
                                           boolean includeFree) throws SteamApiException {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("key",                       apiKey);
        params.put("steamid",                   steamId);
        params.put("include_appinfo",           includeAppInfo ? "true" : "false");
        params.put("include_played_free_games", includeFree    ? "true" : "false");
        params.put("format",                    "json");

        String url  = SteamHttpClient.appendParams(SteamConstants.ENDPOINT_OWNED_GAMES, params);
        String json = SteamHttpClient.get(url);

        List<SteamGame> games = parseGames(json);
        games.sort(Comparator.comparingInt((SteamGame g) -> g.playtimeForever).reversed());

        LOGGER.info("Fetched " + games.size() + " owned games for SteamID: " + steamId);
        return games;
    }

    // =========================================================================
    // Recently Played Games  (FR 3.1.13 — lighter resync)
    // =========================================================================

    /**
     * Fetches games the user has played in the last two weeks.
     * Lighter than a full library sync; useful for incremental resynchronization.
     *
     * @param steamId The target user's 64-bit SteamID.
     * @return List of recently played {@link SteamGame} objects.
     */
    public List<SteamGame> fetchRecentGames(String steamId) throws SteamApiException {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("key",     apiKey);
        params.put("steamid", steamId);
        params.put("format",  "json");

        String url  = SteamHttpClient.appendParams(SteamConstants.ENDPOINT_RECENT_GAMES, params);
        String json = SteamHttpClient.get(url);

        List<SteamGame> games = parseGames(json);
        LOGGER.info("Fetched " + games.size() + " recently played games for SteamID: " + steamId);
        return games;
    }

    // =========================================================================
    // Achievements  (FR 3.1.22)
    // =========================================================================

    /**
     * Fetches all achievements for a specific game for a specific user,
     * including whether each one has been unlocked.
     *
     * @param steamId   The target user's 64-bit SteamID.
     * @param gameAppId The Steam AppID of the game (e.g. "570" for Dota 2).
     * @return List of {@link SteamAchievement} objects.
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
        LOGGER.info("Fetched " + achievements.size()
                + " achievements for SteamID: " + steamId + ", AppID: " + gameAppId);
        return achievements;
    }

    // =========================================================================
    // Friend List  (FR 3.1.25)
    // =========================================================================

    /**
     * Fetches the friend list for a Steam user.
     * Requires the user's friend list to be publicly visible.
     *
     * @param steamId The target user's 64-bit SteamID.
     * @return List of {@link SteamFriend} objects.
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
        LOGGER.info("Fetched " + friends.size() + " friends for SteamID: " + steamId);
        return friends;
    }

    // =========================================================================
    // Parsers — private, each handles one API response shape
    // =========================================================================

    private List<SteamProfile> parseProfiles(String json) {
        List<SteamProfile> profiles = new ArrayList<>();
        for (String block : JsonParser.objectsContaining(json, "steamid")) {
            profiles.add(new SteamProfile(
                JsonParser.field(block, "steamid"),
                JsonParser.field(block, "personaname"),
                JsonParser.field(block, "avatarfull"),
                JsonParser.field(block, "profileurl"),
                JsonParser.intField(block, "communityvisibilitystate"),
                JsonParser.longField(block, "lastlogoff")
            ));
        }
        return profiles;
    }

    private List<SteamGame> parseGames(String json) {
        List<SteamGame> games = new ArrayList<>();
        for (String block : JsonParser.objectsContaining(json, "appid")) {
            String name = JsonParser.field(block, "name");
            games.add(new SteamGame(
                JsonParser.field(block, "appid"),
                name != null ? name : "Unknown",
                JsonParser.field(block, "img_icon_url"),
                JsonParser.intField(block, "playtime_forever"),
                JsonParser.intField(block, "playtime_2weeks")
            ));
        }
        return games;
    }

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
