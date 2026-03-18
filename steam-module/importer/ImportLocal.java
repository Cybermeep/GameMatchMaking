package com.gamematchmaker.steam.importer;

import com.gamematchmaker.steam.model.SteamModels.SteamGame;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.logging.*;
import java.util.regex.*;

/*
 * ImportLocal.java

 *   One responsibility: scan the local Steam filesystem to find which games
 *   are physically installed on this machine. It does NOT call the Steam
 *   Web API (ImportSteam's job), does NOT parse JSON (JsonParser's job).
 *   One reason to change: if Valve changes the .acf manifest file format.

 */
public class ImportLocal {

    private static final Logger LOGGER = Logger.getLogger(ImportLocal.class.getName());

    // Common Steam installation paths by OS — used by detectSteamPath()
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


    /** Path to the Steam installation directory (e.g. "C:\Program Files (x86)\Steam"). */
    private final String filePath;

    /** File pattern to look for inside steamapps (always "appmanifest_*.acf"). */
    private final String fileName;

    public ImportLocal(String filePath, String fileName) {
        this.filePath = filePath;
        this.fileName = fileName;
    }

    // FR 3.1.14 — Get locally installed games

    /**
     * Scans all Steam library folders and returns one SteamGame stub
     * per installed game. Each stub has appId and name populated from
     * the .acf manifest file. Playtime and icon URL come from the Web API.
     *
     * Returns an empty list (never null) if Steam is not found or no
     * games are installed.
     */
    public List<SteamGame> getInstalledGames() {
        List<SteamGame> installed = new ArrayList<>();

        for (String libraryRoot : getLibraryFolders()) {
            Path steamappsDir = Paths.get(libraryRoot, "steamapps");
            if (!Files.isDirectory(steamappsDir)) continue;

            try {
                Files.list(steamappsDir)
                     .filter(path -> {
                         String name = path.getFileName().toString();
                         // Match files like "appmanifest_570.acf", "appmanifest_730.acf"
                         return name.startsWith("appmanifest_") && name.endsWith(".acf");
                     })
                     .forEach(manifestFile -> {
                         SteamGame game = parseManifestFile(manifestFile);
                         if (game != null) installed.add(game);
                     });
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Could not read steamapps folder: " + steamappsDir, e);
            }
        }

        LOGGER.info("Found " + installed.size() + " locally installed games.");
        return installed;
    }

    // Library folder discovery

    /**
     * Returns all Steam library folder paths on this machine.
     * The primary install folder (filePath) is always first. Any additional
     * library locations registered in libraryfolders.vdf are appended.
     * Steam allows games to be installed across multiple drives.
     */
    public List<String> getLibraryFolders() {
        List<String> folders = new ArrayList<>();
        folders.add(filePath); // primary folder is always included

        Path libraryFoldersVdf = Paths.get(filePath, "steamapps", "libraryfolders.vdf");
        if (!Files.exists(libraryFoldersVdf)) return folders;

        try {
            String content = Files.readString(libraryFoldersVdf);
            // VDF format: "path"    "D:\\SteamLibrary"
            Matcher m = Pattern.compile("\"path\"\\s+\"([^\"]+)\"").matcher(content);
            while (m.find()) {
                String extra = m.group(1).replace("\\\\", "\\");
                if (!extra.equals(filePath)) folders.add(extra);
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Could not read libraryfolders.vdf", e);
        }

        return folders;
    }

    // Static factory — auto-detect the Steam installation path

    /**
     * Attempts to auto-detect where Steam is installed on this machine.
     * Checks common installation directories for Windows, Mac, and Linux.
     *
     * Returns the path if found, or null if Steam is not installed.
     * Static so it can be called without creating an ImportLocal first.
     */
    public static String detectSteamPath() {
        String osName = System.getProperty("os.name", "").toLowerCase();

        List<String> candidates;
        if (osName.contains("win"))       candidates = WINDOWS_PATHS;
        else if (osName.contains("mac"))  candidates = MAC_PATHS;
        else                              candidates = LINUX_PATHS;

        for (String path : candidates) {
            if (Files.isDirectory(Paths.get(path))) {
                LOGGER.info("Found Steam at: " + path);
                return path;
            }
        }

        LOGGER.warning("Steam not found in common locations for OS: " + osName);
        return null;
    }

    // Private helpers (Encapsulation — hidden from callers)

    // Reads one appmanifest_APPID.acf file and returns a SteamGame stub.
    // Returns null if the file can't be read or has no appid.
    private SteamGame parseManifestFile(Path path) {
        try {
            String content = Files.readString(path);
            String appId   = vdfField(content, "appid");
            String name    = vdfField(content, "name");
            if (appId == null) return null;
            // Playtime and icon URL are not in the manifest — they come from the Web API
            return new SteamGame(appId, name != null ? name : "Unknown", null, 0, 0);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Could not parse manifest: " + path, e);
            return null;
        }
    }

    // Extracts a value from Valve's VDF (KeyValue) format.
    // VDF looks like:  "fieldName"    "fieldValue"
    private String vdfField(String content, String fieldName) {
        Matcher m = Pattern.compile(
            "\"" + Pattern.quote(fieldName) + "\"\\s+\"([^\"]+)\""
        ).matcher(content);
        return m.find() ? m.group(1) : null;
    }

    // Getters — match the SRS class diagram fields
    public String getFilePath() { return filePath; }
    public String getFileName() { return fileName; }
}
