package com.gamematchmaker.steam.importer;

import com.gamematchmaker.steam.api.SteamApiClient;
import com.gamematchmaker.steam.exception.SteamApiException;
import com.gamematchmaker.steam.model.SteamModels.*;

import java.util.List;
import java.util.logging.Logger;

/*
 * ImportSteam.java
 *
 *   One responsibility: provide a user-scoped entry point for importing
 *   one Steam user's data. It does NOT make raw HTTP calls (SteamApiClient
 *   does that), does NOT parse JSON (JsonParser does that), does NOT manage
 *   sessions. The SRS class diagram (Section 4.2.2.1) defines exactly three
 *   fields: steamId, apiKey, appId — matching a single focused role.
 *
 */
public class ImportSteam {

    private static final Logger LOGGER = Logger.getLogger(ImportSteam.class.getName());


    /** The user's 64-bit Steam Community ID. */
    private final String steamId;

    /** The Steam Web API key used for all requests. */
    private final String apiKey;

    /**
     * The Steam AppID for per-game queries.
     * Set automatically when fetchAchievements() is called.
     * Null until then.
     */
    private String appId;

    // DIP: we hold a reference to SteamApiClient (the abstraction),
    // not to HttpURLConnection or any concrete HTTP class directly.
    private final SteamApiClient apiClient;

    /**
     * Creates an ImportSteam scoped to one specific Steam user.
     *
     * steamId: the user's 64-bit Steam Community ID
     * apiKey:  your Steam Web API key (read from System.getenv("STEAM_API_KEY"))
     */
    public ImportSteam(String steamId, String apiKey) {
        if (steamId == null || steamId.isBlank())
            throw new IllegalArgumentException("SteamID is required.");
        if (apiKey == null || apiKey.isBlank())
            throw new IllegalArgumentException("API key is required.");

        this.steamId   = steamId;
        this.apiKey    = apiKey;
        this.apiClient = new SteamApiClient(apiKey);
    }

    // FR 3.1.2 / FR 3.1.13 — Import / Resynchronize game library

    /**
     * Gets all games this user owns on Steam.
     *
     * includeAppInfo:   true = include game name and icon hash (use for initial import)
     * includePlayedFree: true = include free titles (Dota 2, CS2) they've ever launched
     *
     * Results are sorted by total playtime, most played first.
     * Throws SteamApiException (401) if the user's library is set to Private.
     */
    public List<SteamGame> fetchOwnedGames(boolean includeAppInfo,
                                            boolean includePlayedFree) throws SteamApiException {
        LOGGER.info("Fetching full game library for SteamID: " + steamId);
        return apiClient.fetchOwnedGames(steamId, includeAppInfo, includePlayedFree);
    }

    /**
     * Gets only games played in the last 2 weeks — lighter than a full sync.
     * Good for periodic resyncs where we don't want to re-fetch the whole library.
     */
    public List<SteamGame> fetchRecentGames() throws SteamApiException {
        LOGGER.info("Fetching recently played games for SteamID: " + steamId);
        return apiClient.fetchRecentGames(steamId);
    }

    // FR 3.1.14 — Resynchronize installed games

    /**
     * Returns an ImportLocal scoped to the auto-detected Steam installation.
     * ImportLocal reads the local filesystem to find which games are installed.
     * Returns null if Steam is not found on this machine.
     *
     * Usage:
     *   ImportLocal local = importer.getLocalImporter();
     *   if (local != null) {
     *       List<SteamGame> installed = local.getInstalledGames();
     *   }
     */
    public ImportLocal getLocalImporter() {
        String steamPath = ImportLocal.detectSteamPath();
        if (steamPath == null) {
            LOGGER.warning("Steam does not appear to be installed on this machine.");
            return null;
        }
        return new ImportLocal(steamPath, "appmanifest_*.acf");
    }

    // FR 3.1.22 — Compare achievements in a group

    /**
     * Gets all achievements for this user in one specific game.
     * Includes both earned and unearned achievements.
     *
     * gameAppId: Steam's numeric game identifier.
     *   Examples: "570" = Dota 2, "730" = CS2, "440" = TF2
     *
     * This also sets the appId field (per the SRS class diagram).
     */
    public List<SteamAchievement> fetchAchievements(String gameAppId) throws SteamApiException {
        this.appId = gameAppId; // stored per the SRS class diagram definition
        LOGGER.info("Fetching achievements for steamId=" + steamId + " appId=" + gameAppId);
        return apiClient.fetchAchievements(steamId, gameAppId);
    }

    // FR 3.1.25 — Retrieve mutual friends

    /**
     * Gets this user's Steam friend list.
     * To find mutual friends: call fetchFriends() on two users and
     * find the SteamIDs that appear in both lists.
     */
    public List<SteamFriend> fetchFriends() throws SteamApiException {
        LOGGER.info("Fetching friend list for SteamID: " + steamId);
        return apiClient.fetchFriendList(steamId);
    }

    // Getters 

    public String getSteamId() { return steamId; }
    public String getApiKey()  { return apiKey; }

    /** Returns the appId set by the most recent fetchAchievements() call. Null until then. */
    public String getAppId()   { return appId; }
}
