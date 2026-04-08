/*
 * GroupVote Class
 * 
 */
package edu.isu.gamematch;
import java.sql.Timestamp;

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



    @Override
    public String toString(){
        String gameLabel = "";
        if(game != null){
            gameLabel = game.getGameName();
        }
        return "GroupVote{" + "voteID=" + voteID + ", votedByUserID=" + votedByUserID + ", game=" + gameLabel + ", timestamp=" + timestamp + "}";
    }
}
