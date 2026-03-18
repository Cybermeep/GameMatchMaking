package com.gamematchmaker.steam.importer;

import org.junit.jupiter.api.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/*
 * ImportSteamTest.java

 *   Tests ImportSteam and ImportLocal only. Constructor validation, getter
 *   correctness, and filesystem behaviour with non-existent paths are the
 *   testable surface without requiring a live Steam installation.

 */
class ImportSteamTest {

    // ImportSteam constructor tests

    @Test
    void importSteam_throwsOnNullSteamId() {
        assertThrows(IllegalArgumentException.class,
            () -> new ImportSteam(null, "APIKEY"));
    }

    @Test
    void importSteam_throwsOnBlankSteamId() {
        assertThrows(IllegalArgumentException.class,
            () -> new ImportSteam("   ", "APIKEY"));
    }

    @Test
    void importSteam_throwsOnNullApiKey() {
        assertThrows(IllegalArgumentException.class,
            () -> new ImportSteam("76561198000000001", null));
    }

    @Test
    void importSteam_throwsOnBlankApiKey() {
        assertThrows(IllegalArgumentException.class,
            () -> new ImportSteam("76561198000000001", "  "));
    }

    // ImportSteam getter tests

    @Test
    void importSteam_gettersReturnExpectedValues() {
        ImportSteam importer = new ImportSteam("76561198000000001", "MYAPIKEY");

        assertEquals("76561198000000001", importer.getSteamId());
        assertEquals("MYAPIKEY",          importer.getApiKey());
        // appId should be null until fetchAchievements() is called (per SRS diagram)
        assertNull(importer.getAppId(),
            "appId must be null before fetchAchievements() is called");
    }

    // ImportLocal constructor and getter tests

    @Test
    void importLocal_gettersReturnExpectedValues() {
        ImportLocal local = new ImportLocal("/fake/path", "appmanifest_*.acf");
        assertEquals("/fake/path",        local.getFilePath());
        assertEquals("appmanifest_*.acf", local.getFileName());
    }

    // ImportLocal behaviour tests (no real Steam install needed)

    @Test
    void importLocal_returnsEmptyListForNonExistentPath() {
        // When Steam is not installed at the given path, expect empty list — not a crash
        ImportLocal local = new ImportLocal("/definitely/does/not/exist", "appmanifest_*.acf");
        List<?> games = local.getInstalledGames();

        assertNotNull(games, "getInstalledGames() should never return null");
        assertTrue(games.isEmpty(),
            "Should return empty list when path does not exist — not throw an exception");
    }

    @Test
    void importLocal_libraryFoldersAlwaysContainsPrimaryPath() {
        ImportLocal local = new ImportLocal("/some/steam/path", "appmanifest_*.acf");
        List<String> folders = local.getLibraryFolders();

        assertFalse(folders.isEmpty(), "Library folders list should never be empty");
        assertEquals("/some/steam/path", folders.get(0),
            "The primary path must always be the first entry in getLibraryFolders()");
    }

    @Test
    void importLocal_detectSteamPath_doesNotThrow() {
        // Should return null gracefully if Steam is not installed in this test environment
        assertDoesNotThrow(() -> {
            String path = ImportLocal.detectSteamPath();
            // path is either a valid directory string or null — both are acceptable
        }, "detectSteamPath() must not throw, even if Steam is not installed");
    }
}
