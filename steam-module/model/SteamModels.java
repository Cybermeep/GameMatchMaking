package com.gamematchmaker.steam.model;

/**
 * Lightweight data models for raw Steam API responses.
 *
 * These are Steam-layer DTOs — they represent exactly what the Steam API
 * returns and are intentionally separate from the application's domain
 * models (User, Game, Achievement). The importer classes convert these
 * into domain objects.
 */
public final class SteamModels {

    private SteamModels() {}

    // =========================================================================
    // SteamProfile — from ISteamUser/GetPlayerSummaries
    // =========================================================================

    /**
     * Public profile data returned by Steam's GetPlayerSummaries endpoint.
     * Populated after successful OpenID authentication (FR 3.1.1).
     */
    public static class SteamProfile {

        /** 64-bit Steam Community ID as a string. */
        public final String steamId;

        /** Steam display name (persona name). */
        public final String personaName;

        /** URL to the user's full-size avatar image. */
        public final String avatarFull;

        /** URL to the user's Steam Community profile page. */
        public final String profileUrl;

        /**
         * Profile visibility: 1 = Private, 2 = Friends Only, 3 = Public.
         * Game library import requires visibility == 3.
         */
        public final int communityVisibilityState;

        /** Unix timestamp of the user's last logoff from Steam. */
        public final long lastLogoff;

        public SteamProfile(String steamId, String personaName, String avatarFull,
                            String profileUrl, int communityVisibilityState, long lastLogoff) {
            this.steamId                  = steamId;
            this.personaName              = personaName;
            this.avatarFull               = avatarFull;
            this.profileUrl               = profileUrl;
            this.communityVisibilityState = communityVisibilityState;
            this.lastLogoff               = lastLogoff;
        }

        /** Returns true if the profile is publicly visible (required for library import). */
        public boolean isPublic() {
            return communityVisibilityState == 3;
        }

        @Override
        public String toString() {
            return "SteamProfile{steamId='" + steamId + "', personaName='" + personaName + "'}";
        }
    }

    // =========================================================================
    // SteamGame — from IPlayerService/GetOwnedGames and GetRecentlyPlayedGames
    // =========================================================================

    /**
     * A single entry from the user's Steam game library.
     * Populated by ImportSteam during library import (FR 3.1.2, 3.1.13).
     */
    public static class SteamGame {

        /** Steam AppID (unique identifier for the game). */
        public final String appId;

        /** Display name of the game (present when include_appinfo=true). */
        public final String name;

        /** Icon hash — use SteamConstants.iconUrl() to build the full URL. */
        public final String imgIconUrl;

        /** Total minutes played across all time. */
        public final int playtimeForever;

        /** Minutes played in the last two weeks (0 if not recently played). */
        public final int playtime2Weeks;

        public SteamGame(String appId, String name, String imgIconUrl,
                         int playtimeForever, int playtime2Weeks) {
            this.appId           = appId;
            this.name            = name;
            this.imgIconUrl      = imgIconUrl;
            this.playtimeForever = playtimeForever;
            this.playtime2Weeks  = playtime2Weeks;
        }

        /** Convenience: playtime in hours (rounded down). */
        public int playtimeHours() {
            return playtimeForever / 60;
        }

        @Override
        public String toString() {
            return "SteamGame{appId='" + appId + "', name='" + name + "', playtime=" + playtimeForever + "min}";
        }
    }

    // =========================================================================
    // SteamAchievement — from ISteamUserStats/GetPlayerAchievements
    // =========================================================================

    /**
     * A single achievement entry for a user in a specific game.
     * Used by FR 3.1.22 (Compare Achievements in a Group).
     */
    public static class SteamAchievement {

        /** Internal API name (unique within the game). */
        public final String apiName;

        /** Human-readable display name. */
        public final String displayName;

        /** Description of how to earn the achievement. */
        public final String description;

        /** True if this user has earned the achievement. */
        public final boolean achieved;

        /** Unix timestamp when the achievement was unlocked (0 if not achieved). */
        public final long unlockTime;

        public SteamAchievement(String apiName, String displayName, String description,
                                boolean achieved, long unlockTime) {
            this.apiName     = apiName;
            this.displayName = displayName;
            this.description = description;
            this.achieved    = achieved;
            this.unlockTime  = unlockTime;
        }

        @Override
        public String toString() {
            return "SteamAchievement{apiName='" + apiName + "', achieved=" + achieved + "}";
        }
    }

    // =========================================================================
    // SteamFriend — from ISteamUser/GetFriendList
    // =========================================================================

    /**
     * A single entry from a user's Steam friend list.
     * Used by FR 3.1.25 (Retrieve Mutual Friends).
     */
    public static class SteamFriend {

        /** The friend's 64-bit SteamID. */
        public final String steamId;

        /** Relationship type — typically "friend". */
        public final String relationship;

        /** Unix timestamp when the friendship was established. */
        public final long friendSince;

        public SteamFriend(String steamId, String relationship, long friendSince) {
            this.steamId      = steamId;
            this.relationship = relationship;
            this.friendSince  = friendSince;
        }

        @Override
        public String toString() {
            return "SteamFriend{steamId='" + steamId + "'}";
        }
    }
}
