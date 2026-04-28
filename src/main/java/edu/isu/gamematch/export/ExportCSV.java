package edu.isu.gamematch.export;

import edu.isu.gamematch.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Concrete export class that writes application data to a CSV file on disk
 *
 * The CSV format produced looks like this:
 *   userId,steamId,profileName,gameId,gameName,genre,playtimeMinutes,steamAppUrl
 *   42,76561198012345678,CoolGamer,101,Portal 2,Puzzle,320,https://store.steampowered.com/app/620
 *   42,76561198012345678,CoolGamer,204,Dota 2,MOBA,18400,https://store.steampowered.com/app/570
 * 
 *
 * Example usage:
 * 
 *   ExportCSV exporter = new ExportCSV("/home/user/exports", 5 * 1024 * 1024);
 *   ExportResult result = exporter.exportData(userObject);
 *   if (result.isSuccess()) {
 *       String path = result.getOutputFilePath();
 *   }
 * 
 */
@Component
public class ExportCSV extends ExportData {

    private static final Logger logger = LoggerFactory.getLogger(ExportCSV.class);


    /** Column headers written on the first line of every CSV file. */
    private static final String CSV_HEADER =
            "userId,steamId,profileName,gameId,gameName,genre,playtimeMinutes,steamAppUrl";

    /** Character used to separate fields within a row. */
    private static final char DELIMITER = ',';

    /** Character used to wrap field values that may contain a comma or newline. */
    private static final char QUOTE = '"';

    /** Root directory where exported CSV files will be written. */
    private String outputDirectory;

    /**
     * Maximum size in bytes that a single export file is allowed to reach
     * Prevents runaway writes for users with huge game libraries
     * Default: 5 MB
     */
    private long maxFileSizeBytes;

    /**
     * Whether to append a timestamp to the filename so repeated exports
     * don't overwrite each other
     * Default: true
     */
    private boolean appendTimestamp;

    /**
     * File encoding for the output file
     * UTF-8 is used so that non-ASCII game names (e.g., Japanese titles) round-trip correctly
     */
    private static final String FILE_ENCODING = StandardCharsets.UTF_8.name();

    // Constructors
    /**
     * Creates an ExportCSV instance using the project's default exports directory
     * Max file size defaults to 5 MB and timestamps are appended
     */
    public ExportCSV() {
        this.outputDirectory = System.getProperty("user.dir") + File.separator + "exports";
        this.maxFileSizeBytes = 5L * 1024 * 1024; // 5 MB
        this.appendTimestamp = true;
        ensureOutputDirectoryExists();
    }

    /**
     * Creates an ExportCSV instance with explicit configuration
     *
     * @param outputDirectory  absolute path of the directory to write files into
     * @param maxFileSizeBytes maximum bytes allowed per export file
     */
    public ExportCSV(String outputDirectory, long maxFileSizeBytes) {
        this();
        this.outputDirectory = outputDirectory;
        this.maxFileSizeBytes = maxFileSizeBytes;
        ensureOutputDirectoryExists();
    }

    // ExportData lifecycle validation 
    /**
     * {@inheritDoc}
     *
     * Registers rules specific to CSV export:
     * 
     *   The user being exported must not be null
     *   The user must have at least one game in their library
     *   The output directory must be writable
     *   There must be enough disk space for the estimated file size
     */
    @Override
    protected void initializeValidationRules() {
        exportValidationRules.add(new ExportValidationRule(
                "USER_NOT_NULL",
                "The User object passed to the exporter must not be null"));
        exportValidationRules.add(new ExportValidationRule(
                "HAS_GAMES",
                "User must have at least one game in their library to export"));
        exportValidationRules.add(new ExportValidationRule(
                "DIRECTORY_WRITABLE",
                "Output directory must exist and be writable: " + outputDirectory));
        exportValidationRules.add(new ExportValidationRule(
                "SUFFICIENT_DISK_SPACE",
                "Not enough disk space to write the export file"));
    }

