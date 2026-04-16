package edu.isu.gamematch.imports;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;
import java.util.*;

public abstract class ImportData {
    
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected ImportStatus importStatus;
    protected List<ValidationRule> validationRules;
    
    public ImportData() {
        this.importStatus = new ImportStatus();
        this.validationRules = new ArrayList<>();
        initializeValidationRules();
    }
    
    public final ImportResult importData(String source) {
        logger.info("Starting import process from source: {}", source);
        importStatus.setStartTime(LocalDateTime.now());
        
        try {
            if (!validateSource(source)) {
                return handleFailure("Source validation failed", null);
            }
            
            RawData rawData = extractData(source);
            if (rawData == null || rawData.isEmpty()) {
                return handleFailure("No data extracted from source", null);
            }
            
            TransformedData transformedData = transformData(rawData);
            if (transformedData == null) {
                return handleFailure("Data transformation failed", null);
            }
            
            boolean loadSuccess = loadData(transformedData);
            if (!loadSuccess) {
                return handleFailure("Data loading failed", null);
            }
            
            postImportProcessing(transformedData);
            
            return handleSuccess(transformedData);
            
        } catch (Exception e) {
            logger.error("Import failed", e);
            return handleFailure("Import failed with exception: " + e.getMessage(), e);
        } finally {
            importStatus.setEndTime(LocalDateTime.now());
            logImportResult();
        }
    }
    
    protected abstract boolean validateSource(String source);
    protected abstract RawData extractData(String source);
    protected abstract TransformedData transformData(RawData rawData);
    protected abstract boolean loadData(TransformedData data);
    
    protected void postImportProcessing(TransformedData data) {
        logger.debug("No post-processing defined for this import type");
    }
    
    protected void initializeValidationRules() {
        // Default implementation - subclasses override
    }
    
    private ImportResult handleSuccess(TransformedData data) {
        importStatus.setStatus(ImportStatus.Status.SUCCESS);
        importStatus.setRecordsProcessed(data.getRecordCount());
        logger.info("Import completed successfully: {} records processed", 
            data.getRecordCount());
        return new ImportResult(true, importStatus, data);
    }
    
    private ImportResult handleFailure(String message, Exception e) {
        importStatus.setStatus(ImportStatus.Status.FAILED);
        importStatus.setErrorMessage(message);
        logger.error("Import failed: {}", message, e);
        return new ImportResult(false, importStatus, null);
    }
    
    private void logImportResult() {
        logger.info("Import result: status={}, duration={}ms, records={}", 
            importStatus.getStatus(),
            importStatus.getDurationMillis(),
            importStatus.getRecordsProcessed());
    }
    
    // Inner classes
    public static class ImportStatus {
        public enum Status { PENDING, SUCCESS, FAILED }
        private Status status = Status.PENDING;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private int recordsProcessed;
        private String errorMessage;
        
        public Status getStatus() { return status; }
        public void setStatus(Status status) { this.status = status; }
        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
        public int getRecordsProcessed() { return recordsProcessed; }
        public void setRecordsProcessed(int recordsProcessed) { this.recordsProcessed = recordsProcessed; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public void incrementProcessedRecords() { this.recordsProcessed++; }
        
        public long getDurationMillis() {
            if (startTime == null || endTime == null) return -1;
            return java.time.Duration.between(startTime, endTime).toMillis();
        }
    }
    
    public static class ValidationRule {
        private final String ruleId;
        private final String description;
        public ValidationRule(String ruleId, String description) {
            this.ruleId = ruleId;
            this.description = description;
        }
        public String getRuleId() { return ruleId; }
        public String getDescription() { return description; }
    }
    
    public static class ImportResult {
        private final boolean success;
        private final ImportStatus status;
        private final TransformedData data;
        
        public ImportResult(boolean success, ImportStatus status, TransformedData data) {
            this.success = success;
            this.status = status;
            this.data = data;
        }
        public boolean isSuccess() { return success; }
        public ImportStatus getStatus() { return status; }
        public TransformedData getData() { return data; }
    }
    
    public static class RawData {
        private String source;
        private String format;
        private Object content;
        private Map<String, Object> metadata = new HashMap<>();
        
        public boolean isEmpty() { return content == null; }
        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
        public String getFormat() { return format; }
        public void setFormat(String format) { this.format = format; }
        public Object getContent() { return content; }
        public void setContent(Object content) { this.content = content; }
        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    }
    
    public static class TransformedData {
        private String sourceType;
        private String originalFormat;
        private List<Object> records = new ArrayList<>();
        private int recordCount;
        private List<ValidationResult> validationResults = new ArrayList<>();
        
        public String getSourceType() { return sourceType; }
        public void setSourceType(String sourceType) { this.sourceType = sourceType; }
        public String getOriginalFormat() { return originalFormat; }
        public void setOriginalFormat(String originalFormat) { this.originalFormat = originalFormat; }
        public List<Object> getRecords() { return records; }
        public void setRecords(List<Object> records) { this.records = records; }
        public int getRecordCount() { return recordCount; }
        public void setRecordCount(int recordCount) { this.recordCount = recordCount; }
        public List<ValidationResult> getValidationResults() { return validationResults; }
        public void setValidationResults(List<ValidationResult> validationResults) { 
            this.validationResults = validationResults; 
        }
    }
    
    public static class ValidationResult {
        private final boolean valid;
        private final String message;
        
        public ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }
        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
    }
    
    public static class ImportRecord {
        private final String type;
        private final String content;
        
        public ImportRecord(String type, String content) {
            this.type = type;
            this.content = content;
        }
        public String getType() { return type; }
        public String getContent() { return content; }
    }
}