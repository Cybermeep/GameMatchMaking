package edu.isu.gamematch.controller;

import edu.isu.gamematch.User;
import edu.isu.gamematch.UserProfile;
import edu.isu.gamematch.SQLHandler;
import edu.isu.gamematch.service.OnlineUserTracker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpSession;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;

@Controller
public class LocalAuthController {

    @Autowired
    private SQLHandler sqlHandler;

    @Autowired
    private OnlineUserTracker onlineTracker;

    @GetMapping("/register")
    public String showRegistrationForm() {
        return "register";  // new template
    }

    @PostMapping("/register")
    public String register(@RequestParam String username,
                           @RequestParam String password,
                           @RequestParam String personaName,
                           HttpSession session, Model model) {
        // Check if local username taken
        User existing = sqlHandler.searchUserByLocalUsername(username);
        if (existing != null) {
            model.addAttribute("error", "Username already exists");
            return "register";
        }
        User user = new User();
        user.setLocalUsername(username);
        user.setPasswordHash(hashPassword(password));
        user.setPersonaName(personaName);
        UserProfile profile = new UserProfile(personaName, user);
        user.setUserProfile(profile);
        sqlHandler.createUser(user);
        // log in
        session.setAttribute("db_user", user);
        onlineTracker.userLoggedIn(user.getUserID());
        return "redirect:/dashboard";
    }

    @GetMapping("/login")
    public String showLoginForm() {
        return "login";   // new template
    }

    @PostMapping("/login")
    public String login(@RequestParam String username,
                        @RequestParam String password,
                        HttpSession session, Model model) {
        User user = sqlHandler.searchUserByLocalUsername(username);
        if (user == null || !user.getPasswordHash().equals(hashPassword(password))) {
            model.addAttribute("error", "Invalid credentials");
            return "login";
        }
        session.setAttribute("db_user", user);
        onlineTracker.userLoggedIn(user.getUserID());
        return "redirect:/dashboard";
    }

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}