    // ExportData template method steps
    /**
     * Validates that the export can proceed before any file I/O is attempted
     *
     * Checks are run in order; the first failure short-circuits the rest
     * so the log message is as specific as possible
     *
     * @param exportTarget the {@link User} object to export; may be any Object
     * because ExportData is generic, but we expect a User here
     * @return true if all validation rules pass; false otherwise
     */
    @Override
    protected boolean validateExportTarget(Object exportTarget) {
        logger.debug("Validating export target for CSV export");

        // target must be a non-null User
        if (exportTarget == null || !(exportTarget instanceof User)) {
            logger.error("Export target is null or not a User object");
            return false;
        }

        User user = (User) exportTarget;

        // user must have games to export
        if (user.getAchievementData() == null) {
            // We check for a game library via the UserProfile or achievements;
            // a completely empty account has nothing to write to CSV
            logger.error("User {} has no data to export", user.getUserID());
            return false;
        }

        // output directory must be writable
        if (!validateDirectoryWritable(outputDirectory)) {
            logger.error("Output directory is not writable: {}", outputDirectory);
            return false;
        }

        // rough disk-space check — estimate of 1 KB per game, 10x safety margin
        long estimatedBytes = 1024L * 10;
        if (!validateDiskSpace(estimatedBytes)) {
            logger.error("Insufficient disk space for export");
            return false;
        }

        logger.info("Export target validation passed for user ID: {}", user.getUserID());
        return true;
    }

    /**
     * Converts the User domain object into a flat list of row maps ready to
     * be written as CSV lines
     *
     * Each row represents one game in the user's library. All the fields
     * that live on both the User and the Game are flattened into a single row
     * so the CSV is self-contained the recipient doesn't need to join tables
     * to make sense of it
     *
     * @param exportTarget the User to prepare data for; already validated
     * @return a PreparedExportData object holding the row list and row count
     */
    @Override
    protected PreparedExportData prepareData(Object exportTarget) {
        logger.debug("Preparing CSV data for export");
        User user = (User) exportTarget;

        List<Map<String, String>> rows = new ArrayList<>();

        // Pull the profile name from the associated UserProfile if one exists
        String profileName = "";
        if (user.getUserProfile() != null) {
            profileName = sanitizeField(user.getUserProfile().getProfileName());
        }

        // Build one row per achievement/game entry
        // GameAchievement links User to Game, so we use it to walk the game list
        List<GameAchievement> achievements = user.getAchievementData();
        if (achievements != null && !achievements.isEmpty()) {
            for (GameAchievement achievement : achievements) {
                Game game = achievement.getGame();
                if (game == null) {
                    logger.warn("Skipping achievement with null Game reference for user {}",
                            user.getUserID());
                    continue;
                }

                Map<String, String> row = new LinkedHashMap<>();
                row.put("userId",           String.valueOf(user.getUserID()));
                row.put("steamId",          String.valueOf(user.getSteamID()));
                row.put("profileName",      profileName);
                row.put("gameId",           String.valueOf(game.getGameID()));
                row.put("gameName",         sanitizeField(game.getGameName()));
                row.put("genre",            sanitizeField(game.getGenre() != null ? game.getGenre() : ""));
                row.put("playtimeMinutes",  String.valueOf(game.getPlaytime()));
                row.put("steamAppUrl",      sanitizeField(game.getSteamAppURL() != null ? game.getSteamAppURL() : ""));

                rows.add(row);
            }
        }

        logger.info("Prepared {} rows for CSV export (user ID: {})", rows.size(), user.getUserID());
        return new PreparedExportData(rows, rows.size());
    }

