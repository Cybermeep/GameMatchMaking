package com.gamematchmaker.steam.util;

/*
 * SteamConstants.java
 *
 *   This class has exactly one job: hold all the constant values for the
 *   steam module. Nothing else lives here, so there is only one reason it
 *   would ever need to change — if Steam changes a URL or key name.
 
 */
public final class SteamConstants {

    // Private constructor enforces this as a pure constants holder.
    // Nobody should ever call "new SteamConstants()".
    private SteamConstants() {}

    // ------------------------------------------------------------------
    // Base URLs
    // ------------------------------------------------------------------

    // Root of all Steam Web API calls
    public static final String API_BASE = "https://api.steampowered.com";

    // Steam Community site — used for OpenID and profile pages
    public static final String COMMUNITY_BASE = "https://steamcommunity.com";

    // Where we redirect the user's browser when they click "Login with Steam"
    public static final String OPENID_ENDPOINT = COMMUNITY_BASE + "/openid/login";

    // Steam Web API endpoint paths

    // Gets public profile info for one or more users (name, avatar, etc.)
    public static final String ENDPOINT_PLAYER_SUMMARIES =
            API_BASE + "/ISteamUser/GetPlayerSummaries/v2/";

    // Gets the full list of games owned by a user
    public static final String ENDPOINT_OWNED_GAMES =
            API_BASE + "/IPlayerService/GetOwnedGames/v1/";

    // Gets games played in the last 2 weeks (lighter than full library)
    public static final String ENDPOINT_RECENT_GAMES =
            API_BASE + "/IPlayerService/GetRecentlyPlayedGames/v1/";

    // Gets all achievements for a user in a specific game
    public static final String ENDPOINT_ACHIEVEMENTS =
            API_BASE + "/ISteamUserStats/GetPlayerAchievements/v1/";

    // Gets a user's Steam friend list
    public static final String ENDPOINT_FRIEND_LIST =
            API_BASE + "/ISteamUser/GetFriendList/v1/";

    // OpenID 2.0 protocol strings

    // Required namespace for all OpenID requests
    public static final String OPENID_NS = "http://specs.openid.net/auth/2.0";

    // Mode used to start a login
    public static final String OPENID_MODE_CHECKID = "checkid_setup";

    // Mode used to verify a callback response came from Steam
    public static final String OPENID_MODE_CHECK_AUTH = "check_authentication";

    // Tells Steam to let the user pick which account to use
    public static final String OPENID_IDENTIFIER_SELECT =
            "http://specs.openid.net/auth/2.0/identifier_select";

    // Steam appends the user's 64-bit SteamID to the end of this prefix URL
    public static final String OPENID_CLAIMED_ID_PREFIX =
            COMMUNITY_BASE + "/openid/id/";

    // HTTP session attribute keys

    // Key for the logged-in user's SteamID string stored in the session
    public static final String SESSION_STEAM_ID = "steamId";

    // Key for the full SteamProfile object stored in the session
    public static final String SESSION_USER = "authenticatedUser";

    // Network settings

    // Max time to wait for Steam to respond (8 seconds)
    public static final int HTTP_TIMEOUT_MS = 8_000;

    // Steam's API only accepts up to 100 SteamIDs per GetPlayerSummaries call
    public static final int MAX_IDS_PER_BATCH = 100;

    // Helper method

    /**
     * Builds the CDN URL for a game's icon image.
     *
     * Steam gives us a hash string per game icon. This helper turns
     * that hash + appId into a full image URL. Returns null if either
     * argument is null (some games don't have icons).
     */
    public static String iconUrl(String appId, String iconHash) {
        if (appId == null || iconHash == null) return null;
        return "https://media.steampowered.com/steamcommunity/public/images/apps/"
                + appId + "/" + iconHash + ".jpg";
    }
}
