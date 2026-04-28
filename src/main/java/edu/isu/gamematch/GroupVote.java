package edu.isu.gamematch;

import javax.persistence.*;
import java.sql.Timestamp;


@Entity
@Table(name = "group_votes")
public class GroupVote{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vote_id")
    private int voteID;

    @ManyToOne
    @JoinColumn(name = "game_id")
    private Game game;

    @ManyToOne
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

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

    @Override
    public String toString(){
        String gameLabel = "";
        if(game != null){
            gameLabel = game.getGameName();
        }
        return "GroupVote{" + "voteID=" + voteID + ", votedByUserID=" + votedByUser + ", game=" + gameLabel + ", timestamp=" + timestamp + "}";
    }
}
