package com.gamematchmaker.steam.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/*
 * JsonParserTest.java
 *
 *
 *
 *   This class only tests JsonParser. One class under test, one reason for
 *   this test class to change: if JsonParser's method signatures change.
 *
 *
 */
class JsonParserTest {

    // A sample JSON object that looks like a real Steam API player summary response
    private static final String SAMPLE = """
            {
              "steamid": "76561198000000001",
              "personaname": "TestPlayer",
              "communityvisibilitystate": 3,
              "lastlogoff": 1700000000,
              "achieved": 1
            }
            """;

    // field() tests — extracts String values

    @Test
    void field_extractsSteamId() {
        assertEquals("76561198000000001", JsonParser.field(SAMPLE, "steamid"));
    }

    @Test
    void field_extractsPersonaName() {
        assertEquals("TestPlayer", JsonParser.field(SAMPLE, "personaname"));
    }

    @Test
    void field_returnsNullForMissingKey() {
        // A key that isn't in the JSON should produce null, not an exception
        assertNull(JsonParser.field(SAMPLE, "nonexistent_field"),
            "Missing field should return null");
    }

    @Test
    void field_returnsNullForNullJson() {
        // Null input should never throw NullPointerException
        assertNull(JsonParser.field(null, "steamid"),
            "Null JSON input should return null");
    }

    // intField() tests — extracts int values

    @Test
    void intField_parsesVisibilityState() {
        assertEquals(3, JsonParser.intField(SAMPLE, "communityvisibilitystate"),
            "communityvisibilitystate should parse to int 3");
    }

    @Test
    void intField_returnsZeroForMissingKey() {
        assertEquals(0, JsonParser.intField(SAMPLE, "missing_field"),
            "Missing int field should return 0, not crash");
    }

    // longField() tests — for Unix timestamps and 64-bit SteamIDs

    @Test
    void longField_parsesTimestamp() {
        assertEquals(1700000000L, JsonParser.longField(SAMPLE, "lastlogoff"),
            "lastlogoff should parse to the correct long value");
    }

    // boolField() tests — Steam uses 1/0 for booleans

    @Test
    void boolField_returnsTrueForValueOne() {
        // "achieved": 1 should be treated as true
        assertTrue(JsonParser.boolField(SAMPLE, "achieved"),
            "Value '1' should be treated as boolean true");
    }

    @Test
    void boolField_returnsFalseForMissingField() {
        assertFalse(JsonParser.boolField(SAMPLE, "not_present"),
            "Missing field should return false");
    }

    // objectsContaining() tests — finds JSON object blocks in a larger string

    @Test
    void objectsContaining_findsTwoGameBlocks() {
        String gamesResponse = """
                {"appid": "570", "name": "Dota 2", "playtime_forever": 120}
                {"appid": "730", "name": "CS2",    "playtime_forever": 60}
                """;
        String[] blocks = JsonParser.objectsContaining(gamesResponse, "appid");
        assertEquals(2, blocks.length,
            "Should find exactly 2 game objects in the response");
    }

    @Test
    void objectsContaining_returnsEmptyArrayWhenNoMatches() {
        String[] blocks = JsonParser.objectsContaining("{\"name\": \"test\"}", "appid");
        assertEquals(0, blocks.length,
            "No matching objects should return empty array, not null");
    }

    @Test
    void objectsContaining_returnsEmptyArrayForNullInput() {
        // Should never throw NullPointerException
        String[] blocks = JsonParser.objectsContaining(null, "appid");
        assertEquals(0, blocks.length,
            "Null input should return empty array");
    }

    @Test
    void objectsContaining_integrationWithField_extractsGameName() {
        // Integration test: objectsContaining + field() used together
        // mirrors how SteamApiClient actually uses them
        String gameBlock = "{\"appid\": \"570\", \"name\": \"Dota 2\"}";
        String[] blocks = JsonParser.objectsContaining(gameBlock, "appid");

        assertEquals(1, blocks.length);
        assertEquals("Dota 2", JsonParser.field(blocks[0], "name"),
            "field() on a block from objectsContaining() should extract correctly");
    }
}
