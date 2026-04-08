package edu.isu.gamematch;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

public class SQLHandler extends DataHandler {
    // Constructor matching DataHandler
    public SQLHandler(String serverName, String username, String password) {
        super(serverName, username, password);
    }

    // Alternative constructor with port
    public SQLHandler(String serverName, int port, String username, String password) {
        super(serverName, username, password);
    }

    // Stub implementations that just call parent class or return dummy values
    @Override
    public boolean beginConnection() {
        try {
            Session session = HibernateUtil.getSessionFactory().openSession();
            session.close();
            System.out.println("SQLHandler: Connection to " + getServerName() + " established");
            return true;
        } catch (Exception e) {
            System.err.println("SQLHandler: Failed to establish connection - " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean endConnection() {
        try {
            HibernateUtil.shutdown();
            System.out.println("SQLHandler: Connection to " + getServerName() + " closed");
            return true;
        } catch (Exception e) {
            System.err.println("SQLHandler: Failed to close connection - " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean read() {
        System.out.println("SQLHandler: Reading data from " + getServerName());
        return true;
    }

    public List<User> getAllUsers() {
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            Query<User> query = session.createQuery("FROM User", User.class);
            return query.list();
        } catch (Exception e) {
            System.err.println("SQLHandler: Error reading users - " + e.getMessage());
            return null;
        } finally {
            session.close();
        }
    }

    @Override
    public boolean passData(Map<String, Object> data) {
        System.out.println("SQLHandler: Passing data to " + getServerName());
        return true;
    }

    @Override
    public boolean remove() {
        System.out.println("SQLHandler: Removing data from " + getServerName());
        return true;
    }

    @Override
    public boolean write() {
        System.out.println("SQLHandler: Writing data to " + getServerName());
        return true;
    }

    public boolean writeEntity(Object entity) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            session.persist(entity);
            tx.commit();
            return true;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            System.err.println("SQLHandler: Error writing entity - " + e.getMessage());
            return false;
        } finally {
            session.close();
        }
    }

    @Override
    public boolean createUser() {
        System.out.println("SQLHandler: Creating user on " + getServerName());
        return true;
    }

    public boolean createUser(User user) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            session.persist(user);
            tx.commit();
            System.out.println("SQLHandler: User created successfully");
            return true;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            System.err.println("SQLHandler: Error creating user - " + e.getMessage());
            return false;
        } finally {
            session.close();
        }
    }

    @Override
    public boolean updateUsers() {
        System.out.println("SQLHandler: Updating users on " + getServerName());
        return true;
    }

    public boolean updateUser(User user) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            session.merge(user);
            tx.commit();
            System.out.println("SQLHandler: User updated successfully");
            return true;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            System.err.println("SQLHandler: Error updating user - " + e.getMessage());
            return false;
        } finally {
            session.close();
        }
    }

    @Override
    public User removeUsers() {
        System.out.println("SQLHandler: Removing users from " + getServerName());
        return null;
    }

    public boolean deleteUser(User user) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            session.remove(user);
            tx.commit();
            System.out.println("SQLHandler: User deleted successfully");
            return true;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            System.err.println("SQLHandler: Error deleting user - " + e.getMessage());
            return false;
        } finally {
            session.close();
        }
    }

    @Override
    public User searchUser(String profileName) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            Query<User> query = session.createQuery(
                "FROM User u WHERE u.userProfile.profileName = :name", User.class);
            query.setParameter("name", profileName);
            return query.uniqueResult();
        } catch (Exception e) {
            System.err.println("SQLHandler: Error searching for user - " + e.getMessage());
            return null;
        } finally {
            session.close();
        }
    }

    @Override
    public String generateActivitySummary() {
        System.out.println("SQLHandler: Generating activity summary for " + getServerName());
        return "Activity summary for " + getServerName();
    }

    // ==================== GAME OPERATIONS ====================
    public boolean createGame(Game game) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            session.persist(game);
            tx.commit();
            System.out.println("SQLHandler: Game created successfully");
            return true;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            System.err.println("SQLHandler: Error creating game - " + e.getMessage());
            return false;
        } finally {
            session.close();
        }
    }

    public Game getGameById(int gameID) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            return session.get(Game.class, gameID);
        } catch (Exception e) {
            System.err.println("SQLHandler: Error retrieving game - " + e.getMessage());
            return null;
        } finally {
            session.close();
        }
    }

    public Game searchGameByName(String gameName) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            Query<Game> query = session.createQuery("FROM Game g WHERE g.gameName = :name", Game.class);
            query.setParameter("name", gameName);
            return query.uniqueResult();
        } catch (Exception e) {
            System.err.println("SQLHandler: Error searching for game - " + e.getMessage());
            return null;
        } finally {
            session.close();
        }
    }

    public List<Game> getAllGames() {
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            Query<Game> query = session.createQuery("FROM Game", Game.class);
            return query.list();
        } catch (Exception e) {
            System.err.println("SQLHandler: Error retrieving games - " + e.getMessage());
            return null;
        } finally {
            session.close();
        }
    }

    public boolean updateGame(Game game) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            session.merge(game);
            tx.commit();
            System.out.println("SQLHandler: Game updated successfully");
            return true;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            System.err.println("SQLHandler: Error updating game - " + e.getMessage());
            return false;
        } finally {
            session.close();
        }
    }

    public boolean deleteGame(Game game) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            session.remove(session.merge(game));
            tx.commit();
            System.out.println("SQLHandler: Game deleted successfully");
            return true;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            System.err.println("SQLHandler: Error deleting game - " + e.getMessage());
            return false;
        } finally {
            session.close();
        }
    }

    // ==================== GROUP OPERATIONS ====================
    public boolean createGroup(Group group) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            session.persist(group);
            tx.commit();
            System.out.println("SQLHandler: Group created successfully");
            return true;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            System.err.println("SQLHandler: Error creating group - " + e.getMessage());
            return false;
        } finally {
            session.close();
        }
    }

    public Group getGroupById(int groupID) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            return session.get(Group.class, groupID);
        } catch (Exception e) {
            System.err.println("SQLHandler: Error retrieving group - " + e.getMessage());
            return null;
        } finally {
            session.close();
        }
    }

    public List<Group> getAllGroups() {
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            Query<Group> query = session.createQuery("FROM Group", Group.class);
            return query.list();
        } catch (Exception e) {
            System.err.println("SQLHandler: Error retrieving groups - " + e.getMessage());
            return null;
        } finally {
            session.close();
        }
    }

    public List<Group> getGroupsByOwner(User owner) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            Query<Group> query = session.createQuery("FROM Group g WHERE g.groupOwner = :owner", Group.class);
            query.setParameter("owner", owner);
            return query.list();
        } catch (Exception e) {
            System.err.println("SQLHandler: Error retrieving user's groups - " + e.getMessage());
            return null;
        } finally {
            session.close();
        }
    }

    public boolean updateGroup(Group group) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            session.merge(group);
            tx.commit();
            System.out.println("SQLHandler: Group updated successfully");
            return true;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            System.err.println("SQLHandler: Error updating group - " + e.getMessage());
            return false;
        } finally {
            session.close();
        }
    }

    public boolean deleteGroup(Group group) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            session.remove(session.merge(group));
            tx.commit();
            System.out.println("SQLHandler: Group deleted successfully");
            return true;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            System.err.println("SQLHandler: Error deleting group - " + e.getMessage());
            return false;
        } finally {
            session.close();
        }
    }

    // ==================== ACHIEVEMENT OPERATIONS ====================
    public boolean createAchievement(Achievement achievement) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            session.persist(achievement);
            tx.commit();
            System.out.println("SQLHandler: Achievement created successfully");
            return true;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            System.err.println("SQLHandler: Error creating achievement - " + e.getMessage());
            return false;
        } finally {
            session.close();
        }
    }

    public Achievement getAchievementById(int achievementID) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            return session.get(Achievement.class, achievementID);
        } catch (Exception e) {
            System.err.println("SQLHandler: Error retrieving achievement - " + e.getMessage());
            return null;
        } finally {
            session.close();
        }
    }

    public Achievement searchAchievementByName(String achievementName) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            Query<Achievement> query = session.createQuery("FROM Achievement a WHERE a.achievementName = :name", Achievement.class);
            query.setParameter("name", achievementName);
            return query.uniqueResult();
        } catch (Exception e) {
            System.err.println("SQLHandler: Error searching for achievement - " + e.getMessage());
            return null;
        } finally {
            session.close();
        }
    }

    public List<Achievement> getAllAchievements() {
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            Query<Achievement> query = session.createQuery("FROM Achievement", Achievement.class);
            return query.list();
        } catch (Exception e) {
            System.err.println("SQLHandler: Error retrieving achievements - " + e.getMessage());
            return null;
        } finally {
            session.close();
        }
    }

    public boolean updateAchievement(Achievement achievement) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            session.merge(achievement);
            tx.commit();
            System.out.println("SQLHandler: Achievement updated successfully");
            return true;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            System.err.println("SQLHandler: Error updating achievement - " + e.getMessage());
            return false;
        } finally {
            session.close();
        }
    }

    public boolean deleteAchievement(Achievement achievement) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            session.remove(session.merge(achievement));
            tx.commit();
            System.out.println("SQLHandler: Achievement deleted successfully");
            return true;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            System.err.println("SQLHandler: Error deleting achievement - " + e.getMessage());
            return false;
        } finally {
            session.close();
        }
    }

    // ==================== TAG OPERATIONS ====================
    public boolean createTag(Tag tag) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            session.persist(tag);
            tx.commit();
            System.out.println("SQLHandler: Tag created successfully");
            return true;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            System.err.println("SQLHandler: Error creating tag - " + e.getMessage());
            return false;
        } finally {
            session.close();
        }
    }

    public Tag getTagByName(String tagName) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            return session.get(Tag.class, tagName);
        } catch (Exception e) {
            System.err.println("SQLHandler: Error retrieving tag - " + e.getMessage());
            return null;
        } finally {
            session.close();
        }
    }

    public List<Tag> getTagsByGame(Game game) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            Query<Tag> query = session.createQuery("FROM Tag t WHERE t.game = :game", Tag.class);
            query.setParameter("game", game);
            return query.list();
        } catch (Exception e) {
            System.err.println("SQLHandler: Error retrieving tags - " + e.getMessage());
            return null;
        } finally {
            session.close();
        }
    }

    public List<Tag> getAllTags() {
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            Query<Tag> query = session.createQuery("FROM Tag", Tag.class);
            return query.list();
        } catch (Exception e) {
            System.err.println("SQLHandler: Error retrieving all tags - " + e.getMessage());
            return null;
        } finally {
            session.close();
        }
    }

    public boolean updateTag(Tag tag) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            session.merge(tag);
            tx.commit();
            System.out.println("SQLHandler: Tag updated successfully");
            return true;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            System.err.println("SQLHandler: Error updating tag - " + e.getMessage());
            return false;
        } finally {
            session.close();
        }
    }

    public boolean deleteTag(Tag tag) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            session.remove(session.merge(tag));
            tx.commit();
            System.out.println("SQLHandler: Tag deleted successfully");
            return true;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            System.err.println("SQLHandler: Error deleting tag - " + e.getMessage());
            return false;
        } finally {
            session.close();
        }
    }

    // ==================== USER PROFILE OPERATIONS ====================
    public boolean createUserProfile(UserProfile userProfile) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            session.persist(userProfile);
            tx.commit();
            System.out.println("SQLHandler: UserProfile created successfully");
            return true;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            System.err.println("SQLHandler: Error creating user profile - " + e.getMessage());
            return false;
        } finally {
            session.close();
        }
    }

    public UserProfile getUserProfileById(int profileId) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            return session.get(UserProfile.class, profileId);
        } catch (Exception e) {
            System.err.println("SQLHandler: Error retrieving user profile - " + e.getMessage());
            return null;
        } finally {
            session.close();
        }
    }

    public UserProfile getUserProfileByName(String profileName) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            Query<UserProfile> query = session.createQuery("FROM UserProfile up WHERE up.profileName = :name", UserProfile.class);
            query.setParameter("name", profileName);
            return query.uniqueResult();
        } catch (Exception e) {
            System.err.println("SQLHandler: Error retrieving user profile by name - " + e.getMessage());
            return null;
        } finally {
            session.close();
        }
    }

    public List<UserProfile> getAllUserProfiles() {
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            Query<UserProfile> query = session.createQuery("FROM UserProfile", UserProfile.class);
            return query.list();
        } catch (Exception e) {
            System.err.println("SQLHandler: Error retrieving user profiles - " + e.getMessage());
            return null;
        } finally {
            session.close();
        }
    }

    public boolean updateUserProfile(UserProfile userProfile) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            session.merge(userProfile);
            tx.commit();
            System.out.println("SQLHandler: UserProfile updated successfully");
            return true;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            System.err.println("SQLHandler: Error updating user profile - " + e.getMessage());
            return false;
        } finally {
            session.close();
        }
    }

    public boolean deleteUserProfile(UserProfile userProfile) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            session.remove(session.merge(userProfile));
            tx.commit();
            System.out.println("SQLHandler: UserProfile deleted successfully");
            return true;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            System.err.println("SQLHandler: Error deleting user profile - " + e.getMessage());
            return false;
        } finally {
            session.close();
        }
    }
}