package edu.isu.gamematch.service;

import edu.isu.gamematch.SQLHandler;
import edu.isu.gamematch.User;
import edu.isu.gamematch.UserProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    
    @Autowired
    private SQLHandler sqlHandler;
    
    public User findOrCreateUser(String steamId, String personaName) {
        // Try to find existing user by Steam ID
        User user = sqlHandler.searchUserBySteamId(steamId);
        
        if (user == null) {
            // Create new user
            user = new User();
            user.setSteamID(Long.parseLong(steamId));
            
            // Create user profile
            UserProfile profile = new UserProfile(personaName, user);
            user.setUserProfile(profile);
            
            // Save to database
            sqlHandler.createUser(user);
        }
        
        return user;
    }
}