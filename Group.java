/*
* Srida Kalidindi
* A skeleton for the 'Group' class.
*/
package edu.isu.gamematch;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.*;
@Entity
@Table(name = "groups")
public class Group
{
    //attributes
    int groupID;
    Set<User> members;
    List<Game> games;
    Set<GroupSession> sessions;
    User groupOwner;

    //constructor
    public Group(int ID, User owner)
    {
        this.groupID = ID;
        this.groupOwner = owner;
        this.games = new ArrayList<Game>();
        this.members = new LinkedHashSet<User>();
        this.sessions = new LinkedHashSet<GroupSession>();
    }

    //additional methods
    public void createGroup()
    {
    }
    public void deleteGroup()
    {
        this.groupID = 0;
        this.groupOwner = null;
        this.games = null;
        this.members = null;
    }

    // getters and setters, excluding setters for groupID since that should be immutable after creation
    public int getGroupID() {
        return groupID;
    }
    public Set<User> getMembers() {
        return members;
    }
    public User addGroupMember(User member)
    {
        members.add(member);
        return member;
    }
    public User removeGroupMember(User member)
    {
        members.remove(member);
        return member;
    }
    public List<Game> getGames() {
        return games;
    }
    public List<Game> setGames(List<Game> games) {
        this.games = games;
        return games;
    }
    public Game addGame(Game game)
    {
        games.add(game);
        return game;
    }
    public Game removeGame(Game game)
    {
        games.remove(game);
        return game;
    }
    public Set<GroupSession> getSessions() {
        return sessions;
    }
    public GroupSession addGroupSession(GroupSession session)
    {
        sessions.add(session);
        return session;
    }
    public GroupSession removeGroupSession(GroupSession session)
    {
        sessions.remove(session);
        return session;
    }
    public User getGroupOwner() {
        return groupOwner;
    }
    public User setGroupOwner(User newOwner)
    {
        this.groupOwner = newOwner;
        return newOwner;
    }
}
