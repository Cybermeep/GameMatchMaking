package edu.isu.gamematch.controller;

import edu.isu.gamematch.Group;
import edu.isu.gamematch.SQLHandler;
import edu.isu.gamematch.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.*;

@Controller
@RequestMapping("/api/groups")
public class GroupController {
    
    private static final Logger logger = LoggerFactory.getLogger(GroupController.class);
    
    @Autowired
    private SQLHandler sqlHandler;
    
    @PostMapping("/create")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createGroup(@RequestBody Map<String, String> request,
                                                            HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            User currentUser = (User) session.getAttribute("db_user");
            if (currentUser == null) {
                response.put("success", false);
                response.put("message", "Not logged in");
                return ResponseEntity.status(401).body(response);
            }
            
            String groupName = request.get("name");
            
            Group group = new Group();
            group.setGroupOwner(currentUser);
            group.addGroupMember(currentUser);
            
            boolean success = sqlHandler.createGroup(group);
            
            if (success) {
                response.put("success", true);
                response.put("message", "Group created successfully");
                response.put("groupId", group.getGroupID());
                logger.info("Group created: {} by user {}", groupName, currentUser.getUserID());
            } else {
                response.put("success", false);
                response.put("message", "Failed to create group");
            }
            
        } catch (Exception e) {
            logger.error("Group creation error", e);
            response.put("success", false);
            response.put("message", "Server error: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/my")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getMyGroups(HttpSession session) {
        List<Map<String, Object>> groups = new ArrayList<>();
        
        try {
            User currentUser = (User) session.getAttribute("db_user");
            if (currentUser == null) {
                return ResponseEntity.status(401).body(groups);
            }
            
            List<Group> userGroups = sqlHandler.getGroupsByOwner(currentUser);
            
            for (Group group : userGroups) {
                Map<String, Object> groupInfo = new HashMap<>();
                groupInfo.put("id", group.getGroupID());
                groupInfo.put("name", "Group " + group.getGroupID()); // Add name field to Group
                groupInfo.put("memberCount", group.getMembers().size());
                groupInfo.put("ownerName", group.getGroupOwner() != null && 
                            group.getGroupOwner().getUserProfile() != null ?
                            group.getGroupOwner().getUserProfile().getProfileName() : "Unknown");
                groups.add(groupInfo);
            }
            
        } catch (Exception e) {
            logger.error("Error fetching groups", e);
        }
        
        return ResponseEntity.ok(groups);
    }
    
    @PostMapping("/join")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> joinGroup(@RequestBody Map<String, String> request,
                                                          HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            User currentUser = (User) session.getAttribute("db_user");
            if (currentUser == null) {
                response.put("success", false);
                response.put("message", "Not logged in");
                return ResponseEntity.status(401).body(response);
            }
            
            String inviteCode = request.get("inviteCode");
            
            // In a real implementation, validate invite code and add user to group
            // For demo, just return success
            
            response.put("success", true);
            response.put("message", "Joined group successfully");
            
        } catch (Exception e) {
            logger.error("Join group error", e);
            response.put("success", false);
            response.put("message", "Server error: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
}