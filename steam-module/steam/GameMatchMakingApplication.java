/**
 * Game Match Making Application - Main Entry Point
 * 
 * This application allows Steam users to connect and find games to play together.
 * It integrates with Steam's OAuth for authentication and Steam Web API for
 * game library data.
 * 
 * @author The Match Makers
 * @version 1.0
 * @since 2026-01-22
 */
package edu.isu.gamematch.steam;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class GameMatchMakingApplication {

    public static void main(String[] args) {
        SpringApplication.run(GameMatchMakingApplication.class, args);
    }

    /**
     * RestTemplate bean for making HTTP requests to Steam API
     * 
     * @param builder RestTemplateBuilder for configuration
     * @return configured RestTemplate instance
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }
}