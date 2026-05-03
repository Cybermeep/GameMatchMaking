package edu.isu.gamematch;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages game ranking, voting, session, and now library intersection for groups.
 */
public class GroupOperations {

    private List<Game> gameList;

    public GroupOperations() {
        this.gameList = new ArrayList<>();
    }

    public GroupOperations(List<Game> gameList) {
        this.gameList = gameList;
    }

    public List<Game> getGameList() {
        return gameList;
    }

    public void setGameList(List<Game> gameList) {
        this.gameList = gameList;
    }

    // ==================== INVITE ====================
    public String generateGroupInviteLink() {
        return java.util.UUID.randomUUID().toString();
    }

    // ==================== RANKING (now with genre weighting Fix 5.2) ====================
    /**
     * Scores games by playtime and genre preference of the given user.
     * Sorted descending by score, then alphabetically by name.
     */
    public void rankList(Group group, User user, SQLHandler sqlHandler) {
        List<Game> games = group.getGames();
        if (games == null || games.isEmpty()) return;

        List<String> favoriteGenres = user.getUserProfile() != null
                ? user.getUserProfile().getFavoriteGenres()
                : new ArrayList<>();

        Map<Game, Double> gameScores = new HashMap<>();
        for (Game game : games) {
            double base = game.getPlaytime() / 60.0; // hours
            double bonus = 0.0;
            if (favoriteGenres.contains(game.getGenre())) {
                bonus = 10.0; // genre match bonus
            }
            gameScores.put(game, base + bonus);
        }

        games.sort(Comparator.<Game, Double>comparing(gameScores::get)
                .reversed()
                .thenComparing(Game::getGameName));
    }

    // ==================== VOTING (existing static methods) ====================
    public static Map<Game, Integer> tallyVotes(List<GroupVote> votes) {
        Map<Game, Integer> tally = new HashMap<>();
        if (votes == null) return tally;
        for (GroupVote v : votes) {
            if (v.getGame() != null) {
                tally.merge(v.getGame(), 1, Integer::sum);
            }
        }
        return tally;
    }

    public static Game getWinner(Map<Game, Integer> tally) {
        if (tally == null || tally.isEmpty()) return null;
        return tally.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    // ==================== SESSION ====================
    public Set<GroupSession> retrieveGroupSessionHistory(Group group) {
        return group.getSessions();
    }

    public GroupSession cancelSession(Group group, GroupSession session) {
        session.setActive(false);
        return session;
    }

    // ==================== GROUP MEMBERSHIP ====================
    public User transferGroupOwnership(User newOwner, Group group) {
        if (newOwner != null && group != null && group.getMembers().contains(newOwner)) {
            group.setGroupOwner(newOwner);
            return newOwner;
        }
        return null;
    }

    public User removeGroupMember(User member, Group group) {
        if (member != null && group != null && group.getMembers().contains(member)) {
            group.removeGroupMember(member);
            return member;
        }
        return null;
    }

    public boolean addGroupMember(User member, Group group) {
        if (member != null && group != null) {
            group.addGroupMember(member);
            return true;
        }
        return false;
    }

    // ==================== RANDOMIZE (3.1.23) ====================
    public List<Game> randomizeGameList(List<Game> games) {
        List<Game> shuffled = new ArrayList<>(games);
        Collections.shuffle(shuffled);
        return shuffled;
    }

    // ==================== LIBRARY INTERSECTION (Fix 5.1) ====================
    /**
     * Computes games owned by ALL members of the group.
     * Uses the database backed achievement data for each member.
     */
   public List<Game> getSharedGames(Group group, SQLHandler sqlHandler) {
    Set<Game> shared = null;
    for (User member : group.getMembers()) {
        List<Game> memberGames = sqlHandler.getDistinctGamesByUser(member);
        if (memberGames.isEmpty()) {
            continue;   // Skip members with no games
        }
        if (shared == null) {
            shared = new HashSet<>(memberGames);
        } else {
            shared.retainAll(memberGames);
        }
        if (shared.isEmpty()) break;
    }
    return shared == null ? new ArrayList<>() : new ArrayList<>(shared);
}

}