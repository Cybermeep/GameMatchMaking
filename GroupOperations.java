/*
    GroupOperations class
*/

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    //todo group operations [MORE METHODS HERE]











    //methods from old groupvote
    /*
    records vote for given game along with timestamp. logic is to only cast once and update with updateVote.
    @param selectedGame - selectedGame to vote for
    @return boolean
    */
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


    /*
    changes existing vote to different game and updates timestamp.
    @param newGame - newGame to vote for
    @return boolean
     */
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
        System.out.println("Vote Tally:");
        for(Map.Entry<Game, Integer> entry : tally.entrySet()){
            System.out.println(entry.getKey().getGameName() + ": " + entry.getValue() + " vote(s)");
        }
        return tally;
    }

    /*
    determines winner from tally
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
            System.out.println("Tie has occurred");
            return null;
        }
        return winner;
    }    

}