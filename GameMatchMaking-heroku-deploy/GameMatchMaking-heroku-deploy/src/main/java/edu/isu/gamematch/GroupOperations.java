package edu.isu.gamematch;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages operations on the Group class.
 * Covers FR 3.1.3-3.1.12, 3.1.16-3.1.20, 3.1.23, 3.1.28-3.1.32
 */
public class GroupOperations {
    private List<Game> gameList;

    public GroupOperations() { this.gameList = new ArrayList<>(); }
    public GroupOperations(List<Game> gameList) { this.gameList = gameList; }

    public List<Game> getGameList() { return gameList; }
    public void setGameList(List<Game> gameList) { this.gameList = gameList; }

    // ==================================================
    //  FR 3.1.7 – Generate Group Invite Link
    // ==================================================
    public String generateGroupInviteLink(Group group) {
        if (group.getInviteToken() == null) {
            group.setInviteToken(UUID.randomUUID().toString());
        }
        return "/group/join?token=" + group.getInviteToken();
    }

    // legacy overload
    public String generateGroupInviteLink() {
        return "/group/join?token=" + UUID.randomUUID().toString();
    }

    // ==================================================
    //  FR 3.1.4 – Create Group
    // ==================================================
    public Group createGroup(String name, User owner) {
        Group group = new Group();
        group.setGroupName(name);
        group.setGroupOwner(owner);
        group.setInviteToken(UUID.randomUUID().toString());
        group.addGroupMember(owner);
        return group;
    }

    // ==================================================
    //  FR 3.1.5 – Delete Group
    // ==================================================
    public Group deleteGroup(Group group) {
        if (group == null) return null;
        group.deleteGroup();
        return group;
    }

    // ==================================================
    //  FR 3.1.6 – Transfer Group Ownership
    // ==================================================
    public User transferGroupOwnership(User newOwner, Group group) {
        if (newOwner == null || group == null) return null;
        if (!group.getMembers().contains(newOwner)) {
            System.out.println("transferGroupOwnership: user is not a group member.");
            return null;
        }
        group.setGroupOwner(newOwner);
        return newOwner;
    }

    // ==================================================
    //  FR 3.1.8 – Retrieve Group Members
    // ==================================================
    public Set<User> retrieveGroupMembers(Group group) {
        return group != null ? group.getMembers() : Collections.emptySet();
    }

    // ==================================================
    //  FR 3.1.9 – Join Group (via invite token)
    // ==================================================
    public boolean joinGroupByToken(User user, Group group, String token) {
        if (user == null || group == null || token == null) return false;
        if (!token.equals(group.getInviteToken())) {
            System.out.println("joinGroupByToken: invalid token.");
            return false;
        }
        if (group.getMembers().contains(user)) {
            System.out.println("joinGroupByToken: user already a member.");
            return false;
        }
        group.addGroupMember(user);
        return true;
    }

    // ==================================================
    //  FR 3.1.10 – Leave Group
    // ==================================================
    public boolean leaveGroup(User user, Group group) {
        if (user == null || group == null) return false;
        if (!group.getMembers().contains(user)) {
            System.out.println("leaveGroup: user is not a member.");
            return false;
        }
        if (user.equals(group.getGroupOwner())) {
            System.out.println("leaveGroup: owner must transfer ownership before leaving.");
            return false;
        }
        group.removeGroupMember(user);
        return true;
    }

    // ==================================================
    //  FR 3.1.11 – Remove Group Member
    // ==================================================
    public User removeGroupMember(User member, Group group) {
        if (member == null || group == null) return null;
        if (!group.getMembers().contains(member)) {
            System.out.println("removeGroupMember: user is not a group member.");
            return null;
        }
        group.removeGroupMember(member);
        return member;
    }

    public boolean addGroupMember(User member, Group group) {
        if (member == null || group == null) return false;
        group.addGroupMember(member);
        return true;
    }

    // ==================================================
    //  FR 3.1.17 – Accept Group Join Request
    // ==================================================
    public boolean acceptJoinRequest(JoinRequest request, Group group) {
        if (request == null || group == null) return false;
        if (request.getStatus() != JoinRequest.Status.PENDING) {
            System.out.println("acceptJoinRequest: request already resolved.");
            return false;
        }
        request.setStatus(JoinRequest.Status.ACCEPTED);
        request.setResolvedAt(LocalDateTime.now());
        group.addGroupMember(request.getRequester());
        System.out.println("acceptJoinRequest: " + request.getRequester().getUserID() + " accepted.");
        return true;
    }

