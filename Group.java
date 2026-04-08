/*
* Srida Kalidindi
* A skeleton for the 'Group' class.
*/
package edu.isu.gamematch;
import java.util.ArrayList;
import javax.persistence.*;
@Entity
@Table(name = "groups")
public class Group
{
    //attributes
    int groupID;
    ArrayList<User> members;
    ArrayList<Game> games;
    User groupOwner;

    //constructor
    public Group(int ID, User owner)
    {
        this.groupID = ID;
        this.groupOwner = owner;
        this.games = new ArrayList<Game>();
        this.members = new ArrayList<User>();
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

    //add games?
    //remove games?
}
