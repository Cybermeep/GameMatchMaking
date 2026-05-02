package edu.isu.gamematch.config;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.NativeQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import edu.isu.gamematch.HibernateUtil;

@Component
public class DatabaseInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);

    @Value("${db.recreate:false}")
    private boolean recreate;

    @Override
    public void run(String... args) {
        if (!recreate) return;
        logger.warn("Recreating database tables (db.recreate=true) dropping all tables!");
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            // Drop tables in correct order (child tables first to avoid FK errors)
            String[] tables = {
                "session_members", "group_games", "group_members",
                "user_friends", "user_games", "user_game_tags",
                "group_preference_votes", "group_preferences",
                "group_votes", "group_join_requests", "group_sessions",
                "game_achievements", "achievements", "tags",
                "user_favorite_genres", "user_profiles",
                "games", "groups", "users"
            };
            for (String table : tables) {
                try {
                    NativeQuery query = session.createNativeQuery("DROP TABLE " + table + " CASCADE CONSTRAINTS");
                    query.executeUpdate();
                    logger.info("Dropped table: {}", table);
                } catch (Exception e) {
                    // Table might not exist  ignore
                    logger.debug("Could not drop table {} (maybe already missing): {}", table, e.getMessage());
                }
            }
            tx.commit();
            logger.info("All tables dropped successfully.");
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            logger.error("Error dropping tables", e);
        } finally {
            session.close();
        }
        // Exit the app so you can restart with normal ddl auto
        logger.warn("Shutting down after table recreation. Remove db.recreate=true before next start.");
        System.exit(0);
    }
}