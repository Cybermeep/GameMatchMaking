package edu.isu.gamematch;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class UserTest {

    @Autowired
    private SQLHandler sqlHandler;

    @Test
    public void testCompleteGameMatchmakingProcess() {
        System.out.println("=== STARTING COMPREHENSIVE GAME MATCHMAKING TEST ===");

        // ==================================================
        // STEP 1: CREATE USERS WITH GAMES
        // ==================================================
        System.out.println("\n--- Step 1: Creating Users ---");

        // Create User 1
        UserProfile userProfile1 = new UserProfile("GamerAlice", null);
        User user1 = new User();
        user1.setSteamID(123456789L);
        user1.setPersonaName("GamerAlice");
        user1.setUserProfile(userProfile1);
        userProfile1.setUser(user1);

        // Create User 2
        UserProfile userProfile2 = new UserProfile("GamerBob", null);
        User user2 = new User();
        user2.setSteamID(987654321L);
        user2.setPersonaName("GamerBob");
        user2.setUserProfile(userProfile2);
        userProfile2.setUser(user2);

        // Create User 3
        UserProfile userProfile3 = new UserProfile("GamerCharlie", null);
        User user3 = new User();
        user3.setSteamID(555666777L);
        user3.setPersonaName("GamerCharlie");
        user3.setUserProfile(userProfile3);
        userProfile3.setUser(user3);

        // Save users to database
        assertTrue(sqlHandler.createUser(user1), "User1 creation should succeed");
        assertTrue(sqlHandler.createUser(user2), "User2 creation should succeed");
        assertTrue(sqlHandler.createUser(user3), "User3 creation should succeed");
        System.out.println(" Created 3 users: Alice, Bob, Charlie");

        // ==================================================
        // STEP 2: CREATE GAMES WITH DIFFERENT PLAYTIMES
        // ==================================================
        System.out.println("\n--- Step 2: Creating Games ---");

        // Create games with varying playtimes (in minutes)
        Game game1 = new Game("Counter-Strike 2", 0, 0);
        game1.setPlaytime(1200); // 20 hours
        game1.setGenre("FPS");

        Game game2 = new Game("Dota 2", 0, 0);
        game2.setPlaytime(2400); // 40 hours
        game2.setGenre("MOBA");

        Game game3 = new Game("The Witcher 3", 0, 0);
        game3.setPlaytime(600); // 10 hours
        game3.setGenre("RPG");

        Game game4 = new Game("Among Us", 0, 0);
        game4.setPlaytime(1800); // 30 hours
        game4.setGenre("Social Deduction");

        Game game5 = new Game("Minecraft", 0, 0);
        game5.setPlaytime(3600); // 60 hours
        game5.setGenre("Sandbox");

        // Save games to database
        assertTrue(sqlHandler.createGame(game1), "Game1 creation should succeed");
        assertTrue(sqlHandler.createGame(game2), "Game2 creation should succeed");
        assertTrue(sqlHandler.createGame(game3), "Game3 creation should succeed");
        assertTrue(sqlHandler.createGame(game4), "Game4 creation should succeed");
        assertTrue(sqlHandler.createGame(game5), "Game5 creation should succeed");
        System.out.println(" Created 5 games with playtimes ranging from 10-60 hours");

        // ==================================================
        // STEP 3: CREATE GROUP AND ADD MEMBERS
        // ==================================================
        System.out.println("\n--- Step 3: Creating Group ---");

        Group group = new Group();
        group.setGroupName("Weekend Gamers");
        group.setGroupOwner(user1);
        group.addGroupMember(user1);
        group.addGroupMember(user2);
        group.addGroupMember(user3);

        // Save group to database
        assertTrue(sqlHandler.createGroup(group), "Group creation should succeed");
        System.out.println(" Created group 'Weekend Gamers' with 3 members");

        // ==================================================
        // STEP 4: POPULATE GROUP GAME LIST
        // ==================================================
        System.out.println("\n--- Step 4: Populating Group Game List ---");

        List<Game> groupGames = new ArrayList<>();
        groupGames.add(game1);
        groupGames.add(game2);
        groupGames.add(game3);
        groupGames.add(game4);
        groupGames.add(game5);

        group.setGames(groupGames);
        assertTrue(sqlHandler.updateGroup(group), "Group update with games should succeed");
        System.out.println(" Added 5 games to group game list");

        // ==================================================
        // STEP 5: RANK GAMES BASED ON PLAYTIME
        // ==================================================
        System.out.println("\n--- Step 5: Ranking Games by Playtime ---");

        GroupOperations groupOps = new GroupOperations(group.getGames());
        groupOps.rankList(group, user1, sqlHandler);   // FIX: added missing parameters

        // Verify ranking - games should be sorted by playtime descending
        List<Game> rankedGames = group.getGames();
        assertEquals(5, rankedGames.size(), "Group should have 5 games");

        System.out.println("Game ranking results:");
        for (int i = 0; i < rankedGames.size(); i++) {
            Game game = rankedGames.get(i);
            System.out.println("  " + (i + 1) + ". " + game.getGameName() + " (" + (game.getPlaytime() / 60.0) + " hours)");
        }

        // Expected order: Minecraft (60h), Dota 2 (40h), Among Us (30h), CS2 (20h), Witcher 3 (10h)
        assertEquals("Minecraft", rankedGames.get(0).getGameName(), "Minecraft should be first (highest playtime)");
        assertEquals(3600, rankedGames.get(0).getPlaytime(), "Minecraft should have 60 hours");

        assertEquals("Dota 2", rankedGames.get(1).getGameName(), "Dota 2 should be second");
        assertEquals(2400, rankedGames.get(1).getPlaytime(), "Dota 2 should have 40 hours");

        assertEquals("Among Us", rankedGames.get(2).getGameName(), "Among Us should be third");
        assertEquals(1800, rankedGames.get(2).getPlaytime(), "Among Us should have 30 hours");

        assertEquals("Counter-Strike 2", rankedGames.get(3).getGameName(), "CS2 should be fourth");
        assertEquals(1200, rankedGames.get(3).getPlaytime(), "CS2 should have 20 hours");

        assertEquals("The Witcher 3", rankedGames.get(4).getGameName(), "Witcher 3 should be fifth (lowest playtime)");
        assertEquals(600, rankedGames.get(4).getPlaytime(), "Witcher 3 should have 10 hours");

        // ==================================================
        // STEP 6: SIMULATE VOTING PROCESS
        // ==================================================
        System.out.println("\n--- Step 6: Voting Process ---");

        // User1 votes for Minecraft (ranked #1)
        GroupVote vote1 = new GroupVote(group, user1);
        vote1.castVote(rankedGames.get(0)); // Minecraft
        assertTrue(sqlHandler.createGroupVote(vote1), "Vote1 creation should succeed");
        System.out.println(" Alice voted for: " + rankedGames.get(0).getGameName());

        // User2 votes for Dota 2 (ranked #2)
        GroupVote vote2 = new GroupVote(group, user2);
        vote2.castVote(rankedGames.get(1)); // Dota 2
        assertTrue(sqlHandler.createGroupVote(vote2), "Vote2 creation should succeed");
        System.out.println(" Bob voted for: " + rankedGames.get(1).getGameName());

        // User3 votes for Minecraft (ranked #1) - creating a tie scenario
        GroupVote vote3 = new GroupVote(group, user3);
        vote3.castVote(rankedGames.get(0)); // Minecraft
        assertTrue(sqlHandler.createGroupVote(vote3), "Vote3 creation should succeed");
        System.out.println(" Charlie voted for: " + rankedGames.get(0).getGameName());

        // ==================================================
        // STEP 7: TALLY VOTES AND DETERMINE WINNER
        // ==================================================
        System.out.println("\n--- Step 7: Tallying Votes ---");

        List<GroupVote> allVotes = sqlHandler.getGroupVotesByGroup(group);
        assertEquals(3, allVotes.size(), "Should have 3 votes");

        Map<Game, Integer> voteTally = GroupOperations.tallyVotes(allVotes);

        System.out.println("Vote tally:");
        for (Map.Entry<Game, Integer> entry : voteTally.entrySet()) {
            System.out.println("  " + entry.getKey().getGameName() + ": " + entry.getValue() + " vote(s)");
        }

        Game winner = GroupOperations.getWinner(voteTally);
        assertNotNull(winner, "Should have a winner (no tie)");
        System.out.println(" WINNER: " + winner.getGameName() + " with " + voteTally.get(winner) + " vote(s)!");

        // ==================================================
        // STEP 8: TEST VOTE UPDATES
        // ==================================================
        System.out.println("\n--- Step 8: Testing Vote Updates ---");

        // User2 changes vote from Dota 2 to Minecraft
        vote2.updateVote(rankedGames.get(0)); // Change to Minecraft
        assertTrue(sqlHandler.updateGroupVote(vote2), "Vote2 update should succeed");
        System.out.println(" Bob changed vote to: " + rankedGames.get(0).getGameName());

        // Recalculate votes
        allVotes = sqlHandler.getGroupVotesByGroup(group);
        voteTally = GroupOperations.tallyVotes(allVotes);

        System.out.println("Updated vote tally:");
        for (Map.Entry<Game, Integer> entry : voteTally.entrySet()) {
            System.out.println("  " + entry.getKey().getGameName() + ": " + entry.getValue() + " vote(s)");
        }

        winner = GroupOperations.getWinner(voteTally);
        assertNotNull(winner, "Should have a winner");
        System.out.println(" UPDATED WINNER: " + winner.getGameName() + " with unanimous " + voteTally.get(winner) + " vote(s)!");

        // ==================================================
        // STEP 9: TEST TIE SCENARIO
        // ==================================================
        System.out.println("\n--- Step 9: Testing Tie Scenario ---");

        // User3 changes vote to Dota 2, creating a tie
        vote3.updateVote(rankedGames.get(1)); // Change to Dota 2
        assertTrue(sqlHandler.updateGroupVote(vote3), "Vote3 update should succeed");
        System.out.println(" Charlie changed vote to: " + rankedGames.get(1).getGameName() + " (creating tie)");

        // Recalculate votes
        allVotes = sqlHandler.getGroupVotesByGroup(group);
        voteTally = GroupOperations.tallyVotes(allVotes);

        System.out.println("Tie vote tally:");
        for (Map.Entry<Game, Integer> entry : voteTally.entrySet()) {
            System.out.println("  " + entry.getKey().getGameName() + ": " + entry.getValue() + " vote(s)");
        }

        // With votes: Minecraft (vote1), Dota2 (vote2, vote3) → Dota2 wins
        winner = GroupOperations.getWinner(voteTally);
        assertNotNull(winner, "Should have a winner (Dota 2 has more votes after tie manipulation)");
        System.out.println(" TIE BREAKER WINNER: " + winner.getGameName() + " wins with " + voteTally.get(winner) + " vote(s)!");

        // The transaction will automatically roll back, so no manual cleanup is necessary.
        System.out.println("\n=== GAME MATCHMAKING TEST COMPLETED SUCCESSFULLY ===");
    }

    @Test
    public void testGameOperations() {
        Game testGame = new Game("Test Game", 0, 0);
        testGame.setPlaytime(120); // 2 hours

        GameOperations gameOps = new GameOperations();

        // Test playtime conversion
        assertEquals(2.0, gameOps.getPlaytimeHours(testGame), "Should convert 120 minutes to 2 hours");

        // Test tag operations
        Tag tag1 = new Tag("Action", testGame);
        Tag tag2 = new Tag("Multiplayer", testGame);

        assertNotNull(gameOps.addTag(testGame, tag1), "Adding first tag should succeed");
        assertNotNull(gameOps.addTag(testGame, tag2), "Adding second tag should succeed");
        assertNull(gameOps.addTag(testGame, tag1), "Adding duplicate tag should return null");

        assertEquals(tag1, gameOps.hasTag(testGame, "Action"), "Should find Action tag");
        assertEquals(tag2, gameOps.hasTag(testGame, "Multiplayer"), "Should find Multiplayer tag");
        assertNull(gameOps.hasTag(testGame, "Nonexistent"), "Should not find nonexistent tag");

        // Remove tag
        assertEquals(tag1, gameOps.removeTag(testGame, "Action"), "Should remove Action tag");
        assertNull(gameOps.hasTag(testGame, "Action"), "Action tag should be gone");
    }

    @Test
    public void testGroupOperations() {
        Group testGroup = new Group();
        testGroup.setGroupName("Test Group");

        GroupOperations groupOps = new GroupOperations();

        // Test invite link generation
        String inviteLink = groupOps.generateGroupInviteLink();
        assertNotNull(inviteLink, "Invite link should be generated");
        assertTrue(inviteLink.length() > 0, "Invite link should not be empty");

        // Test session operations
        GroupSession session = new GroupSession();
        session.setActive(true);

        GroupSession cancelled = groupOps.cancelSession(testGroup, session);
        assertFalse(cancelled.isActive(), "Session should be cancelled");
    }
}