    // ==================================================
    //  FR 3.1.18 – Decline Group Join Request
    // ==================================================
    public boolean declineJoinRequest(JoinRequest request) {
        if (request == null) return false;
        if (request.getStatus() != JoinRequest.Status.PENDING) {
            System.out.println("declineJoinRequest: request already resolved.");
            return false;
        }
        request.setStatus(JoinRequest.Status.DECLINED);
        request.setResolvedAt(LocalDateTime.now());
        System.out.println("declineJoinRequest: request declined.");
        return true;
    }

    // ==================================================
    //  FR 3.1.19 – Retrieve Pending Join Requests
    // ==================================================
    public List<JoinRequest> getPendingJoinRequests(Group group) {
        if (group == null) return Collections.emptyList();
        return group.getJoinRequests().stream()
                .filter(r -> r.getStatus() == JoinRequest.Status.PENDING)
                .collect(Collectors.toList());
    }

    // Create a new join request from a user
    public JoinRequest requestToJoin(User user, Group group) {
        if (user == null || group == null) return null;
        if (group.getMembers().contains(user)) {
            System.out.println("requestToJoin: already a member.");
            return null;
        }
        boolean alreadyPending = group.getJoinRequests().stream()
                .anyMatch(r -> r.getRequester().equals(user) && r.getStatus() == JoinRequest.Status.PENDING);
        if (alreadyPending) {
            System.out.println("requestToJoin: pending request already exists.");
            return null;
        }
        JoinRequest req = new JoinRequest(group, user);
        group.getJoinRequests().add(req);
        return req;
    }

    // ==================================================
    //  FR 3.1.3 – Request Filtered Games List
    //  FR 3.1.20 – Set Playtime Requirement
    // ==================================================

    /** FR 3.1.20: Set minimum playtime (hours) for group's game filter. */
    public void setPlaytimeRequirement(Group group, int minHours) {
        if (group == null) return;
        group.setMinPlaytimeMinutes(minHours * 60);
    }

    /**
     * FR 3.1.3: Returns the intersection of all members' games, filtered by the group's
     * minimum playtime and optionally by genre, sorted by total playtime across members.
     */
    public List<Game> getFilteredGamesList(Group group) {
        if (group == null || group.getMembers().isEmpty()) return Collections.emptyList();

        List<Game> intersection = new ArrayList<>(group.getGames());

        // Filter by minimum playtime
        int minPt = group.getMinPlaytimeMinutes();
        if (minPt > 0) {
            intersection.removeIf(g -> g.getPlaytime() < minPt);
        }

        // Filter by preferred genres if set
        String preferred = group.getPreferredGenres();
        if (preferred != null && !preferred.isBlank()) {
            Set<String> genres = Arrays.stream(preferred.split(","))
                    .map(String::trim).map(String::toLowerCase)
                    .collect(Collectors.toSet());
            intersection.removeIf(g -> g.getGenre() == null ||
                    !genres.contains(g.getGenre().toLowerCase()));
        }

        return intersection;
    }

    // ==================================================
    //  FR 3.1.12 – Vote on Game Selection
    // ==================================================
    public boolean castVote(GroupVote vote, Game selectedGame) {
        if (vote == null) return false;
        if (vote.getGame() != null) {
            System.out.println("Vote already cast. Use updateVote() to change it.");
            return false;
        }
        if (selectedGame == null) { System.out.println("Game not found"); return false; }
        vote.setGame(selectedGame);
        vote.setTimestamp(new Timestamp(System.currentTimeMillis()));
        return true;
    }

    public boolean updateVote(GroupVote vote, Game newGame) {
        if (vote == null) return false;
        if (vote.getGame() == null) { System.out.println("No vote to update."); return false; }
        if (newGame == null) { System.out.println("Game not found"); return false; }
        vote.setGame(newGame);
        vote.setTimestamp(new Timestamp(System.currentTimeMillis()));
        return true;
    }

    public static Map<Game, Integer> tallyVotes(List<GroupVote> votes) {
        Map<Game, Integer> tally = new HashMap<>();
        if (votes == null || votes.isEmpty()) return tally;
        for (GroupVote vote : votes) {
            if (vote.getGame() != null) {
                tally.merge(vote.getGame(), 1, Integer::sum);
            }
        }
        return tally;
    }

