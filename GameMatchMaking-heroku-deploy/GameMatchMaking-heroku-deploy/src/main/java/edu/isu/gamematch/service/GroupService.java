package edu.isu.gamematch.service;

import edu.isu.gamematch.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Spring service layer bridging GroupOperations and SQLHandler for all
 * group-related SRS requirements. Consumed by GroupController (HTTP endpoints).
 */
@Service
public class GroupService {

    @Autowired
    private SQLHandler db;

    private final GroupOperations ops = new GroupOperations();

    // FR 3.1.4 – Create Group
    public Group createGroup(String name, User owner) {
        Group group = ops.createGroup(name, owner);
        db.createGroup(group);
        return group;
    }

    public Group getGroupById(int groupId) {
    return db.getGroupById(groupId);
            }

    // FR 3.1.5 – Delete Group
    public boolean deleteGroup(int groupId) {
        Group group = db.getGroupById(groupId);
        if (group == null) return false;
        db.deleteGroup(group);
        return true;
    }

    // FR 3.1.6 – Transfer Ownership
    public boolean transferOwnership(int groupId, User newOwner) {
        Group group = db.getGroupById(groupId);
        if (group == null) return false;
        User result = ops.transferGroupOwnership(newOwner, group);
        if (result == null) return false;
        db.updateGroup(group);
        return true;
    }

    // FR 3.1.7 – Invite Link
    public String getInviteLink(int groupId) {
        Group group = db.getGroupById(groupId);
        if (group == null) return null;
        String link = ops.generateGroupInviteLink(group);
        db.updateGroup(group);
        return link;
    }

    // FR 3.1.8 – Get Members
    public Set<User> getMembers(int groupId) {
        Group group = db.getGroupById(groupId);
        return group != null ? ops.retrieveGroupMembers(group) : Collections.emptySet();
    }

    // FR 3.1.9 – Join via token
    public boolean joinByToken(User user, String token) {
        Group group = db.getGroupByInviteToken(token);
        if (group == null) return false;
        boolean ok = ops.joinGroupByToken(user, group, token);
        if (ok) db.updateGroup(group);
        return ok;
    }

    // FR 3.1.10 – Leave Group
    public boolean leaveGroup(User user, int groupId) {
        Group group = db.getGroupById(groupId);
        if (group == null) return false;
        boolean ok = ops.leaveGroup(user, group);
        if (ok) db.updateGroup(group);
        return ok;
    }

    // FR 3.1.11 – Remove Member
    public boolean removeMember(User member, int groupId) {
        Group group = db.getGroupById(groupId);
        if (group == null) return false;
        User result = ops.removeGroupMember(member, group);
        if (result == null) return false;
        db.updateGroup(group);
        return true;
    }

    // FR 3.1.17 – Accept Join Request
    public boolean acceptJoinRequest(int requestId) {
        JoinRequest req = db.getJoinRequestById(requestId);
        if (req == null) return false;
        boolean ok = ops.acceptJoinRequest(req, req.getGroup());
        if (ok) { db.updateJoinRequest(req); db.updateGroup(req.getGroup()); }
        return ok;
    }

    // FR 3.1.18 – Decline Join Request
    public boolean declineJoinRequest(int requestId) {
        JoinRequest req = db.getJoinRequestById(requestId);
        if (req == null) return false;
        boolean ok = ops.declineJoinRequest(req);
        if (ok) db.updateJoinRequest(req);
        return ok;
    }

    // FR 3.1.19 – Retrieve Pending Requests
    public List<JoinRequest> getPendingRequests(int groupId) {
        Group group = db.getGroupById(groupId);
        if (group == null) return Collections.emptyList();
        return db.getPendingJoinRequests(group);
    }

    // Request to join (creates JoinRequest)
    public JoinRequest requestToJoin(User user, int groupId) {
        Group group = db.getGroupById(groupId);
        if (group == null) return null;
        JoinRequest req = ops.requestToJoin(user, group);
        if (req != null) db.createJoinRequest(req);
        return req;
    }

    // FR 3.1.3 – Filtered Games List
    public List<Game> getFilteredGamesList(int groupId) {
        Group group = db.getGroupById(groupId);
        if (group == null) return Collections.emptyList();
        return ops.getFilteredGamesList(group);
    }

    // FR 3.1.20 – Set Playtime Requirement
    public boolean setPlaytimeRequirement(int groupId, int minHours) {
        Group group = db.getGroupById(groupId);
        if (group == null) return false;
        ops.setPlaytimeRequirement(group, minHours);
        db.updateGroup(group);
        return true;
    }

    // FR 3.1.16 – Session History
    public Set<GroupSession> getSessionHistory(int groupId) {
        Group group = db.getGroupById(groupId);
        if (group == null) return Collections.emptySet();
        return ops.retrieveGroupSessionHistory(group);
    }

    // FR 3.1.30 – Schedule Session
    public GroupSession scheduleSession(int groupId, int gameId, LocalDateTime when, int durationMinutes) {
        Group group = db.getGroupById(groupId);
        if (group == null) return null;
        Game game = gameId > 0 ? db.getGameById(gameId) : null;
        GroupSession session = ops.scheduleSession(group, game, when, durationMinutes);
        if (session != null) db.createGroupSession(session);
        return session;
    }

    // FR 3.1.31 – Group Calendar
    public List<GroupSession> getCalendar(int groupId) {
        Group group = db.getGroupById(groupId);
        if (group == null) return Collections.emptyList();
        return ops.getGroupCalendar(group);
    }

    // FR 3.1.23 – Randomize
    public List<Game> randomize(int groupId) {
        Group group = db.getGroupById(groupId);
        if (group == null) return Collections.emptyList();
        return ops.randomizeGameList(group.getGames());
    }

    // FR 3.1.28 – Apply Group Preference
    public boolean applyPreference(int groupId, String genre) {
        Group group = db.getGroupById(groupId);
        if (group == null) return false;
        ops.applyGroupPreference(group, genre);
        db.updateGroup(group);
        return true;
    }

    // FR 3.1.29 – Share Games List
    public String shareGamesList(int groupId) {
        Group group = db.getGroupById(groupId);
        if (group == null) return null;
        String link = ops.shareGamesList(group);
        db.updateGroup(group);
        return link;
    }

    // Shared games list by token (public, no auth)
    public List<Game> getSharedGamesList(String token) {
        Group group = db.getGroupByShareToken(token);
        if (group == null) return null;
        return ops.getFilteredGamesList(group);
    }

    // Voting
    public GroupVote createVote(Group group, User user) {
        GroupVote vote = new GroupVote(group, user);
        db.createGroupVote(vote);
        return vote;
    }

    public boolean castVote(int voteId, int gameId) {
        GroupVote vote = db.getGroupVoteById(voteId);
        Game game = db.getGameById(gameId);
        if (vote == null || game == null) return false;
        boolean ok = ops.castVote(vote, game);
        if (ok) db.updateGroupVote(vote);
        return ok;
    }

    public Map<Game, Integer> tallyVotes(int groupId) {
        Group group = db.getGroupById(groupId);
        if (group == null) return Collections.emptyMap();
        return GroupOperations.tallyVotes(db.getGroupVotesByGroup(group));
    }
}
