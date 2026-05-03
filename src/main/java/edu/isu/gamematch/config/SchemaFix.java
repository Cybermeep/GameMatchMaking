package edu.isu.gamematch.config;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import edu.isu.gamematch.HibernateUtil;

@Component
@Order(1)   // Run early, but after HibernateUtil is available
public class SchemaFix implements CommandLineRunner {

    @Override
    public void run(String... args) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();

            // 1. Make USER_ID nullable in GROUP_JOIN_REQUESTS (for invite tokens)
            try {
                session.createNativeQuery("ALTER TABLE GROUP_JOIN_REQUESTS MODIFY (USER_ID NULL)").executeUpdate();
                System.out.println("SchemaFix: Made USER_ID nullable in GROUP_JOIN_REQUESTS.");
            } catch (Exception e) {
                System.out.println("SchemaFix: GROUP_JOIN_REQUESTS.USER_ID may already be nullable or table missing: " + e.getMessage());
            }

            // 2. Add steam_appid column to games (if not already present)
            try {
                session.createNativeQuery("ALTER TABLE games ADD steam_appid VARCHAR2(20)").executeUpdate();
                System.out.println("SchemaFix: Added steam_appid column to games.");
            } catch (Exception e) {
                System.out.println("SchemaFix: steam_appid column may already exist: " + e.getMessage());
            }

            // 3. Add has_achievements column to games (or modify its default)
            try {
                // Attempt to add the column if it does not exist
                session.createNativeQuery("ALTER TABLE games ADD has_achievements NUMBER(1) DEFAULT 0").executeUpdate();
                System.out.println("SchemaFix: Added has_achievements column to games.");
            } catch (Exception e) {
                // Column probably exists; try to modify its default and update any nulls
                try {
                    session.createNativeQuery("ALTER TABLE games MODIFY has_achievements DEFAULT 0").executeUpdate();
                    session.createNativeQuery("UPDATE games SET has_achievements = 0 WHERE has_achievements IS NULL").executeUpdate();
                    System.out.println("SchemaFix: Updated has_achievements defaults and nulls.");
                } catch (Exception e2) {
                    System.out.println("SchemaFix: Could not modify has_achievements: " + e2.getMessage());
                }
            }

            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            System.err.println("SchemaFix: Unexpected error: " + e.getMessage());
        } finally {
            session.close();
        }
    }
}