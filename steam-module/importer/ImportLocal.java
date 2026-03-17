package com.gamematchmaker.steam.importer;

import com.gamematchmaker.steam.model.SteamModels.SteamGame;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.logging.*;
import java.util.regex.*;

/**
 * Uses filePath and fileName for files stored on the local machine to
 * import game files and verify ownership.
 *
 * Directly corresponds to the "ImportLocal" class in the SRS Class Diagram
 * (Section 4.2.2.1).
 *
 * Reads Steam's {@code appmanifest_APPID.acf} files from the local Steam
 * installation to determine which games are currently installed on the machine.
 * Also parses {@code libraryfolders.vdf} to discover additional library locations.
 *
 * Supports FR 3.1.14 (Resynchronize Installed Games).
 *
 * Note: In production deployments where the server and client are separate
 * machines, the browser-side client submits installed AppIDs via the API
 * instead of using this class directly.
 */
public class ImportLocal {

    private static final Logger LOGGER = Logger.getLogger(ImportLocal.class.getName());

    // Default Steam installation paths per OS (per SRS class definition)
    private static final List<String> WINDOWS_PATHS = List.of(
        "C:\\Program Files (x86)\\Steam",
        "C:\\Program Files\\Steam"
    );
    private static final List<String> LINUX_PATHS = List.of(
        System.getProperty("user.home") + "/.steam/steam",
        System.getProperty("user.home") + "/.local/share/Steam"
    );
    private static final List<String> MAC_PATHS = List.of(
        System.getProperty("user.home") + "/Library/Application Support/Steam"
    );

    /** Base path to the Steam installation directory (per SRS class definition). */
    private final String filePath;

    /** Target filename pattern, e.g. "appmanifest_*.acf" (per SRS class definition). */
    private final String fileName;

    public ImportLocal(String filePath, String fileName) {
        this.filePath = filePath;
        this.fileName = fileName;
    }

    // =========================================================================
    // FR 3.1.14 — Get locally installed games
    // =========================================================================

    /**
     * Scans all Steam library folders for {@code appmanifest_*.acf} files
     * and returns one {@link SteamGame} stub per installed game.
     *
     * Each stub has {@code appId} and {@code name} populated from the ACF file.
     * All other fields (playtime etc.) come from the Steam Web API.
     *
     * @return List of installed games. Empty if Steam is not found or no games
     *         are installed.
     */
    public List<SteamGame> getInstalledGames() {
        List<SteamGame> installed = new ArrayList<>();

        for (String libraryRoot : getLibraryFolders()) {
            Path steamapps = Paths.get(libraryRoot, "steamapps");
            if (!Files.isDirectory(steamapps)) continue;

            try {
                Files.list(steamapps)
                     .filter(p -> {
                         String name = p.getFileName().toString();
                         return name.startsWith("appmanifest_") && name.endsWith(".acf");
                     })
                     .forEach(manifest -> {
                         SteamGame g = parseManifest(manifest);
                         if (g != null) installed.add(g);
                     });
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Error scanning: " + steamapps, e);
            }
        }

        LOGGER.info("Found " + installed.size() + " locally installed Steam games.");
        return installed;
    }

    // =========================================================================
    // Library folder discovery
    // =========================================================================

    /**
     * Returns all Steam library folder paths configured on this machine,
     * starting with the primary install path and including any additional
     * paths defined in {@code libraryfolders.vdf}.
     */
    public List<String> getLibraryFolders() {
        List<String> folders = new ArrayList<>();
        folders.add(filePath);  // primary library is always first

        Path vdf = Paths.get(filePath, "steamapps", "libraryfolders.vdf");
        if (!Files.exists(vdf)) return folders;

        try {
            String content = Files.readString(vdf);
            // VDF format: "path"  "D:\\SteamLibrary"
            Matcher m = Pattern.compile("\"path\"\\s+\"([^\"]+)\"").matcher(content);
            while (m.find()) {
                String extra = m.group(1).replace("\\\\", "\\");
                if (!extra.equals(filePath)) folders.add(extra);
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Could not read libraryfolders.vdf: " + vdf, e);
        }

        return folders;
    }

    // =========================================================================
    // Static factory — auto-detect Steam path
    // =========================================================================

    /**
     * Attempts to auto-detect the Steam installation directory on the current OS.
     *
     * @return The first valid Steam path found, or {@code null} if Steam is
     *         not installed or could not be located.
     */
    public static String detectSteamPath() {
        String os = System.getProperty("os.name", "").toLowerCase();
        List<String> candidates;

        if (os.contains("win"))       candidates = WINDOWS_PATHS;
        else if (os.contains("mac"))  candidates = MAC_PATHS;
        else                          candidates = LINUX_PATHS;

        for (String path : candidates) {
            if (Files.isDirectory(Paths.get(path))) {
                LOGGER.info("Auto-detected Steam path: " + path);
                return path;
            }
        }

        LOGGER.warning("Could not auto-detect Steam installation path on: " + os);
        return null;
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Parses an {@code appmanifest_APPID.acf} file into a minimal SteamGame stub.
     * Returns {@code null} if the file cannot be read or lacks an appid.
     */
    private SteamGame parseManifest(Path path) {
        try {
            String content = Files.readString(path);
            String appId   = vdfValue(content, "appid");
            String name    = vdfValue(content, "name");
            if (appId == null) return null;
            // SteamGame: appId, name, iconUrl=null, playtimeForever=0, playtime2Weeks=0
            return new SteamGame(appId, name != null ? name : "Unknown", null, 0, 0);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error reading manifest: " + path, e);
            return null;
        }
    }

    /**
     * Extracts a value from Valve's KeyValue (VDF) text format.
     * Handles:  "key"    "value"
     */
    private String vdfValue(String content, String key) {
        Matcher m = Pattern.compile(
            "\"" + Pattern.quote(key) + "\"\\s+\"([^\"]+)\""
        ).matcher(content);
        return m.find() ? m.group(1) : null;
    }

    // ── Getters (per SRS class definition) ───────────────────────────────────

    public String getFilePath() { return filePath; }
    public String getFileName() { return fileName; }
}
