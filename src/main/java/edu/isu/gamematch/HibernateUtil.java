package edu.isu.gamematch;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public class HibernateUtil {
    private static final SessionFactory sessionFactory = buildSessionFactory();

    private static SessionFactory buildSessionFactory() {
        try {
            Configuration cfg = new Configuration().configure();
            
            // Set credentials from environment variables (FIX #4)
            String username = System.getenv("ORACLE_USERNAME");
            String password = System.getenv("ORACLE_PASSWORD");
            
            if (username == null || username.isEmpty()) {
                System.err.println("WARNING: ORACLE_USERNAME environment variable not set!");
                username = "system"; // fallback
            }
            if (password == null || password.isEmpty()) {
                System.err.println("WARNING: ORACLE_PASSWORD environment variable not set!");
                password = "oracle"; // fallback
            }
            
            cfg.setProperty("hibernate.connection.username", username);
            cfg.setProperty("hibernate.connection.password", password);
            
            System.out.println("Hibernate configured with username: " + username);
            
            return cfg.buildSessionFactory();
        } catch (Exception e) {
            System.err.println("SessionFactory creation failed: " + e);
            throw new ExceptionInInitializerError(e);
        }
    }

    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public static void shutdown() {
        getSessionFactory().close();
    }
}