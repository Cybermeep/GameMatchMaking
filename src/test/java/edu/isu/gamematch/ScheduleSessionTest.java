package edu.isu.gamematch;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class ScheduleSessionTest {

    static {
        // Force H2 database – same as SteamAuthTest
        System.setProperty("hibernate.connection.url",
                "jdbc:h2:mem:gamematch-test;DB_CLOSE_DELAY=-1");
        System.setProperty("hibernate.connection.username", "sa");
        System.setProperty("hibernate.connection.password", "");
        System.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
        System.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
    }

    @Autowired
    private SQLHandler sqlHandler;

    @Test
    public void testScheduleSessionProcess() {
        System.out.println("=== STARTING SCHEDULE SESSION TEST ===");

        // ==================================================
        // STEP 1: CREATE USERS
        // ==================================================
        User user1 = createUser(111222333L, "SessionAlice");
        User user2 = createUser(444555666L, "SessionBob");
        assertTrue(sqlHandler.createUser(user1), "User1 creation should succeed");
        assertTrue(sqlHandler.createUser(user2), "User2 creation should succeed");

        // ==================================================
        // STEP 2: CREATE GAME
        // ==================================================
        Game game = new Game("Helldivers 2", 0, 0);
        game.setPlaytime(900);
        game.setGenre("Co-op Shooter");
        assertTrue(sqlHandler.createGame(game), "Game creation should succeed");

        // ==================================================
        // STEP 3: CREATE GROUP
        // ==================================================
        Group group = new Group();
        group.setGroupName("Scheduled Squad");
        group.setGroupOwner(user1);
        group.addGroupMember(user1);
        group.addGroupMember(user2);
        assertTrue(sqlHandler.createGroup(group), "Group creation should succeed");

        // ==================================================
        // STEP 4: SCHEDULE SESSION
        // ==================================================
        LocalDateTime scheduledDate = LocalDateTime.of(2026, 5, 10, 19, 30);
        GroupSession session = new GroupSession(group);
        session.setGame(game);
        session.setScheduledDate(scheduledDate);
        session.setDuration(120);
        session.setActive(true);
        session.addMember(user1);
        session.addMember(user2);
        assertTrue(sqlHandler.createGroupSession(session), "Session creation should succeed");

        // ==================================================
        // STEP 5: VERIFY SESSION WAS SAVED
        // ==================================================
        GroupSession savedSession = sqlHandler.getGroupSessionById(session.getSessionID());
        assertNotNull(savedSession, "Saved session should be retrievable by ID");
        assertEquals(group.getGroupID(), savedSession.getGroup().getGroupID(), "Session should belong to the group");
        assertEquals(game.getGameID(), savedSession.getGame().getGameID(), "Session should use the selected game");
        assertEquals(scheduledDate, savedSession.getScheduledDate(), "Scheduled date should be saved");
        assertEquals(120, savedSession.getDuration(), "Duration should be saved");
        assertTrue(savedSession.isActive(), "New session should be active");

        // ==================================================
        // STEP 6: VERIFY LOOKUPS
        // ==================================================
        List<GroupSession> groupSessions = sqlHandler.getGroupSessionsByGroup(group);
        assertEquals(1, groupSessions.size(), "Group should have one scheduled session");
        List<GroupSession> gameSessions = sqlHandler.getGroupSessionsByGame(game);
        assertEquals(1, gameSessions.size(), "Game should have one scheduled session");

        System.out.println("=== SCHEDULE SESSION TEST COMPLETED SUCCESSFULLY ===");
    }

    @Test
    public void testUpdateAndCancelScheduledSession() {
        User user = createUser(777888999L, "SessionCharlie");
        Game game = new Game("Stardew Valley", 0, 0);
        Group group = createGroup("Calendar Crew", user);

        assertTrue(sqlHandler.createUser(user), "User creation should succeed");
        assertTrue(sqlHandler.createGame(game), "Game creation should succeed");
        assertTrue(sqlHandler.createGroup(group), "Group creation should succeed");

        GroupSession session = new GroupSession(group);
        session.setGame(game);
        session.setScheduledDate(LocalDateTime.of(2026, 5, 12, 18, 0));
        session.setDuration(60);
        session.setActive(true);
        assertTrue(sqlHandler.createGroupSession(session), "Session creation should succeed");

        // Update
        LocalDateTime updatedDate = LocalDateTime.of(2026, 5, 12, 20, 0);
        session.setScheduledDate(updatedDate);
        session.setDuration(90);
        assertTrue(sqlHandler.updateGroupSession(session), "Session update should succeed");

        GroupSession updatedSession = sqlHandler.getGroupSessionById(session.getSessionID());
        assertEquals(updatedDate, updatedSession.getScheduledDate(), "Scheduled date should update");
        assertEquals(90, updatedSession.getDuration(), "Duration should update");

        // Cancel
        GroupOperations groupOps = new GroupOperations();
        groupOps.cancelSession(group, session);
        assertTrue(sqlHandler.updateGroupSession(session), "Cancelled session update should succeed");

        GroupSession cancelledSession = sqlHandler.getGroupSessionById(session.getSessionID());
        assertFalse(cancelledSession.isActive(), "Cancelled session should be inactive");
    }

    @Test
    public void testDeleteScheduledSession() {
        User user = createUser(123123123L, "SessionDana");
        Game game = new Game("Rocket League", 0, 0);
        Group group = createGroup("Delete Test Group", user);

        assertTrue(sqlHandler.createUser(user), "User creation should succeed");
        assertTrue(sqlHandler.createGame(game), "Game creation should succeed");
        assertTrue(sqlHandler.createGroup(group), "Group creation should succeed");

        GroupSession session = new GroupSession(group);
        session.setGame(game);
        session.setScheduledDate(LocalDateTime.of(2026, 5, 15, 21, 0));
        session.setDuration(45);
        session.setActive(true);
        assertTrue(sqlHandler.createGroupSession(session), "Session creation should succeed");

        int sessionId = session.getSessionID();
        assertTrue(sqlHandler.deleteGroupSession(session), "Session deletion should succeed");
        assertNull(sqlHandler.getGroupSessionById(sessionId), "Deleted session should not be found");
    }

    private User createUser(long steamID, String personaName) {
        UserProfile profile = new UserProfile(personaName, null);
        User user = new User();
        user.setSteamID(steamID);
        user.setPersonaName(personaName);
        user.setUserProfile(profile);
        profile.setUser(user);
        return user;
    }

    private Group createGroup(String groupName, User owner) {
        Group group = new Group();
        group.setGroupName(groupName);
        group.setGroupOwner(owner);
        group.addGroupMember(owner);
        return group;
    }
}