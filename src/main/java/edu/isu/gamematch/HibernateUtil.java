package edu.isu.gamematch;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public class HibernateUtil {

    private static SessionFactory sessionFactory;

    private static SessionFactory buildSessionFactory() {
        try {
            Configuration cfg = new Configuration();

            // Allow full override via system properties, fallback to env vars
            String jdbcUrl = System.getProperty("hibernate.connection.url",
                    System.getenv("JDBC_DATABASE_URL"));
            if (jdbcUrl == null || jdbcUrl.isEmpty()) {
                jdbcUrl = "jdbc:oracle:thin:@localhost:1521/XEPDB1";
            }

            String username = System.getProperty("hibernate.connection.username",
                    System.getenv("ORACLE_USERNAME"));
            String password = System.getProperty("hibernate.connection.password",
                    System.getenv("ORACLE_PASSWORD"));

            String driver = System.getProperty("hibernate.connection.driver_class",
                    "oracle.jdbc.OracleDriver");
            String dialect = System.getProperty("hibernate.dialect",
                    "org.hibernate.dialect.Oracle12cDialect");

            cfg.setProperty("hibernate.connection.driver_class", driver);
            cfg.setProperty("hibernate.connection.url", jdbcUrl);
            cfg.setProperty("hibernate.connection.username", username);
            cfg.setProperty("hibernate.connection.password", password);
            cfg.setProperty("hibernate.dialect", dialect);
            cfg.setProperty("hibernate.hbm2ddl.auto", "update");
            cfg.setProperty("hibernate.show_sql", "false");

            cfg.addAnnotatedClass(User.class);
            cfg.addAnnotatedClass(UserProfile.class);
            cfg.addAnnotatedClass(Game.class);
            cfg.addAnnotatedClass(Achievement.class);
            cfg.addAnnotatedClass(GameAchievement.class);
            cfg.addAnnotatedClass(Tag.class);
            cfg.addAnnotatedClass(Group.class);
            cfg.addAnnotatedClass(GroupSession.class);
            cfg.addAnnotatedClass(GroupVote.class);
            cfg.addAnnotatedClass(GroupJoinRequest.class);
            cfg.addAnnotatedClass(GroupPreference.class);
            cfg.addAnnotatedClass(GroupPreferenceVote.class);

            sessionFactory = cfg.buildSessionFactory();
            System.out.println("Hibernate SessionFactory created with: " + jdbcUrl);
            return sessionFactory;
        } catch (Exception e) {
            System.err.println("SessionFactory creation failed: " + e);
            throw new ExceptionInInitializerError(e);
        }
    }

    public static SessionFactory getSessionFactory() {
        if (sessionFactory == null) buildSessionFactory();
        return sessionFactory;
    }

    public static void shutdown() {
        if (sessionFactory != null) sessionFactory.close();
    }
}