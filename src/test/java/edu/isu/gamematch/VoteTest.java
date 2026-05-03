package edu.isu.gamematch;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class VoteTest {
    /**
     * user’s vote is recorded 
     * Paths: 1. Success, 2. Existing vote, 3. Invalid game
     */
    @Test
    public void testVoteRecording() {
        Game game = new Game("Elden Ring", 0, 0);
        GroupVote vote = new GroupVote(new Group(), new User());

        // Path 1
        assertTrue(vote.castVote(game), "Vote should be recorded");
        assertNotNull(vote.getVoteTime(), "Post-condition: Time must be recorded");

        // Path 2
        assertFalse(vote.castVote(game), "Should not allow re-recording without update");

        // Path 3
        GroupVote emptyVote = new GroupVote(new Group(), new User());
        assertFalse(emptyVote.castVote(null), "Should fail if no game is selected");
    }

    /**
     * vote totals are updated and visible
     * Paths: 1. Tallying valid votes, 2. Skipping empty votes
     */
    @Test
    public void testVoteTallying() {
        Game gameA = new Game("Halo", 0, 0);
        User user1 = new User();
        User user2 = new User();
        
        List<GroupVote> votes = new ArrayList<>();
        
        // Vote 1
        GroupVote v1 = new GroupVote(new Group(), user1);
        v1.castVote(gameA);
        votes.add(v1);

        // Vote 2
        GroupVote v2 = new GroupVote(new Group(), user2);
        votes.add(v2);

        // Tallying the list
        Map<Game, Integer> tally = GroupVote.tallyVotes(votes);
        
        assertEquals(1, tally.get(gameA), "Tally should show 1 vote for Game A");
        assertFalse(tally.containsKey(null), "Path: Null games should not be in tally");
    }

    /**
     * update recorded vote
     * Paths: 1. Successful update, 2. No existing vote to update
     */
    @Test
    public void testUpdateVote() {
        Game oldGame = new Game("CS:GO", 0, 0);
        Game newGame = new Game("Valorant", 0, 0);
        GroupVote vote = new GroupVote(new Group(), new User());

        // Path: Updating before casting
        assertFalse(vote.updateVote(newGame), "Should fail if no vote exists");

        // Path: Successful update
        vote.castVote(oldGame);
        assertTrue(vote.updateVote(newGame), "User should be able to change their vote");
        assertEquals(newGame, vote.getGame(), "Recorded game should be updated");
    }
}
