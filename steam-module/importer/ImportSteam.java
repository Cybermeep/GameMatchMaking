package com.gamematchmaker.steam.importer;

import com.gamematchmaker.steam.api.SteamApiClient;
import com.gamematchmaker.steam.exception.SteamApiException;
import com.gamematchmaker.steam.model.SteamModels.*;

import java.util.List;
import java.util.logging.Logger;

/**
 * Captures information related to the importation of Steam data.
 *
 * Directly corresponds to the "ImportSteam" class in the SRS Class Diagram
 * (Section 4.2.2.1). Holds the user's steamID, their API key, and the
 * appID for per-game queries.
 *
 * This class is the primary entry point for importing a single user's data.
 * It delegates all HTTP calls to {@link SteamApiClient} and all local file
 * operations to {@link ImportLocal}.
 *
 * ┌──────────────┐      delegates      ┌─────────────────┐
 * │ ImportSteam  │ ──────────────────► │  SteamApiClient │ ──► Steam Web API
 * │  (this)      │                     └─────────────────┘
 * └──────────────┘
 *        │  delegates (local files)
 *        ▼
 * ┌──────────────┐
 * │ ImportLocal  │ ──► Local Steam filesystem
 * └──────────────┘
 *
 * Supports:
 *   FR 3.1.2  — Import Steam Game Library
 *   FR 3.1.13 — Resynchronize Game Library
 *   FR 3.1.14 — Resynchronize Installed Games
 *   FR 3.1.22 — Compare Achievements in a Group
 *   FR 3.1.25 — Retrieve Mutual Friends
 */
public class ImportSteam {

    private static final Logger LOGGER = Logger.getLogger(ImportSteam.class.getName());

    /** The user's 64-bit Steam Community ID (per SRS class definition). */
    private final String steamId;

    /** The Steam Web API key (per SRS class definition). */
    private final String apiKey;

    /**
     * The Steam AppID for per-game queries (per SRS class definition).
     * Set automatically when calling {@link #fetchAchievements(String)}.
     */
    private String appId;

    /** Shared API client — constructed once, reused across all method calls. */
    private final SteamApiClient apiClient;

    public ImportSteam(String steamId, String apiKey) {
        if (steamId == null || steamId.isBlank())
            throw new IllegalArgumentException("SteamID must not be null or blank.");
        if (apiKey == null || apiKey.isBlank())
            throw new IllegalArgumentException("API key must not be null or blank.");

        this.steamId   = steamId;
        this.apiKey    = apiKey;
        this.apiClient = new SteamApiClient(apiKey);
    }

    // =========================================================================
    // FR 3.1.2 / FR 3.1.13 — Import / Resynchronize Game Library
    // =========================================================================

    /**
     * Fetches the user's complete owned game library from Steam.
     * Used for initial library import (FR 3.1.2) and full resync (FR 3.1.13).
     *
     * @param includeAppInfo    Include game title and icon URL in results.
     * @param includePlayedFree Include free-to-play games the user has launched.
     * @return List of {@link SteamGame} objects sorted by playtime descending.
     */
    public List<SteamGame> fetchOwnedGames(boolean includeAppInfo,
                                            boolean includePlayedFree) throws SteamApiException {
        LOGGER.info("Importing owned games for SteamID: " + steamId);
        return apiClient.fetchOwnedGames(steamId, includeAppInfo, includePlayedFree);
    }

    /**
     * Fetches only games played in the last two weeks — lighter than a full sync.
     * Useful for incremental resynchronization (FR 3.1.13).
     */
    public List<SteamGame> fetchRecentGames() throws SteamApiException {
        LOGGER.info("Fetching recent games for SteamID: " + steamId);
        return apiClient.fetchRecentGames(steamId);
    }

    // =========================================================================
    // FR 3.1.14 — Resynchronize Installed Games
    // =========================================================================

    /**
     * Reads locally installed games from the Steam filesystem.
     * Delegates to {@link ImportLocal} using the auto-detected Steam path.
     *
     * @return A new {@link ImportLocal} scoped to the auto-detected Steam path,
     *         ready to call {@link ImportLocal#getInstalledGames()}.
     *         Returns {@code null} if Steam is not installed on this machine.
     */
    public ImportLocal getLocalImporter() {
        String steamPath = ImportLocal.detectSteamPath();
        if (steamPath == null) {
            LOGGER.warning("Steam installation not found on this machine.");
            return null;
        }
        return new ImportLocal(steamPath, "appmanifest_*.acf");
    }

    // =========================================================================
    // FR 3.1.22 — Compare Achievements in a Group
    // =========================================================================

    /**
     * Fetches all achievements for the user in a specific game, including
     * whether each has been unlocked and the unlock timestamp.
     *
     * @param gameAppId Steam AppID of the game (e.g. "570" for Dota 2).
     * @return List of {@link SteamAchievement} objects.
     */
    public List<SteamAchievement> fetchAchievements(String gameAppId) throws SteamApiException {
        this.appId = gameAppId;  // set per SRS class definition
        LOGGER.info("Fetching achievements for SteamID: " + steamId + ", AppID: " + gameAppId);
        return apiClient.fetchAchievements(steamId, gameAppId);
    }

    // =========================================================================
    // FR 3.1.25 — Retrieve Mutual Friends
    // =========================================================================

    /**
     * Fetches the user's Steam friend list.
     *
     * To find mutual friends between two users, call this on both users and
     * compute the intersection of their {@link SteamFriend#steamId} sets.
     *
     * @return List of {@link SteamFriend} objects.
     */
    public List<SteamFriend> fetchFriends() throws SteamApiException {
        LOGGER.info("Fetching friend list for SteamID: " + steamId);
        return apiClient.fetchFriendList(steamId);
    }

    // =========================================================================
    // Getters — matching SRS class definition fields
    // =========================================================================

    /** The user's 64-bit Steam Community ID. */
    public String getSteamId() { return steamId; }

    /** The Steam Web API key used for all requests. */
    public String getApiKey()  { return apiKey; }

    /**
     * The Steam AppID set by the most recent {@link #fetchAchievements} call.
     * Null until fetchAchievements is called.
     */
    public String getAppId()   { return appId; }
}
