package edu.isu.gamematch.config;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.persistence.EntityManagerFactory;

@org.springframework.context.annotation.Configuration
public class DatabaseConfig {
    
    @Bean
    public SessionFactory sessionFactory() {
        return new Configuration().configure().buildSessionFactory();
    }
    
    @Bean
    public SQLHandler sqlHandler() {
        return (SQLHandler) SQLHandler.createInstance("localhost", "sa", "");
    }
}