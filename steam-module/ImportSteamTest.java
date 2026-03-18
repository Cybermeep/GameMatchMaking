package com.gamematchmaker.steam.importer;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class ImportSteamTest {

    // ── ImportSteam constructor ───────────────────────────────────────────────

    @Test
    void constructor_throwsOnNullSteamId() {
        assertThrows(IllegalArgumentException.class,
            () -> new ImportSteam(null, "APIKEY"));
    }

    @Test
    void constructor_throwsOnBlankSteamId() {
        assertThrows(IllegalArgumentException.class,
            () -> new ImportSteam("  ", "APIKEY"));
    }

    @Test
    void constructor_throwsOnNullApiKey() {
        assertThrows(IllegalArgumentException.class,
            () -> new ImportSteam("76561198000000001", null));
    }

    @Test
    void constructor_throwsOnBlankApiKey() {
        assertThrows(IllegalArgumentException.class,
            () -> new ImportSteam("76561198000000001", "  "));
    }

    @Test
    void getters_returnExpectedValues() {
        ImportSteam importer = new ImportSteam("76561198000000001", "MYAPIKEY");
        assertEquals("76561198000000001", importer.getSteamId());
        assertEquals("MYAPIKEY",          importer.getApiKey());
        assertNull(importer.getAppId(),   "appId should be null before fetchAchievements is called");
    }

    // ── ImportLocal ───────────────────────────────────────────────────────────

    @Test
    void importLocal_getters_returnExpectedValues() {
        ImportLocal local = new ImportLocal("/fake/path", "appmanifest_*.acf");
        assertEquals("/fake/path",        local.getFilePath());
        assertEquals("appmanifest_*.acf", local.getFileName());
    }

    @Test
    void importLocal_getInstalledGames_returnsEmptyForNonExistentPath() {
        ImportLocal local = new ImportLocal("/definitely/does/not/exist", "appmanifest_*.acf");
        var games = local.getInstalledGames();
        assertNotNull(games);
        assertTrue(games.isEmpty(), "Should return empty list when Steam path does not exist.");
    }

    @Test
    void importLocal_getLibraryFolders_includesPrimaryPath() {
        ImportLocal local = new ImportLocal("/some/steam/path", "appmanifest_*.acf");
        var folders = local.getLibraryFolders();
        assertFalse(folders.isEmpty());
        assertEquals("/some/steam/path", folders.get(0), "Primary path must always be first.");
    }

    @Test
    void detectSteamPath_doesNotThrow() {
        // Should return null gracefully if Steam is not installed in test environment
        assertDoesNotThrow(ImportLocal::detectSteamPath);
    }
}
