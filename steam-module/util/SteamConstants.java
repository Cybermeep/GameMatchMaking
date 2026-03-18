package com.gamematchmaker.steam.util;

/**
 * Central registry of all Steam API constants, endpoint URLs, OpenID
 * strings, and configuration defaults for the steam module.
 *
 * Every other class in this module reads from here — nothing is
 * hard-coded elsewhere.
 */
public final class SteamConstants {

    private SteamConstants() {}

    // =========================================================================
    // Base URLs
    // =========================================================================

    /** Root of all Steam Web API calls. */
    public static final String API_BASE = "https://api.steampowered.com";

    /** Steam Community base (used for profile URLs and OpenID). */
    public static final String COMMUNITY_BASE = "https://steamcommunity.com";

    /** Steam OpenID 2.0 login endpoint. */
    public static final String OPENID_ENDPOINT = COMMUNITY_BASE + "/openid/login";

    // =========================================================================
    // Steam Web API Endpoints
    // =========================================================================

    /** ISteamUser — GetPlayerSummaries/v2 — public profile data. */
    public static final String ENDPOINT_PLAYER_SUMMARIES =
            API_BASE + "/ISteamUser/GetPlayerSummaries/v2/";

    /** IPlayerService — GetOwnedGames/v1 — full owned game library. */
    public static final String ENDPOINT_OWNED_GAMES =
            API_BASE + "/IPlayerService/GetOwnedGames/v1/";

    /** IPlayerService — GetRecentlyPlayedGames/v1 — last-2-weeks games. */
    public static final String ENDPOINT_RECENT_GAMES =
            API_BASE + "/IPlayerService/GetRecentlyPlayedGames/v1/";

    /** ISteamUserStats — GetPlayerAchievements/v1 — per-game achievements. */
    public static final String ENDPOINT_ACHIEVEMENTS =
            API_BASE + "/ISteamUserStats/GetPlayerAchievements/v1/";

    /** ISteamUser — GetFriendList/v1 — friend SteamID list. */
    public static final String ENDPOINT_FRIEND_LIST =
            API_BASE + "/ISteamUser/GetFriendList/v1/";

    // =========================================================================
    // OpenID 2.0 Protocol Values
    // =========================================================================

    /** OpenID namespace URI required by Steam. */
    public static final String OPENID_NS = "http://specs.openid.net/auth/2.0";

    /** OpenID mode to initiate a login (redirect user to Steam). */
    public static final String OPENID_MODE_CHECKID = "checkid_setup";

    /** OpenID mode to verify a callback response from Steam. */
    public static final String OPENID_MODE_CHECK_AUTH = "check_authentication";

    /** Identifier used in directed identity requests. */
    public static final String OPENID_IDENTIFIER_SELECT =
            "http://specs.openid.net/auth/2.0/identifier_select";

    /** URL prefix that Steam appends the 64-bit SteamID to in claimed_id. */
    public static final String OPENID_CLAIMED_ID_PREFIX =
            COMMUNITY_BASE + "/openid/id/";

    // =========================================================================
    // Session Keys
    // =========================================================================

    /** HttpSession attribute key for the authenticated user's SteamID string. */
    public static final String SESSION_STEAM_ID = "steamId";

    /** HttpSession attribute key for the full authenticated User object. */
    public static final String SESSION_USER = "authenticatedUser";

    // =========================================================================
    // HTTP / Networking
    // =========================================================================

    /** Connect and read timeout in milliseconds for all Steam API requests. */
    public static final int HTTP_TIMEOUT_MS = 8_000;

    /** Maximum number of Steam IDs per GetPlayerSummaries batch call. */
    public static final int MAX_IDS_PER_BATCH = 100;

    // =========================================================================
    // Steam Icon URL helper
    // =========================================================================

    /**
     * Builds the CDN URL for a game's small icon image.
     * @param appId     Steam AppID (e.g. "570")
     * @param iconHash  Hash from the API response (img_icon_url field)
     * @return Full CDN URL, or null if either argument is null.
     */
    public static String iconUrl(String appId, String iconHash) {
        if (appId == null || iconHash == null) return null;
        return "https://media.steampowered.com/steamcommunity/public/images/apps/"
                + appId + "/" + iconHash + ".jpg";
    }
}
