package edu.isu.gamematch;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public class HibernateUtil {
    private static final SessionFactory sessionFactory = buildSessionFactory();

    private static SessionFactory buildSessionFactory() {
        try {
            Configuration config = new Configuration().configure();

            // Read credentials from environment variables (set in .env or Heroku config vars).
            // Falls back to the school credentials if env vars are not set.
            String username = System.getenv("ORACLE_USERNAME");
            String password = System.getenv("ORACLE_PASSWORD");
            String dbUrl    = System.getenv("ORACLE_DB_URL");

            if (username != null && !username.isEmpty()) {
                config.setProperty("hibernate.connection.username", username);
            }
            if (password != null && !password.isEmpty()) {
                config.setProperty("hibernate.connection.password", password);
            }
            if (dbUrl != null && !dbUrl.isEmpty()) {
                config.setProperty("hibernate.connection.url", dbUrl);
            }

            return config.buildSessionFactory();

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