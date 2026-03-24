/*
 * GroupVote Class
 * 
 */

import java.util.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GroupVote{
    private int voteID; 
    private Game game;
    private Group group;
    private Timestamp timestamp;
    private int votedByUserID;

    public GroupVote(){}

    public GroupVote(int voteID, Group group, int votedByUserID){
        this.voteID = voteID;
        this.group = group;
        this.votedByUserID = votedByUserID;
    }

    //getters and setters
    public int getVoteID(){
        return voteID;
    }

    public void setVoteID(int voteID){
        this.voteID = voteID;
    }

    public Game getGame(){
        return game;
    }

    public  void setGame(Game game){
        this.game = game;
    }

    public Group getGroup(){
        return group;
    }

    public void setGroup(Group group){
        this.group = group;
    }

    public Timestamp getTimestamp(){
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp){
        this.timestamp = timestamp;
    }

    public int getVotedByUserID(){
        return votedByUserID;
    }
    
    public void setVotedByUserID(int votedByUserID){
        this.votedByUserID = votedByUserID;
    }

    //methods


    /*
    records vote for given game along with timestamp. logic is to only cast once and update with updateVote.
    @param selectedGame - selectedGame to vote for
    @return boolean
    */
    public boolean castVote(Game selectedGame){
        if(this.game != null){
            System.out.println("Vote already cast. Use updateVote() to change it.");
            return false;
        }
        if(selectedGame == null){
            System.out.println("Game not found");
            return false;
        }
        this.game = selectedGame;
        this.timestamp = new Timestamp(System.currentTimeMillis());
        System.out.println("Vote cast for: " + selectedGame.getGameName());
        return true;
    }


    /*
    changes existing vote to different game and updates timestamp.
    @param newGame - newGame to vote for
    @return boolean
     */
    public boolean updateVote(){
        if(this.game != null){
            System.out.println("No vote to update. Use castVote() first.");
            return false;
        }
        if(selectedGame == null){
            System.out.println("Game not found");
            return false;
        }
        System.out.println("Vote updated from '" + this.game.getGameName()+ "' to '" + newGame.getGameName() + "'.");
        this.game = newGame;
        this.timestamp = new Timestamp(System.currentTimeMillis());
        return true;
    }

    /*
    tallies all votes in list, returns map of Game to vote count
    @param votes - list of GroupVotes to tally
    @return Map
     */
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
        //writing these console outputs as a base in case our web app falls through
        System.out.println("Vote Tally:");
        for(Map.Entry<Game, Integer> entry : tally.entrySet()){
            System.out.println(entry.getKey().getGameName() + ": " + entry.getValue() + " vote(s)");
        }
        return tally;
    }

    /*
    determines winner from tally
    how do i handle ties
    @param tally - result of tallyVotes
    @return Game
    */
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
            System.out.println("Tie has occurred. Do something."); //should we do a revote of the tied games or something else idk i prob have to rewrite later
            return null;
        }
        return winner;
    }

    @Override
    public String toString(){
        String gameLabel = "";
        if(game != null){
            gameLabel = game.getGameName();
        }
        return "GroupVote{" + "voteID=" + voteID + ", votedByUserID=" + votedByUserID + ", game=" + gameLabel + ", timestamp=" + timestamp + "}";
    }
}
