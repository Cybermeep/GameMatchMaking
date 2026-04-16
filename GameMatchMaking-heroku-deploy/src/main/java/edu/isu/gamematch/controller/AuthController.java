package edu.isu.gamematch.controller;

import edu.isu.gamematch.SQLHandler;
import edu.isu.gamematch.User;
import edu.isu.gamematch.UserProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/api/auth")
public class AuthController {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    
    @Autowired
    private SQLHandler sqlHandler;
    
    @PostMapping("/register")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String username = request.get("username");
            String email = request.get("email");
            String password = request.get("password");
            String steamId = request.get("steamId");
            
            // Validate input
            if (username == null || username.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Username is required");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Check if username already exists
            User existingUser = sqlHandler.searchUser(username);
            if (existingUser != null) {
                response.put("success", false);
                response.put("message", "Username already taken");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Create new user
            User user = new User();
            if (steamId != null && !steamId.isEmpty()) {
                try {
                    user.setSteamID(Long.parseLong(steamId));
                } catch (NumberFormatException e) {
                    // Invalid Steam ID, continue without it
                }
            }
            
            // Create profile
            UserProfile profile = new UserProfile(username, user);
            // Store email and password hash in a real implementation
            // For now, just store the username
            
            user.setUserProfile(profile);
            
            boolean success = sqlHandler.createUser(user);
            
            if (success) {
                response.put("success", true);
                response.put("message", "Registration successful");
                response.put("userId", user.getUserID());
                logger.info("New user registered: {}", username);
            } else {
                response.put("success", false);
                response.put("message", "Failed to create user");
            }
            
        } catch (Exception e) {
            logger.error("Registration error", e);
            response.put("success", false);
            response.put("message", "Server error: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/login")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> request, 
                                                      HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String username = request.get("username");
            String password = request.get("password");
            
            // Find user by username
            User user = sqlHandler.searchUser(username);
            
            if (user != null) {
                // In a real implementation, verify password hash
                // For demo, any password works
                
                session.setAttribute("db_user", user);
                session.setAttribute("user_id", user.getUserID());
                
                response.put("success", true);
                response.put("message", "Login successful");
                response.put("userId", user.getUserID());
                response.put("username", user.getUserProfile() != null ? 
                            user.getUserProfile().getProfileName() : username);
                
                logger.info("User logged in: {}", username);
            } else {
                response.put("success", false);
                response.put("message", "Invalid username or password");
            }
            
        } catch (Exception e) {
            logger.error("Login error", e);
            response.put("success", false);
            response.put("message", "Server error: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/logout")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> logout(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        
        session.invalidate();
        response.put("success", true);
        response.put("message", "Logged out successfully");
        
        return ResponseEntity.ok(response);
    }
}