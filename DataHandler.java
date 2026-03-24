import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public class DataHandler {
    private SessionFactory sessionFactory;
    
    public DataHandler(String serverName, int serverPort, String serverPassword) {
        initializeHibernate(serverName, serverPort, serverPassword);
    }
    
    private void initializeHibernate(String serverName, int serverPort, String serverPassword) {
        Configuration config = new Configuration();
        config.configure("hibernate.cfg.xml");
        // Override properties if needed
        config.setProperty("hibernate.connection.url", 
            "jdbc:mysql://" + serverName + ":" + serverPort + "/gamematchmaking");
        config.setProperty("hibernate.connection.password", serverPassword);
        
        this.sessionFactory = config.buildSessionFactory();
    }
    
    public Session getSession() {
        return sessionFactory.openSession();
    }
    
    public void closeFactory() {
        sessionFactory.close();
    }
    
    // Example: Save a user
    public void saveUser(User user) {
        try (Session session = getSession()) {
            session.beginTransaction();
            session.persist(user);
            session.getTransaction().commit();
        }
    }
}