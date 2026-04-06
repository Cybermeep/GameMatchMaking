package edu.isu.gamematch;
import java.util.Map;

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
        System.out.println("SQLHandler: Beginning connection to " + getServerName());
        return true;
    }

    @Override
    public boolean endConnection() {
        System.out.println("SQLHandler: Ending connection to " + getServerName());
        return true;
    }

    @Override
    public boolean read() {
        System.out.println("SQLHandler: Reading data from " + getServerName());
        return true;
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

    @Override
    public boolean createUser() {
        System.out.println("SQLHandler: Creating user on " + getServerName());
        return true;
    }

    @Override
    public boolean updateUsers() {
        System.out.println("SQLHandler: Updating users on " + getServerName());
        return true;
    }

    @Override
    public User removeUsers() {
        System.out.println("SQLHandler: Removing users from " + getServerName());
        return null;
    }

    @Override
    public User searchUser(String profileName) {
        System.out.println("SQLHandler: Searching for user " + profileName + " on " + getServerName());
        return null;
    }

    @Override
    public String generateActivitySummary() {
        System.out.println("SQLHandler: Generating activity summary for " + getServerName());
        return "Activity summary for " + getServerName();
    }
}