package edu.isu.gamematch.imports;

import org.springframework.stereotype.Component;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Concrete implementation for importing data from local file system
 * 
 */
@Component
public class ImportLocalData extends ImportData {
    
    private List<String> allowedFileExtensions;
    private long maxFileSize;
    private String baseDirectory;
    private String fileEncoding;
    
    public ImportLocalData() {
        this.allowedFileExtensions = Arrays.asList(".json", ".xml", ".csv", ".txt");
        this.maxFileSize = 10 * 1024 * 1024; // 10 MB default
        this.baseDirectory = System.getProperty("user.dir") + "/imports";
        this.fileEncoding = "UTF-8";
    }
    
    public ImportLocalData(String baseDirectory, long maxFileSize) {
        this();
        this.baseDirectory = baseDirectory;
        this.maxFileSize = maxFileSize;
    }
    
    @Override
    protected void initializeValidationRules() {
        validationRules.add(new ValidationRule("FILE_EXISTS", "File must exist"));
        validationRules.add(new ValidationRule("FILE_SIZE", "File size must not exceed " + maxFileSize));
        validationRules.add(new ValidationRule("FILE_EXTENSION", "File extension must be one of: " + allowedFileExtensions));
        validationRules.add(new ValidationRule("FILE_READABLE", "File must be readable"));
    }
    
    @Override
    protected boolean validateSource(String source) {
        logger.debug("Validating local source: {}", source);
        
        // Build full path
        String fullPath = baseDirectory + File.separator + source;
        
        // Check file exists
        if (!validateFileExists(fullPath)) {
            logger.error("File does not exist: {}", fullPath);
            return false;
        }
        
        // Check file size
        if (!validateFileSize(fullPath)) {
            logger.error("File exceeds max size: {}", maxFileSize);
            return false;
        }
        
        // Check file extension
        if (!validateFileExtension(fullPath)) {
            logger.error("File extension not allowed for: {}", fullPath);
            return false;
        }
        
        // Check file readable
        if (!validateFileReadable(fullPath)) {
            logger.error("File is not readable: {}", fullPath);
            return false;
        }
        
        logger.info("Local source validation passed: {}", fullPath);
        return true;
    }
    
    @Override
    protected RawData extractData(String source) {
        String fullPath = baseDirectory + File.separator + source;
        logger.debug("Extracting data from: {}", fullPath);
        
        try {
            String content = readFileContents(fullPath);
            String fileExtension = getFileExtension(fullPath);
            
            RawData rawData = new RawData();
            rawData.setSource(source);
            rawData.setFormat(fileExtension);
            rawData.setContent(content);
            rawData.setMetadata(extractFileMetadata(fullPath));
            
            logger.info("Extracted {} bytes from {}", content.length(), source);
            return rawData;
            
        } catch (IOException e) {
            logger.error("Failed to extract data from: {}", source, e);
            return null;
        }
    }
    
    @Override
    protected TransformedData transformData(RawData rawData) {
        logger.debug("Transforming data from format: {}", rawData.getFormat());
        
        TransformedData transformedData = new TransformedData();
        transformedData.setSourceType("LOCAL_FILE");
        transformedData.setOriginalFormat(rawData.getFormat());
        
        try {
            // Parse based on file format
            switch (rawData.getFormat().toLowerCase()) {
                case ".json":
                    transformedData.setRecords(parseJsonContent(rawData.getContent()));
                    break;
                case ".xml":
                    transformedData.setRecords(parseXmlContent(rawData.getContent()));
                    break;
                case ".csv":
                    transformedData.setRecords(parseCsvContent(rawData.getContent()));
                    break;
                default:
                    transformedData.setRecords(parseTextContent(rawData.getContent()));
            }
            
            transformedData.setRecordCount(transformedData.getRecords().size());
            transformedData.setValidationResults(validateTransformedData(transformedData));
            
            logger.info("Transformed {} records from local file", transformedData.getRecordCount());
            return transformedData;
            
        } catch (Exception e) {
            logger.error("Data transformation failed", e);
            return null;
        }
    }
    
    @Override
    protected boolean loadData(TransformedData data) {
        logger.debug("Loading {} records into system", data.getRecordCount());
        
        try {
            // Todo: implement this with the actual data layer
            
            for (Object record : data.getRecords()) {
                // In real implementation, this would call repository.save()
                logger.trace("Loading record: {}", record);
                importStatus.incrementProcessedRecords();
            }
            
            logger.info("Successfully loaded {} records", data.getRecordCount());
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to load data", e);
            return false;
        }
    }
    
    @Override
    protected void postImportProcessing(TransformedData data) {
        logger.debug("Post-import processing for local file import");
        
        // Archive or move the imported file
        // Send notifications
        // Update audit logs
        
        importStatus.setPostProcessed(true);
    }
    
    // Private helper methods
    
    private boolean validateFileExists(String path) {
        return Files.exists(Paths.get(path));
    }
    
    private boolean validateFileSize(String path) {
        try {
            long size = Files.size(Paths.get(path));
            return size <= maxFileSize;
        } catch (IOException e) {
            return false;
        }
    }
    
    private boolean validateFileExtension(String path) {
        String extension = getFileExtension(path);
        return allowedFileExtensions.contains(extension.toLowerCase());
    }
    
    private boolean validateFileReadable(String path) {
        return Files.isReadable(Paths.get(path));
    }
    
    private String getFileExtension(String path) {
        int lastDot = path.lastIndexOf('.');
        return lastDot > 0 ? path.substring(lastDot) : "";
    }
    
    private String readFileContents(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), fileEncoding);
    }
    
    private Map<String, Object> extractFileMetadata(String path) throws IOException {
        Map<String, Object> metadata = new HashMap<>();
        Path filePath = Paths.get(path);
        metadata.put("fileName", filePath.getFileName().toString());
        metadata.put("size", Files.size(filePath));
        metadata.put("lastModified", Files.getLastModifiedTime(filePath));
        return metadata;
    }
    
    //barebones json parser, needs actual implementation
    private List<Object> parseJsonContent(String content) {
        List<Object> records = new ArrayList<>();
        records.add(new ImportRecord("json", content.substring(0, Math.min(100, content.length()))));
        return records;
    }
    
    private List<Object> parseXmlContent(String content) {
        List<Object> records = new ArrayList<>();
        records.add(new ImportRecord("xml", content.substring(0, Math.min(100, content.length()))));
        return records;
    }
    
    private List<Object> parseCsvContent(String content) {
        List<Object> records = new ArrayList<>();
        String[] lines = content.split("\n");
        for (String line : lines) {
            records.add(new ImportRecord("csv", line));
        }
        return records;
    }
    
    private List<Object> parseTextContent(String content) {
        List<Object> records = new ArrayList<>();
        records.add(new ImportRecord("text", content));
        return records;
    }
    
    private List<ValidationResult> validateTransformedData(TransformedData data) {
        List<ValidationResult> results = new ArrayList<>();
        // In real implementation, validate against business rules
        results.add(new ValidationResult(true, "All records validated"));
        return results;
    }
}