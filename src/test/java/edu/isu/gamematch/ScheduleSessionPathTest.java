package edu.isu.gamematch;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import edu.isu.gamematch.controller.GameMatchController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class ScheduleSessionPathTest {

    private static final String SESSION_DB_USER = "db_user";

    @Mock
    private SQLHandler sqlHandler;

    private GameMatchController controller;
    private MockHttpSession httpSession;

    @BeforeEach
    public void setUp() {
        controller = new GameMatchController();
        ReflectionTestUtils.setField(controller, "sqlHandler", sqlHandler);
        httpSession = new MockHttpSession();
    }

    @Test
    public void scheduleSessionRedirectsHomeWhenUserIsNotLoggedIn() {
        String view = controller.scheduleSession(10, 20, "2026-05-10T19:30", 120, httpSession);
        assertEquals("redirect:/", view);
        verify(sqlHandler, never()).getGroupById(any(Integer.class));
        verify(sqlHandler, never()).getGameById(any(Integer.class));
        verify(sqlHandler, never()).createGroupSession(any(GroupSession.class));
    }

    @Test
    public void scheduleSessionRedirectsHomeWhenGroupDoesNotExist() {
        httpSession.setAttribute(SESSION_DB_USER, createUser(1, "Scheduler"));
        when(sqlHandler.getGroupById(10)).thenReturn(null);
        when(sqlHandler.getGameById(20)).thenReturn(new Game(20, "Helldivers 2", "Co-op Shooter"));
        String view = controller.scheduleSession(10, 20, "2026-05-10T19:30", 120, httpSession);
        assertEquals("redirect:/", view);
        verify(sqlHandler, never()).createGroupSession(any(GroupSession.class));
    }

    @Test
    public void scheduleSessionRedirectsHomeWhenGameDoesNotExist() {
        User user = createUser(1, "Scheduler");
        Group group = createGroup(10, user);
        httpSession.setAttribute(SESSION_DB_USER, user);
        when(sqlHandler.getGroupById(10)).thenReturn(group);
        when(sqlHandler.getGameById(20)).thenReturn(null);
        String view = controller.scheduleSession(10, 20, "2026-05-10T19:30", 120, httpSession);
        assertEquals("redirect:/", view);
        verify(sqlHandler, never()).createGroupSession(any(GroupSession.class));
    }

    @Test
    public void scheduleSessionCreatesActiveSessionWhenUserGroupAndGameExist() {
        User user = createUser(1, "Scheduler");
        Group group = createGroup(10, user);
        Game game = new Game(20, "Helldivers 2", "Co-op Shooter");
        httpSession.setAttribute(SESSION_DB_USER, user);
        when(sqlHandler.getGroupById(10)).thenReturn(group);
        when(sqlHandler.getGameById(20)).thenReturn(game);
        when(sqlHandler.createGroupSession(any(GroupSession.class))).thenReturn(true);
        String view = controller.scheduleSession(10, 20, "2026-05-10T19:30", 120, httpSession);
        assertEquals("redirect:/groups/10/sessions", view);
        ArgumentCaptor<GroupSession> captor = ArgumentCaptor.forClass(GroupSession.class);
        verify(sqlHandler).createGroupSession(captor.capture());
        GroupSession createdSession = captor.getValue();
        assertSame(group, createdSession.getGroup());
        assertSame(game, createdSession.getGame());
        assertEquals(LocalDateTime.of(2026, 5, 10, 19, 30), createdSession.getScheduledDate());
        assertEquals(120, createdSession.getDuration());
        assertTrue(createdSession.isActive());
    }

    private User createUser(int userId, String personaName) {
        User user = new User();
        user.setUserID(userId);
        user.setPersonaName(personaName);
        return user;
    }

    private Group createGroup(int groupId, User owner) {
        Group group = new Group();
        group.setGroupID(groupId);
        group.setGroupName("Scheduled Squad");
        group.setGroupOwner(owner);
        group.addGroupMember(owner);
        return group;
    }
}