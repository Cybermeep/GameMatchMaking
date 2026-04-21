package edu.isu.gamematch.config;

import edu.isu.gamematch.SQLHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DatabaseConfig {
    
    @Value("${spring.datasource.url}")
    private String dbUrl;
    
    @Value("${spring.datasource.username}")
    private String dbUsername;
    
    @Value("${spring.datasource.password}")
    private String dbPassword;
    
    @Bean
    public SQLHandler sqlHandler() {
        // Parse server name from JDBC URL (extract host)
        String serverName = extractServerName(dbUrl);
        // Create instance using the static factory method
        return (SQLHandler) SQLHandler.createInstance(serverName, dbUsername, dbPassword);
    }
    
    private String extractServerName(String jdbcUrl) {
        // Example: jdbc:oracle:thin:@oracle-db:1521/XEPDB1
        try {
            String[] parts = jdbcUrl.split("@");
            if (parts.length > 1) {
                String hostPart = parts[1].split(":")[0];
                return hostPart;
            }
        } catch (Exception e) {
            // fallback
        }
        return "localhost";
    }
}