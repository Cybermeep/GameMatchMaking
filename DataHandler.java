package edu.isu.gamematch;
import java.util.Map;


public abstract class DataHandler {
    // Fields
    private String serverName;
    private String username;
    private String password;
    protected static DataHandler handler;
    // Constructor
    public DataHandler(String serverName, String username, String password) {
        this.serverName = serverName;
        this.username = username;
        this.password = password;
    }

    // Connection methods
    public abstract boolean beginConnection();

    public abstract boolean endConnection();
    // Data operations
    public abstract boolean read();

    public abstract boolean passData(Map<String, Object> data);

    public abstract boolean remove();

    public abstract boolean write();

    // User management methods
    public abstract boolean createUser();
    
    public abstract boolean updateUsers();

    public abstract User removeUsers();

    public abstract User searchUser(String profileName);

    public abstract String generateActivitySummary();

    // Getters and setters
    public abstract String getServerName();

    public abstract void setServerName(String serverName);

    public abstract String getUsername();

    public abstract void setUsername(String username);
    
    public abstract String getPassword();

    public abstract void setPassword(String password);

    public abstract DataHandler getDh();

    public abstract void setDh(DataHandler dh);
}

