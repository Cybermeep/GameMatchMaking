/*
    GroupOperations class
*/

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.time.LocalDateTime;

/**
 * Manages operations on the Group class, including its related classes like GroupSession, GroupVote, etc...
 */
public class GroupOperations{
    private List<Game> gameList;

    public GroupOperations(){
        this.gameList = new ArrayList<Game>();
    }
    public GroupOperations(List<Game> gameList){
        this.gameList = gameList;
    }

    public List<Game> getGameList(){
        return gameList;
    }
    public void setGameList(List<Game> gameList){
        this.gameList = gameList;
    }

    //methods

    /**
     * Generates a unique invite link for the group that can be shared with others to join the group.
     * @return a unique invite link as a String
     */
    public String generateGroupInviteLink() {
        return java.util.UUID.randomUUID().toString();
    }

    // ==================================================
    //              GAME RANKING OPERATIONS
    // ==================================================
    
    /**
     * Scores each game based on total hours, recent hours, and days since last played
     * and stores the scores in a HashMap, then uses that to sort the groups games list.
     */
    public void rankList(Group group) {
        // Simplified implementation for demo
        Map<Game, Double> gameScores = new HashMap<>();
        List<Game> games = group.getGames();

        for (Game game : games) {
            // Simple scoring based on playtime
            double score = game.getPlaytime() / 60.0; // Convert to hours
            gameScores.put(game, score);
        }

        // Sort games list based on scores, then alphabetically for ties
        games.sort(Comparator.comparingDouble((Game game) -> gameScores.get(game))
                             .reversed()
                             .thenComparing(Game::getGameName));
    }

    // ==================================================
    //             GROUP SESSION OPERATIONS
    // ==================================================

    /**
     * Returns a set of all sessions for the provided group.
     */
    public Set<GroupSession> retrieveGroupSessionHistory(Group group) {
        return group.getSessions();
    }

    /**
     * Cancels a scheduled session by setting its active status to false.
     */
    public GroupSession cancelSession(Group group, GroupSession session) {
        session.setActive(false);
        return session;
    }

    // ==================================================
    //               GROUP VOTE OPERATIONS
    // ==================================================
    
    public boolean castVote(GroupVote vote, Game selectedGame){
        if(vote == null){
            return false;
        }
        if(vote.getGame() != null){
            System.out.println("Vote already cast. Use updateVote() to change it.");
            return false;
        }
        if(selectedGame == null){
            System.out.println("Game not found");
            return false;
        }
        vote.setGame(selectedGame);
        vote.setTimestamp(new Timestamp(System.currentTimeMillis()));
        System.out.println("Vote cast for: " + selectedGame.getGameName());
        return true;
    }

    public boolean updateVote(GroupVote vote, Game newGame){
        if(vote == null){
            return false;
        }
        if(vote.getGame() == null){
            System.out.println("No vote to update. Use castVote() first.");
            return false;
        }
        if(newGame == null){
            System.out.println("Game not found");
            return false;
        }
        System.out.println("Vote changed from '" + vote.getGame().getGameName()+ "' to '" + newGame.getGameName() + "'.");
        vote.setGame(newGame);
        vote.setTimestamp(new Timestamp(System.currentTimeMillis()));
        return true;
    }

    public static Map<Game, Integer> tallyVotes(List<GroupVote> votes){
        Map<Game, Integer> tally = new HashMap<Game,Integer>();
        if(votes == null || votes.isEmpty()){
            System.out.println("No votes to tally");
            return tally;
        }
        for(int i = 0; i < votes.size(); i++){
            GroupVote vote = votes.get(i);
            if(vote.getGame() != null){
                Game votedGame = vote.getGame();
                if(tally.containsKey(votedGame)){
                    tally.put(votedGame, tally.get(votedGame) + 1);
                }
                else{
                    tally.put(votedGame, 1);
                }
            }
        }
        System.out.println("Vote Tally:");
        for(Map.Entry<Game, Integer> entry : tally.entrySet()){
            System.out.println(entry.getKey().getGameName() + ": " + entry.getValue() + " vote(s)");
        }
        return tally;
    }

    public static Game getWinner(Map<Game, Integer> tally){
        if(tally == null || tally.isEmpty()){
            return null;
        }

        Game winner = null;
        int topCount = 0;
        boolean tie = false;

        for(Map.Entry<Game, Integer> entry : tally.entrySet()){
            int count = entry.getValue();
            if(count > topCount){
                topCount = count;
                winner = entry.getKey();
                tie = false;
            }
            else if(count == topCount){
                tie = true;
            }
        }

        if(tie){
            System.out.println("Tie has occurred");
            return null;
        }
        return winner;
    }    

    public Group deleteGroup(Group group){
        if(group == null){
            return null;
        }
        group.deleteGroup();
        return group;
    }

    public User transferGroupOwnership(User newOwner, Group group){
        if(newOwner == null || group == null){
            return null;
        }
        if(!group.getMembers().contains(newOwner)){
            System.out.println("transferGroupOwnership: user is not a group member.");
            return null;
        }
        group.setGroupOwner(newOwner);
        return newOwner;
    }

    public User removeGroupMember(User member, Group group){
        if(member == null || group == null) {
            return null;
        }
        if(!group.getMembers().contains(member)) {
            System.out.println("removeGroupMember: user is not a group member.");
            return null;
        }
        group.removeGroupMember(member);
        return member;
    }

    public boolean addGroupMember(User member, Group group){
        if(member == null || group == null){
            return false;
        }
        group.addGroupMember(member);
        return true;
    }

    public List<Game> randomizeGameList(List<Game> gameList){
        if(gameList == null){
            return new ArrayList<Game>();
        }
        List<Game> shuffled = new ArrayList<Game>(gameList);
        Collections.shuffle(shuffled);
        return shuffled;
    }

    public User initiateGameSelectionVote(GroupVote vote, User user){
        if(vote == null || user == null){
            return null;
        }
        vote.setVotedByUser(user);
        return user;
    }
}