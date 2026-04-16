package edu.isu.gamematch.export;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Abstract base class for all data export operations in the GameMatchMaker system
 *
 * This class is the export-side mirror of {@code ImportData}. Where ImportData defines the workflow for pulling data in (validate -> extract -> transform -> load)
 * ExportData defines the workflow for pushing data out:
 * 
 *   — confirm the target object and the output destination are ready
 *   — flatten domain objects into a format-neutral row/record structure
 *   — serialize and write to the destination (file, API, etc.)
 *   — confirm the write, send notifications, update audit logs
 * 
 
 *
 * Concrete subclasses implement the format-specific details for each step
 * Currently the only subclass is {@link ExportCSV}, but the pattern supports
 * future formats like JSON export, PDF report generation, or an API push
 *
 * This class uses the Template Method design patter, the overall algorithm
 * is fixed here in {@link #exportData(Object)}, and subclasses override the individual steps
 */
public abstract class ExportData {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    /** Runtime status of the current (or most recent) export operation. */
    protected ExportStatus exportStatus;

    /** Validation rules registered by the subclass via {@link #initializeValidationRules()}. */
    protected List<ExportCSV.ExportValidationRule> exportValidationRules;

    public ExportData() {
        this.exportStatus = new ExportStatus();
        this.exportValidationRules = new ArrayList<>();
        initializeValidationRules();
    }

    // Template method, the fixed export workflow

    /**
     * Exports the given domain object to the configured destination
     *
     * The workflow is always: validate, prepare, write, post-process
     * Subclasses control the details of each step but not this order
     *
     * @param exportTarget the domain object to export (e.g., a {@code User})
     * @return an {@link ExportResult} describing success/failure and the output location
     */
    public final ExportResult exportData(Object exportTarget) {
        logger.info("Starting export operation");
        exportStatus.setStartTime(LocalDateTime.now());

        try {
            // make sure we have valid input and a writable destination
            if (!validateExportTarget(exportTarget)) {
                return handleFailure("Export target validation failed", null);
            }

            // convert the domain object into export-ready rows/records
            ExportCSV.PreparedExportData preparedData = prepareData(exportTarget);
            if (preparedData == null || preparedData.getRecordCount() == 0) {
                return handleFailure("No data to export after preparation step", null);
            }

            // write the prepared data to disk (or wherever the subclass targets)
            String outputFilePath = writeExport(preparedData);
            if (outputFilePath == null) {
                return handleFailure("Write step failed — output path is null", null);
            }

            // optional cleanup, notifications, audit logging
            postExportProcessing(outputFilePath);

            return handleSuccess(outputFilePath, preparedData.getRecordCount());

        } catch (Exception e) {
            logger.error("Unexpected error during export", e);
            return handleFailure("Export failed with exception: " + e.getMessage(), e);
        } finally {
            exportStatus.setEndTime(LocalDateTime.now());
            logger.info("Export finished — status: {}, records: {}, duration: {}ms",
                    exportStatus.getStatus(),
                    exportStatus.getRecordsExported(),
                    exportStatus.getDurationMillis());
        }
    }

    // Abstract methods

    /**
     * Registers the validation rules used by {@link #validateExportTarget(Object)}
     * Called once during construction so rules are in place before the first export
     */
    protected abstract void initializeValidationRules();

    /**
     * Validates the export target and output destination before any data is read
     *
     * @param exportTarget the object to validate; may be null
     * @return true if the export can proceed; false to abort with a failure result
     */
    protected abstract boolean validateExportTarget(Object exportTarget);

    /**
     * Flattens the domain object into a list of records ready to be serialize
     *
     * @param exportTarget the already-validated domain object
     * @return a PreparedExportData holder with the records and a count; null to abort
     */
    protected abstract ExportCSV.PreparedExportData prepareData(Object exportTarget);

    /**
     * Serializes and writes the prepared records to their destination
     *
     * @param preparedData the records returned by {@link #prepareData(Object)}
     * @return the output file path (or URL/identifier) on success; null on failure
     */
    protected abstract String writeExport(ExportCSV.PreparedExportData preparedData);
    /**
     * Performs any tasks that should happen after a successful write
     * Default implementation does nothing, Override to add notifications,
     * file archiving, or audit logging
     *
     * @param outputFilePath the path returned by {@link #writeExport(ExportCSV.PreparedExportData)}
     */
    protected void postExportProcessing(String outputFilePath) {
        logger.debug("No post-export processing defined for this export type");
    }

    // Private result builders
    private ExportResult handleSuccess(String outputFilePath, int recordCount) {
        exportStatus.setStatus(ExportStatus.Status.SUCCESS);
        exportStatus.setRecordsExported(recordCount);
        logger.info("Export completed: {} records written to {}", recordCount, outputFilePath);
        return new ExportResult(true, exportStatus, outputFilePath);
    }

    private ExportResult handleFailure(String message, Exception e) {
        exportStatus.setStatus(ExportStatus.Status.FAILED);
        exportStatus.setErrorMessage(message);
        logger.error("Export failed: {}", message, e);
        return new ExportResult(false, exportStatus, null);
    }

    // Inner helper classes
    /**
     * Holds the outcome of a single {@link #exportData(Object)} call
     */
    public static class ExportResult {
        private final boolean success;
        private final ExportStatus status;
        private final String outputFilePath;

        public ExportResult(boolean success, ExportStatus status, String outputFilePath) {
            this.success = success;
            this.status = status;
            this.outputFilePath = outputFilePath;
        }

        /** @return true if the export completed without errors */
        public boolean isSuccess()         { return success; }

        /** @return status details (timing, error message, record count) */
        public ExportStatus getStatus()    { return status; }

        /**
         * @return the absolute path of the exported file, or null if the export failed
         */
        public String getOutputFilePath()  { return outputFilePath; }
    }

    /**
     * Tracks runtime information about an in-progress or completed export
     */
    public static class ExportStatus {

        public enum Status { PENDING, SUCCESS, FAILED }

        private Status status = Status.PENDING;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private int recordsExported;
        private String errorMessage;
        private boolean postProcessed;

        public Status        getStatus()                  { return status; }
        public void          setStatus(Status s)          { this.status = s; }

        public LocalDateTime getStartTime()               { return startTime; }
        public void          setStartTime(LocalDateTime t){ this.startTime = t; }

        public LocalDateTime getEndTime()                 { return endTime; }
        public void          setEndTime(LocalDateTime t)  { this.endTime = t; }

        public int           getRecordsExported()         { return recordsExported; }
        public void          setRecordsExported(int n)    { this.recordsExported = n; }

        public String        getErrorMessage()            { return errorMessage; }
        public void          setErrorMessage(String m)    { this.errorMessage = m; }

        public boolean       isPostProcessed()            { return postProcessed; }
        public void          setPostProcessed(boolean b)  { this.postProcessed = b; }

        /**
         * Elapsed time in milliseconds, or -1 if the export hasn't finished yet
         */
        public long getDurationMillis() {
            if (startTime == null || endTime == null) return -1;
            return java.time.Duration.between(startTime, endTime).toMillis();
        }
    }
}
