package edu.isu.gamematch;

import org.hibernate.Session;

public class HibernateTest {
    public static void main(String[] args) {
        try {
            System.out.println("Testing HibernateUtil...");
            Session session = HibernateUtil.getSessionFactory().openSession();
            System.out.println("✓ SessionFactory created successfully");
            System.out.println("✓ Session opened successfully");
            session.close();
            System.out.println("✓ Session closed successfully");
            HibernateUtil.shutdown();
            System.out.println("✓ Connection test passed!");
        } catch (Exception e) {
            System.err.println("✗ Connection test failed:");
            e.printStackTrace();
        }
    }
}