    public static Game getWinner(Map<Game, Integer> tally) {
        if (tally == null || tally.isEmpty()) return null;
        Game winner = null;
        int topCount = 0;
        boolean tie = false;
        for (Map.Entry<Game, Integer> e : tally.entrySet()) {
            if (e.getValue() > topCount) { topCount = e.getValue(); winner = e.getKey(); tie = false; }
            else if (e.getValue() == topCount) { tie = true; }
        }
        return tie ? null : winner;
    }

    public User initiateGameSelectionVote(GroupVote vote, User user) {
        if (vote == null || user == null) return null;
        vote.setVotedByUser(user);
        return user;
    }

    // ==================================================
    //  FR 3.1.16 – Retrieve Group Session History
    // ==================================================
    public Set<GroupSession> retrieveGroupSessionHistory(Group group) {
        return group != null ? group.getSessions() : Collections.emptySet();
    }

    public GroupSession cancelSession(Group group, GroupSession session) {
        session.setActive(false);
        return session;
    }

    // ==================================================
    //  FR 3.1.30 – Schedule Group Game Session
    // ==================================================
    public GroupSession scheduleSession(Group group, Game game, LocalDateTime scheduledDate, int durationMinutes) {
        if (group == null || scheduledDate == null) return null;
        GroupSession session = new GroupSession();
        session.setGroup(group);
        session.setGame(game);
        session.setScheduledDate(scheduledDate);
        session.setDuration(durationMinutes);
        session.setActive(true);
        group.addGroupSession(session);
        System.out.println("Session scheduled for group " + group.getGroupID() + " on " + scheduledDate);
        return session;
    }

    // ==================================================
    //  FR 3.1.31 – Retrieve Group Calendar
    // ==================================================
    public List<GroupSession> getGroupCalendar(Group group) {
        if (group == null) return Collections.emptyList();
        return group.getSessions().stream()
                .filter(s -> s.getScheduledDate() != null && s.isActive())
                .sorted(Comparator.comparing(GroupSession::getScheduledDate))
                .collect(Collectors.toList());
    }

    // ==================================================
    //  FR 3.1.23 – Randomize Games List
    // ==================================================
    public List<Game> randomizeGameList(List<Game> gameList) {
        if (gameList == null) return new ArrayList<>();
        List<Game> shuffled = new ArrayList<>(gameList);
        Collections.shuffle(shuffled);
        return shuffled;
    }

    // ==================================================
    //  FR 3.1.28 – Add Group Preference (voted)
    // ==================================================
    /**
     * Proposes a genre preference. Returns a GroupVote for all members to approve.
     * Once all votes pass, the caller should call applyGroupPreference().
     */
    public GroupVote proposeGroupPreference(Group group, User proposer, String genre) {
        if (group == null || proposer == null || genre == null) return null;
        GroupVote pref = new GroupVote(group, proposer);
        // Store the proposed genre in the vote as a placeholder game name (game field left null)
        // In production, a separate GroupPreferenceVote entity would be cleaner
        System.out.println("Preference '" + genre + "' proposed by user " + proposer.getUserID() +
                " — members must vote to approve.");
        return pref;
    }

    public void applyGroupPreference(Group group, String genre) {
        if (group == null || genre == null || genre.isBlank()) return;
        String current = group.getPreferredGenres() == null ? "" : group.getPreferredGenres();
        Set<String> genres = new LinkedHashSet<>(Arrays.asList(current.split(",")));
        genres.remove("");
        genres.add(genre.trim());
        group.setPreferredGenres(String.join(",", genres));
    }

    // ==================================================
    //  FR 3.1.29 – Share Final Games List (permalink)
    // ==================================================
    public String shareGamesList(Group group) {
        if (group == null) return null;
        if (group.getShareToken() == null) {
            group.setShareToken(UUID.randomUUID().toString());
        }
        return "/shared/games?token=" + group.getShareToken();
    }

    // ==================================================
    //  FR 3.1.13 – Ranking (used by filtered list)
    // ==================================================
    public void rankList(Group group) {
        Map<Game, Double> scores = new HashMap<>();
        List<Game> games = group.getGames();
        for (Game game : games) {
            scores.put(game, (double) game.getPlaytime() / 60.0);
        }
        games.sort(Comparator.comparingDouble((Game g) -> scores.get(g))
                             .reversed()
                             .thenComparing(Game::getGameName));
    }
}
