package com.gamematchmaker.steam.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minimal JSON field extractor for parsing Steam API responses.
 *
 * Steam API responses are well-structured and predictable, so a
 * lightweight regex-based approach is used here to avoid requiring
 * an external JSON library dependency in the steam module.
 *
 * Replace with Jackson or Gson in any production build that already
 * includes those dependencies — just swap out the method bodies.
 *
 * All methods are static and stateless.
 */
public final class JsonParser {

    private JsonParser() {}

    /**
     * Extracts the string or numeric value of a named JSON field.
     *
     * Handles both quoted string values:   "key":"value"
     * and bare numeric values:             "key":12345
     *
     * @param json  The JSON text to search.
     * @param key   The field name to look for.
     * @return The field's value as a String, or {@code null} if not found.
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
     * Same as {@link #field} but returns an {@code int}, defaulting to 0
     * if the field is absent or not parseable.
     */
    public static int intField(String json, String key) {
        String v = field(json, key);
        if (v == null) return 0;
        try { return Integer.parseInt(v); } catch (NumberFormatException e) { return 0; }
    }

    /**
     * Same as {@link #field} but returns a {@code long}, defaulting to 0L.
     */
    public static long longField(String json, String key) {
        String v = field(json, key);
        if (v == null) return 0L;
        try { return Long.parseLong(v); } catch (NumberFormatException e) { return 0L; }
    }

    /**
     * Returns true if the named field has the value {@code "1"} or {@code "true"}.
     */
    public static boolean boolField(String json, String key) {
        String v = field(json, key);
        return "1".equals(v) || "true".equalsIgnoreCase(v);
    }

    /**
     * Returns all JSON object blocks (top-level {...}) from a JSON array or
     * response body that contain a specific key. Used for iterating over
     * arrays of game or achievement objects in Steam API responses.
     *
     * @param json      The JSON text to search.
     * @param anchorKey A key that must be present inside each block to match.
     * @return Array of matching JSON object strings.
     */
    public static String[] objectsContaining(String json, String anchorKey) {
        if (json == null) return new String[0];
        Pattern p = Pattern.compile("\\{[^{}]*\"" + Pattern.quote(anchorKey) + "\"[^{}]*\\}");
        Matcher m = p.matcher(json);
        java.util.List<String> blocks = new java.util.ArrayList<>();
        while (m.find()) blocks.add(m.group());
        return blocks.toArray(new String[0]);
    }
}