    /**
     * Writes the prepared rows to a CSV file in the output directory
     *
     * The filename follows the pattern:
     * {@code export_<userId>_<timestamp>.csv}
     * for example: {@code export_42_2024-11-15T14-32-00.csv}
     *
     * @param preparedData the rows returned by {@link #prepareData(Object)}
     * @return the absolute path of the file that was written, or null on failure
     */
    @Override
    protected String writeExport(PreparedExportData preparedData) {
        String outputFilePath = buildOutputFilePath(preparedData);
        logger.debug("Writing CSV export to: {}", outputFilePath);

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(
                        new FileOutputStream(outputFilePath), FILE_ENCODING))) {

            // Write the header row first
            writer.write(CSV_HEADER);
            writer.newLine();

            // Write one data row for each prepared record
            @SuppressWarnings("unchecked")
            List<Map<String, String>> rows = (List<Map<String, String>>) preparedData.getRecords();
            int rowsWritten = 0;

            for (Map<String, String> row : rows) {
                String csvLine = buildCsvLine(row);

                // Safety check — stop writing if file would exceed size limit
                long currentSize = new File(outputFilePath).length();
                if (currentSize + csvLine.length() > maxFileSizeBytes) {
                    logger.warn("Reached max file size limit ({} bytes) after {} rows",
                            maxFileSizeBytes, rowsWritten);
                    break;
                }

                writer.write(csvLine);
                writer.newLine();
                rowsWritten++;
            }

            logger.info("Successfully wrote {} rows to {}", rowsWritten, outputFilePath);
            return outputFilePath;

        } catch (IOException e) {
            logger.error("Failed to write CSV file: {}", outputFilePath, e);
            return null;
        }
    }

    /**
     * Performs cleanup tasks after a successful export
     *
     * Currently:
     * 
     *   Verifies the output file was actually created on disk
     *   Logs the final file size for auditing purposes
     *   Could be extended to: send an email notification, archive old exports, update an audit log, etc
     *
     * @param outputFilePath the path returned by {@link #writeExport(PreparedExportData)}
     */
    @Override
    protected void postExportProcessing(String outputFilePath) {
        logger.debug("Running post-export processing for: {}", outputFilePath);

        File outputFile = new File(outputFilePath);
        if (outputFile.exists()) {
            logger.info("Export file confirmed on disk: {} ({} bytes)",
                    outputFilePath, outputFile.length());
        } else {
            logger.warn("Post-export check: file not found at expected path: {}", outputFilePath);
        }

        exportStatus.setPostProcessed(true);

        // TODO: notify the user that their export is ready for download
        // TODO: write an entry to the audit log table
    }

    // Private helper methods
    /**
     * Builds a single CSV line from a row map, quoting any field that contains
     * a comma, double-quote, or newline character.
     *
     * The field order follows the {@link #CSV_HEADER} constant so columns
     * always line up correctly.
     *
     * @param row a map of column name → value for one game record
     * @return a properly formatted CSV line string (no trailing newline)
     */
    private String buildCsvLine(Map<String, String> row) {
        // Pull values in header order so columns never get scrambled
        String[] fieldOrder = {
            "userId", "steamId", "profileName",
            "gameId", "gameName", "genre",
            "playtimeMinutes", "steamAppUrl"
        };

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fieldOrder.length; i++) {
            String value = row.getOrDefault(fieldOrder[i], "");
            sb.append(quoteIfNecessary(value));
            if (i < fieldOrder.length - 1) {
                sb.append(DELIMITER);
            }
        }
        return sb.toString();
    }

    /**
     * Wraps a field value in double-quotes if it contains a comma, quote, or
     * newline — all of which would break CSV parsing if left bare.
     * Existing double-quotes inside the value are escaped by doubling them
     *
     * @param value the raw field value
     * @return the value as-is if safe, or quoted + escaped if needed
     */
    private String quoteIfNecessary(String value) {
        if (value == null) return "";

        boolean needsQuoting = value.indexOf(DELIMITER) >= 0
                || value.indexOf(QUOTE) >= 0
                || value.indexOf('\n') >= 0
                || value.indexOf('\r') >= 0;

        if (needsQuoting) {
            // Double any existing quotes, then wrap the whole thing
            String escaped = value.replace("\"", "\"\"");
            return QUOTE + escaped + QUOTE;
        }

        return value;
    }

    /**
     * Removes characters that are problematic in CSV files — specifically
     * control characters (except tab) that don't belong in text fields and
     * could confuse downstream parsers
     *
     * @param value the raw string from a domain object
     * @return the sanitized string, or an empty string if value was null
     */
    private String sanitizeField(String value) {
        if (value == null) return "";
        // Strip ASCII control characters (0x00–0x1F) except tab (0x09)
        return value.replaceAll("[\\x00-\\x08\\x0B-\\x1F]", "");
    }

    /**
     * Constructs the full output file path.
     * Appends a timestamp if {@link #appendTimestamp} is true
     *
     * @param preparedData used to grab the user ID for the filename
     * @return the absolute path string for the output file
     */
    private String buildOutputFilePath(PreparedExportData preparedData) {
        String timestamp = appendTimestamp
                ? "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss"))
                : "";
        String fileName = "export_" + preparedData.getExportId() + timestamp + ".csv";
        return outputDirectory + File.separator + fileName;
    }

    /**
     * Checks whether the output directory exists and is writable
     *
     * @param directoryPath the path to check
     * @return true if the directory is usable; false otherwise
     */
    private boolean validateDirectoryWritable(String directoryPath) {
        Path path = Paths.get(directoryPath);
        return Files.exists(path) && Files.isDirectory(path) && Files.isWritable(path);
    }

    /**
     * Checks whether the filesystem has at least {@code requiredBytes} of free space
     * in the output directory's partition
     *
     * @param requiredBytes minimum number of bytes needed
     * @return true if there is enough space; false if the check fails or space is tight
     */
    private boolean validateDiskSpace(long requiredBytes) {
        try {
            long freeSpace = new File(outputDirectory).getFreeSpace();
            return freeSpace >= requiredBytes;
        } catch (Exception e) {
            logger.warn("Could not check disk space — proceeding anyway: {}", e.getMessage());
            return true; // If we can't check, don't block the export
        }
    }

    /**
     * Creates the output directory (and any missing parent directories) if it
     * doesn't already exist. Called from the constructor so the directory is
     * always ready before the first export attempt
     */
    private void ensureOutputDirectoryExists() {
        try {
            Files.createDirectories(Paths.get(outputDirectory));
            logger.debug("Output directory ready: {}", outputDirectory);
        } catch (IOException e) {
            logger.warn("Could not create output directory: {} — {}",
                    outputDirectory, e.getMessage());
        }
    }

    // Setters for optional configuration
    /**
     * Overrides the output directory after construction
     * The new directory will be created if it doesn't exist
     *
     * @param outputDirectory absolute path to the desired output folder
     */
    public void setOutputDirectory(String outputDirectory) {
        this.outputDirectory = outputDirectory;
        ensureOutputDirectoryExists();
    }

    /**
     * Overrides the maximum file size limit after construction
     *
     * @param maxFileSizeBytes the new limit in bytes
     */
    public void setMaxFileSizeBytes(long maxFileSizeBytes) {
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    /**
     * Controls whether exported filenames include a timestamp suffix
     *
     * @param appendTimestamp true to include timestamps; false to use a fixed name
     */
    public void setAppendTimestamp(boolean appendTimestamp) {
        this.appendTimestamp = appendTimestamp;
    }

    // Inner helper classes 
    /**
     * A single validation rule checked before an export begins.
     * Stored as a list in ExportData so failure messages are consistent
     * across all export types.
     */
    public static class ExportValidationRule {
        private final String ruleId;
        private final String description;

        public ExportValidationRule(String ruleId, String description) {
            this.ruleId = ruleId;
            this.description = description;
        }

        public String getRuleId()      { return ruleId; }
        public String getDescription() { return description; }
    }

    /**
     * Holds the list of rows (or other records) ready to be written to disk,
     * along with metadata about the export like a record count and identifier.
     */
    public static class PreparedExportData {

        private final List<?> records;
        private final int recordCount;
        // Export ID is used to build a unique filename; in a further implementation this would be the user ID or a UUID from a job queue.
        private String exportId = "unknown";

        public PreparedExportData(List<?> records, int recordCount) {
            this.records = records;
            this.recordCount = recordCount;
        }

        public List<?> getRecords()          { return records; }
        public int     getRecordCount()      { return recordCount; }
        public String  getExportId()         { return exportId; }
        public void    setExportId(String id){ this.exportId = id; }
    }
}
