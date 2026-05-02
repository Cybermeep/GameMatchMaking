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
    User user = sqlHandler.searchUserBySteamId(steamId);

    if (user == null) {
        user = new User();
        user.setSteamID(Long.parseLong(steamId));
        user.setPersonaName(personaName);               
        UserProfile profile = new UserProfile(personaName, user);
        user.setUserProfile(profile);
        sqlHandler.createUser(user);
    }

    return user;
}
}