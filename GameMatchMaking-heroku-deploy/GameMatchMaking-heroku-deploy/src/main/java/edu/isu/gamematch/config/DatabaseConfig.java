package edu.isu.gamematch.config;

import edu.isu.gamematch.SQLHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@org.springframework.context.annotation.Configuration
public class DatabaseConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public SQLHandler sqlHandler() {
        return (SQLHandler) SQLHandler.createInstance("oracle-school", "", "");
    }
}