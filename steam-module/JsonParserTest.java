package com.gamematchmaker.steam.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class JsonParserTest {

    private static final String SAMPLE = """
        {
          "steamid": "76561198000000001",
          "personaname": "TestPlayer",
          "communityvisibilitystate": 3,
          "lastlogoff": 1700000000,
          "achieved": 1
        }
        """;

    @Test
    void field_extractsStringValue() {
        assertEquals("76561198000000001", JsonParser.field(SAMPLE, "steamid"));
    }

    @Test
    void field_extractsPersonaName() {
        assertEquals("TestPlayer", JsonParser.field(SAMPLE, "personaname"));
    }

    @Test
    void field_returnsNullForMissingKey() {
        assertNull(JsonParser.field(SAMPLE, "nonexistent_key"));
    }

    @Test
    void field_returnsNullForNullJson() {
        assertNull(JsonParser.field(null, "steamid"));
    }

    @Test
    void intField_parsesInteger() {
        assertEquals(3, JsonParser.intField(SAMPLE, "communityvisibilitystate"));
    }

    @Test
    void intField_returnsZeroForMissingKey() {
        assertEquals(0, JsonParser.intField(SAMPLE, "missing"));
    }

    @Test
    void longField_parsesLong() {
        assertEquals(1700000000L, JsonParser.longField(SAMPLE, "lastlogoff"));
    }

    @Test
    void boolField_returnsTrueForOne() {
        assertTrue(JsonParser.boolField(SAMPLE, "achieved"));
    }

    @Test
    void boolField_returnsFalseForMissingKey() {
        assertFalse(JsonParser.boolField(SAMPLE, "not_present"));
    }

    @Test
    void objectsContaining_findsMatchingBlocks() {
        String json = """
            {"appid": "570", "name": "Dota 2", "playtime_forever": 120}
            {"appid": "730", "name": "CS2", "playtime_forever": 60}
            """;
        String[] blocks = JsonParser.objectsContaining(json, "appid");
        assertEquals(2, blocks.length);
    }

    @Test
    void objectsContaining_returnsEmptyArrayForNoMatches() {
        String[] blocks = JsonParser.objectsContaining("{}", "appid");
        assertEquals(0, blocks.length);
    }

    @Test
    void objectsContaining_returnsEmptyArrayForNullJson() {
        String[] blocks = JsonParser.objectsContaining(null, "appid");
        assertEquals(0, blocks.length);
    }
}
