package com.gamematchmaker.steam.api;

import org.junit.jupiter.api.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/*
 * SteamApiClientTest.java
 *
 *   This class only tests SteamApiClient. Every test focuses on either the
 *   constructor validation (fail-fast) or the edge cases that don't need a
 *   network — specifically the null/empty input short-circuits in fetchProfiles.

 */
class SteamApiClientTest {

    // Constructor validation tests

    @Test
    void constructor_throwsOnNullApiKey() {
        // Should throw immediately so we find the misconfiguration at startup
        assertThrows(IllegalArgumentException.class,
            () -> new SteamApiClient(null));
    }

    @Test
    void constructor_throwsOnBlankApiKey() {
        assertThrows(IllegalArgumentException.class,
            () -> new SteamApiClient("  "));
    }

    @Test
    void constructor_succeedsWithValidKey() {
        // A non-blank key should be accepted (format is not validated here)
        assertDoesNotThrow(() -> new SteamApiClient("VALID_KEY_1234567890ABCDEF"));
    }

    // fetchProfiles() edge cases that short-circuit before any HTTP call

    @Test
    void fetchProfiles_returnsEmptyListForNullInput() throws Exception {
        SteamApiClient client = new SteamApiClient("FAKE_KEY");
        // null input should return an empty list, not throw NullPointerException
        List<?> result = client.fetchProfiles(null);
        assertTrue(result.isEmpty(),
            "null input should return empty list, never crash");
    }

    @Test
    void fetchProfiles_returnsEmptyListForEmptyInput() throws Exception {
        SteamApiClient client = new SteamApiClient("FAKE_KEY");
        // Empty list input should also return an empty list
        List<?> result = client.fetchProfiles(List.of());
        assertTrue(result.isEmpty(),
            "empty list input should return empty list");
    }
}
