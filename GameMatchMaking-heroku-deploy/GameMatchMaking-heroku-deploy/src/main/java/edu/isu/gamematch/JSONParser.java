package edu.isu.gamematch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;

public class JSONParser extends DataParser {
    
    private final ObjectMapper objectMapper;
    
    public JSONParser(DataHandler dh) {
        super(dh);
        this.objectMapper = new ObjectMapper();
    }

    @Override
    protected String getFormatName() {
        return "JSON";
    }

    @Override
    protected boolean validateContent(String rawContent) {
        if (rawContent == null || rawContent.trim().isEmpty()) {
            logger.warn("JSON content is null or empty");
            return false;
        }
        
        String trimmed = rawContent.trim();
        // Basic JSON validation - starts with { or [
        return (trimmed.startsWith("{") && trimmed.endsWith("}")) ||
               (trimmed.startsWith("[") && trimmed.endsWith("]"));
    }

    @Override
    protected String preProcessContent(String rawContent) {
        // Remove BOM if present and trim whitespace
        String processed = rawContent.trim();
        if (processed.startsWith("\uFEFF")) {
            processed = processed.substring(1);
        }
        return processed;
    }

    @Override
    protected List<Object> parseContent(String processedContent) throws Exception {
        List<Object> records = new ArrayList<>();
        
        try {
            JsonNode rootNode = objectMapper.readTree(processedContent);
            
            if (rootNode.isArray()) {
                for (JsonNode node : rootNode) {
                    records.add(node);
                }
            } else if (rootNode.isObject()) {
                records.add(rootNode);
            }
            
            logger.info("Successfully parsed {} JSON elements", records.size());
            
        } catch (Exception e) {
            logger.error("Failed to parse JSON content", e);
            throw e;
        }
        
        return records;
    }
}