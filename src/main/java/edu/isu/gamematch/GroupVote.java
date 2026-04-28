package edu.isu.gamematch;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "group_votes")
public class GroupVote{
    @Id
@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "group_vote_seq")
@SequenceGenerator(name = "group_vote_seq", sequenceName = "GROUP_VOTE_SEQ", allocationSize = 1)
@Column(name = "vote_id")
private int voteID;

    @ManyToOne
    @JoinColumn(name = "game_id")
    private Game game;

    @ManyToOne
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @Column(name = "vote_time")
    private Timestamp voteTime;

    @Column(name = "timestamp")
    private Timestamp timestamp;

    @ManyToOne
    @JoinColumn(name = "voted_by_user_id", nullable = false)
    private User votedByUser;

    public GroupVote(){}

    public GroupVote(Group group, User votedByUser){
        this.group = group;
        this.votedByUser = votedByUser;
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

    public Timestamp getVoteTime() {
        return voteTime;
    }

    public void setVoteTime(Timestamp voteTime) {
        this.voteTime = voteTime;
    }

    public Timestamp getTimestamp(){
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp){
        this.timestamp = timestamp;
    }

    public User getVotedByUser(){
        return votedByUser;
    }

    public void setVotedByUser(User votedByUser){
        this.votedByUser = votedByUser;
    }

    public int getVotedByUserID(){
        return votedByUser != null ? votedByUser.getUserID() : 0;
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
    public boolean updateVote(Game newGame){
        if(this.game == null){
            System.out.println("No vote to update. Use castVote() first.");
            return false;
        }
        if(newGame == null){
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
        Map<Game, Integer> tally = new HashMap<>();
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
            System.out.println("Tie has occurred"); //single responsbility (dont handle ties here)
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
        return "GroupVote{" + "voteID=" + voteID + ", votedByUserID=" + votedByUser + ", game=" + gameLabel + ", timestamp=" + timestamp + "}";
    }
}
