package edu.isu.gamematch;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "groups")
public class Group
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_id")
    private int groupID;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    private User groupOwner;

    @ManyToMany
    @JoinTable(
        name = "group_members",
        joinColumns = @JoinColumn(name = "group_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<User> members = new ArrayList<>();

    @ManyToMany
    @JoinTable(
        name = "group_games",
        joinColumns = @JoinColumn(name = "group_id"),
        inverseJoinColumns = @JoinColumn(name = "game_id")
    )
    private List<Game> games = new ArrayList<>();

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GroupSession> sessions = new ArrayList<>();

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GroupVote> votes = new ArrayList<>();

    // Default constructor required by JPA
    public Group() {
    }

    //constructor
    public Group(User owner)
    {
        this.groupOwner = owner;
    }

    //additional methods
    /*
    public void createGroup()
    {
    }
    */

    public void deleteGroup()
    {
        this.groupID = 0;
        this.groupOwner = null;
        this.games = null;
        this.members = null;
    }

    public boolean transferGroupOwnership(User newOwner)
    {
        this.groupOwner = newOwner;
        return true;
    }

    public String generateGroupInviteLink()
    {
        return "";
    }

    public boolean removeGroupMember(User member)
    {
        if(members.contains(member))
        {
            members.remove(member);
            return true;
        }
        else
        {
            return false;
        }
    }

    public boolean addGroupMember(User member)
    {
        members.add(member);
        return true;
    }

    // Getters and setters
    public int getGroupID() {
        return groupID;
    }

    public void setGroupID(int groupID) {
        this.groupID = groupID;
    }

    public User getGroupOwner() {
        return groupOwner;
    }

    public void setGroupOwner(User groupOwner) {
        this.groupOwner = groupOwner;
    }

    public List<User> getMembers() {
        return members;
    }

    public void setMembers(List<User> members) {
        this.members = members;
    }

    public List<Game> getGames() {
        return games;
    }

    public void setGames(List<Game> games) {
        this.games = games;
    }

    public List<GroupSession> getSessions() {
        return sessions;
    }

    public void setSessions(List<GroupSession> sessions) {
        this.sessions = sessions;
    }

    public void addSession(GroupSession session) {
        sessions.add(session);
        session.setGroup(this);
    }

    public void removeSession(GroupSession session) {
        sessions.remove(session);
        session.setGroup(null);
    }

    public List<GroupVote> getVotes() {
        return votes;
    }

    public void setVotes(List<GroupVote> votes) {
        this.votes = votes;
    }

    public void addVote(GroupVote vote) {
        votes.add(vote);
        vote.setGroup(this);
    }

    public void removeVote(GroupVote vote) {
        votes.remove(vote);
        vote.setGroup(null);
    }

    public void addGame(Game game) {
        if (!games.contains(game)) {
            games.add(game);
        }
    }

    public void removeGame(Game game) {
        games.remove(game);
    }

    //add games?
    //remove games?
}
