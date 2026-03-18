package com.gamematchmaker.steam.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * JsonParser.java
 *
 *   extract field values from a JSON string.
 *   It does NOT make HTTP calls, does NOT create domain objects — those
 *   are SteamHttpClient's and SteamApiClient's jobs respectively.

 */
public final class JsonParser {

    // Private constructor — pure utility class, never instantiated
    private JsonParser() {}

    /**
     * Pulls out the value of a named field from a JSON string.
     *
     * Handles both  "key": "value"  (string)
     * and           "key": 12345   (number)
     *
     * Returns null if the field isn't found.
     * Example: field(json, "personaname") returns "Jeff"
     */
    public static String field(String json, String key) {
        if (json == null || key == null) return null;
        Pattern p = Pattern.compile(
            "\"" + Pattern.quote(key) + "\"\\s*:\\s*\"?([^\"\\s,}\\]]+)\"?"
        );
        Matcher m = p.matcher(json);
        return m.find() ? m.group(1) : null;
    }

    /**
     * Same as field(), but converts the result to an int.
     * Returns 0 if the field is missing or not a valid number.
     * Example: intField(json, "playtime_forever") returns 120
     */
    public static int intField(String json, String key) {
        String value = field(json, key);
        if (value == null) return 0;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Same as field(), but converts to a long.
     * Used for Unix timestamps and 64-bit SteamIDs, which overflow int.
     */
    public static long longField(String json, String key) {
        String value = field(json, key);
        if (value == null) return 0L;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    /**
     * Returns true if the field value is "1" or "true".
     * Steam uses 1/0 for booleans (e.g. "achieved": 1).
     */
    public static boolean boolField(String json, String key) {
        String value = field(json, key);
        return "1".equals(value) || "true".equalsIgnoreCase(value);
    }

    /**
     * Finds all flat JSON objects {...} inside a larger JSON string that
     * contain a specific key. Used to iterate over arrays of objects in
     * Steam API responses (e.g. the "games" array in GetOwnedGames).
     *
     * Example: objectsContaining(json, "appid") returns each game block
     * as its own string that can be passed back to field() individually.
     *
     * Note: This only matches non-nested objects (no {} inside {}).
     * Steam's API responses fit within that constraint for our use cases.
     */
    public static String[] objectsContaining(String json, String anchorKey) {
        if (json == null) return new String[0];
        Pattern p = Pattern.compile("\\{[^{}]*\"" + Pattern.quote(anchorKey) + "\"[^{}]*\\}");
        Matcher m = p.matcher(json);
        List<String> blocks = new ArrayList<>();
        while (m.find()) {
            blocks.add(m.group());
        }
        return blocks.toArray(new String[0]);
    }
}
