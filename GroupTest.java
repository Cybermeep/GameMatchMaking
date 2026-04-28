

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class GroupTest{
    private User user1;
    private User user2;
    private User user3;

    private Game game1; 
    private Game game2;
    private Game game3;

    private Achievement ach1;
    private Achievement ach2;
    private Achievement ach3;

    private Group group;
    private GroupOperations groupOps;

    @BeforeEach
    public void setUp() {
        groupOps = new GroupOperations();

        user1 = new User();
        user1.setUserID(1);
        user1.setSteamID(1000L);

        user2 = new User();
        user2.setUserID(2);
        user2.setSteamID(2000L);

        user3 = new User();
        user3.setUserID(3);
        user3.setSteamID(3000L);

        game1 = new Game();
        game1.setGameID(101);
        game1.setGameName("Tetris");
        game1.setPlaytimeForever(300);
        game1.setPlaytimeLastTwoWeeks(120);

        game2 = new Game();
        game2.setGameID(102);
        game2.setGameName("Hollow Knight");
        game2.setPlaytimeForever(180);
        game2.setPlaytimeLastTwoWeeks(60);

        game3 = new Game();
        game3.setGameID(103);
        game3.setGameName("Celeste");
        game3.setPlaytimeForever(60);
        game3.setPlaytimeLastTwoWeeks(0);

        ach1 = new Achievement("Too Fast", "Completed 40L in under a minute");
        ach2 = new Achievement("True Hunter", "Defeated all bosses");
        ach3 = new Achievement("Summit", "Reached the summit");

        GameAchievement ga1 = new GameAchievement(game1, ach1, user1);
        GameAchievement ga2 = new GameAchievement(game2, ach2, user2);
        GameAchievement ga3 = new GameAchievement(game3, ach3, user3);

        game1.addGameAchievement(ga1);
        game2.addGameAchievement(ga2);
        game3.addGameAchievement(ga3);

        group = new Group();
        group.setGroupID(1);
        group.setGroupOwner(user1);
        group.addGroupMember(user1);
        group.addGroupMember(user2);
        group.addGroupMember(user3);

        group.addGame(game1);
        group.addGame(game2);
        group.addGame(game3);
    }

    /**
     * create group with owner and verify membership
     */
    @Test
    public void testGroupCreationWithOwnerAndMembers() {
        assertTrue(group.getMembers().contains(user1), "user1 should be a member");
        assertTrue(group.getMembers().contains(user2), "user2 should be a member");
        assertTrue(group.getMembers().contains(user3), "user3 should be a member");
        assertEquals(user1, group.getGroupOwner(), "user1 should be the group owner");
        assertEquals(3, group.getMembers().size(), "Group should have exactly 3 members");
    }

    /**
     *  adds a new member successfully
     */
    @Test
    public void testAddGroupMember() {
        User newUser = new User();
        newUser.setUserID(4);

        boolean result = groupOps.addGroupMember(newUser, group);

        assertTrue(result, "addGroupMember should return true on success");
        assertTrue(group.getMembers().contains(newUser), "newUser should now be in group");
        assertEquals(4, group.getMembers().size());
    }

    /**
     * addGroupMember null arguments
     */
    @Test
    public void testAddGroupMemberNullArguments() {
        boolean nullMember = groupOps.addGroupMember(null, group);
        boolean nullGroup  = groupOps.addGroupMember(user1, null);

        assertFalse(nullMember, "null member should return false");
        assertFalse(nullGroup,  "null group should return false");
    }

    /**
     * remove an existing member
     */
    @Test
    public void testRemoveGroupMember() {
        User removed = groupOps.removeGroupMember(user3, group);

        assertEquals(user3, removed, "should return the removed user");
        assertFalse(group.getMembers().contains(user3), "user3 should no longer be in group");
        assertEquals(2, group.getMembers().size());
    }

    /**
     * remove a user who is not a member of the group
     */
    @Test
    public void testRemoveGroupMemberNotInGroup() {
        User outsider = new User();
        outsider.setUserID(99);

        User result = groupOps.removeGroupMember(outsider, group);

        assertNull(result, "removing non-member should return null");
        assertEquals(3, group.getMembers().size(), "group size should be unchanged");
    }

    /**
     * transfer ownership of group
     */
    @Test
    public void testTransferGroupOwnership() {
        User newOwner = groupOps.transferGroupOwnership(user2, group);

        assertEquals(user2, newOwner, "should return the new owner");
        assertEquals(user2, group.getGroupOwner(), "group owner should now be user2");
    }

    /**
     * transfer ownership of group to nonmember 
     */
    @Test
    public void testTransferGroupOwnershipToNonMember() {
        User outsider = new User();
        outsider.setUserID(99);

        User result = groupOps.transferGroupOwnership(outsider, group);

        assertNull(result, "transferring to non-member should return null");
        assertEquals(user1, group.getGroupOwner(), "owner should be unchanged");
    }

    /**
     * delete group
     */
    @Test
    public void testDeleteGroup() {
        Group deleted = groupOps.deleteGroup(group);

        assertNotNull(deleted, "deleteGroup should return the group");
        assertEquals(0, deleted.getGroupID(), "groupID should be cleared to 0");
        assertNull(deleted.getGroupOwner(), "owner should be null after delete");
    }

    /**
     * delete group with null
     */
    @Test
    public void testDeleteGroupNull() {
        Group result = groupOps.deleteGroup(null);
        assertNull(result, "null input should return null");
    }

}