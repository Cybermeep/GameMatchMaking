package com.gamematchmaker.steam.api;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class SteamApiClientTest {

    @Test
    void constructor_throwsOnNullApiKey() {
        assertThrows(IllegalArgumentException.class,
            () -> new SteamApiClient(null));
    }

    @Test
    void constructor_throwsOnBlankApiKey() {
        assertThrows(IllegalArgumentException.class,
            () -> new SteamApiClient("  "));
    }

    @Test
    void constructor_acceptsValidApiKey() {
        assertDoesNotThrow(() -> new SteamApiClient("VALID_KEY_1234567890ABCDEF"));
    }

    @Test
    void fetchProfiles_returnsEmptyListForNullInput() throws Exception {
        SteamApiClient client = new SteamApiClient("FAKE_KEY");
        var result = client.fetchProfiles(null);
        assertTrue(result.isEmpty());
    }

    @Test
    void fetchProfiles_returnsEmptyListForEmptyInput() throws Exception {
        SteamApiClient client = new SteamApiClient("FAKE_KEY");
        var result = client.fetchProfiles(java.util.List.of());
        assertTrue(result.isEmpty());
    }
}
