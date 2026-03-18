package com.gamematchmaker.steam.model;

/*
 * SteamModels.java
 *
 *
 *   Each inner class holds exactly the data that one Steam API response
 *   returns. SteamProfile is profile data only. SteamGame is game library
 *   data only. They don't mix concerns. If Steam changes their profile API,
 *   only SteamProfile changes. If they change the games API, only SteamGame
 *   changes. Four classes, four separate reasons to change.

 */
public final class SteamModels {

    // This class is a named container for the inner classes below.
    // Nobody should ever create a SteamModels instance.
    private SteamModels() {}

    // SteamProfile
    // Source API: ISteamUser/GetPlayerSummaries/v2
    // Used by: FR 3.1.1 (login), FR 3.1.15 (view another user's profile)

    /**
     * A Steam user's public profile — name, avatar, visibility state.
     * Populated after successful OpenID authentication (FR 3.1.1).
     *
     * All fields are final: once a profile is created it cannot be modified.
     * If the profile changes, create a new SteamProfile with updated data.
     */
    public static class SteamProfile {

        // Steam's unique 64-bit account identifier, stored as String
        // because it exceeds int range (up to 76561198999999999)
        public final String steamId;

        // The display name the user set in Steam (not their login username)
        public final String personaName;

        // URL to their full-size (184x184) avatar image
        public final String avatarFull;

        // Link to their Steam Community profile page
        public final String profileUrl;

        // 1 = Private, 2 = Friends Only, 3 = Public
        // Must be 3 to allow library import (see isPublic() below)
        public final int communityVisibilityState;

        // When they last signed out of Steam (seconds since Jan 1, 1970)
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

        /**
         * Abstraction helper — returns true if this profile is public.
         * Hides the "== 3" detail from callers; they just ask isPublic().
         * We need this to be true before we can import the game library.
         */
        public boolean isPublic() {
            return communityVisibilityState == 3;
        }

        @Override
        public String toString() {
            return "SteamProfile{steamId='" + steamId + "', personaName='" + personaName + "'}";
        }
    }

    // SteamGame
    // Source API: IPlayerService/GetOwnedGames or GetRecentlyPlayedGames
    // Used by: FR 3.1.2 (import library), FR 3.1.13 (resync library)

    /**
     * One game from a user's Steam library.
     * Steam identifies every game by a numeric AppID
     */
    public static class SteamGame {

        // Steam's unique AppID for this game
        public final String appId;

        // Display name — only populated when include_appinfo=true was sent
        public final String name;

        // Hash to build the icon URL — pass to SteamConstants.iconUrl()
        public final String imgIconUrl;

        // Total minutes ever played across all time
        public final int playtimeForever;

        // Minutes played in the last two weeks (0 if not recently active)
        public final int playtime2Weeks;

        public SteamGame(String appId, String name, String imgIconUrl,
                         int playtimeForever, int playtime2Weeks) {
            this.appId           = appId;
            this.name            = name;
            this.imgIconUrl      = imgIconUrl;
            this.playtimeForever = playtimeForever;
            this.playtime2Weeks  = playtime2Weeks;
        }

        /**
         * Abstraction helper — converts playtime from minutes to hours.
         * Integer division in Java rounds down automatically.
         */
        public int playtimeHours() {
            return playtimeForever / 60;
        }

        @Override
        public String toString() {
            return "SteamGame{appId='" + appId + "', name='" + name
                    + "', playtime=" + playtimeForever + "min}";
        }
    }

    // SteamAchievement
    // Source API: ISteamUserStats/GetPlayerAchievements/v1
    // Used by: 3.1.22 (compare achievements in a group)

    /**
     * One achievement for a user in a specific game.
     * Requires both steamId AND gameAppId to fetch.
     */
    public static class SteamAchievement {

        // Internal key Steam uses to identify this achievement (e.g. "ACH_WIN_1")
        public final String apiName;

        // Friendly name shown to players (e.g. "First Blood")
        public final String displayName;

        // Description of how to earn it
        public final String description;

        // Whether this specific user has earned it
        public final boolean achieved;

        // Timestamp when they earned it (0 = not yet earned)
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

    // SteamFriend
    // Source API: ISteamUser/GetFriendList/v1
    // Used by: 3.1.25 (retrieve mutual friends)

    /**
     * One entry from a user's Steam friend list.
     * Only contains the friend's SteamID — call fetchProfile() to get name/avatar.
     */
    public static class SteamFriend {

        // The SteamID of this friend
        public final String steamId;

        // Always "friend" for our queries (we filter by relationship=friend)
        public final String relationship;

        // Unix timestamp of when this friendship was established
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
