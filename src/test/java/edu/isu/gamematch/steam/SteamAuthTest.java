package edu.isu.gamematch.steam;

import edu.isu.gamematch.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class SteamAuthTest {

    static {
    System.setProperty("hibernate.connection.url",
            "jdbc:h2:mem:gamematch-test;DB_CLOSE_DELAY=-1");
    System.setProperty("hibernate.connection.username", "sa");
    System.setProperty("hibernate.connection.password", "");
    System.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
    System.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
}

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SQLHandler sqlHandler;

    @MockBean
    private SteamAPIService steamAPIService;

    private final String TEST_STEAM_ID = "76561199811956222";
    private final String TEST_PERSONA_NAME = "TestGamer";

    private SteamUser mockSteamUser;

    @BeforeEach
    void setUpMocks() {
        mockSteamUser = new SteamUser();
        mockSteamUser.setSteamId(TEST_STEAM_ID);
        mockSteamUser.setPersonaName(TEST_PERSONA_NAME);
        mockSteamUser.setAvatarUrl("http://avatar.url");
        mockSteamUser.setProfileUrl("https://steamcommunity.com/profiles/" + TEST_STEAM_ID);

        SteamGame game1 = new SteamGame();
        game1.setAppId("730");
        game1.setName("CS:GO");
        game1.setPlaytimeForever(1200);
        mockSteamUser.addGame(game1);

        SteamGame game2 = new SteamGame();
        game2.setAppId("570");
        game2.setName("Dota 2");
        game2.setPlaytimeForever(2400);
        mockSteamUser.addGame(game2);

        SteamGame recent = new SteamGame();
        recent.setAppId("730");
        recent.setName("CS:GO");
        recent.setPlaytimeLastTwoWeeks(300);
        mockSteamUser.getRecentlyPlayed().add(recent);

        when(steamAPIService.fetchCompleteUserData(TEST_STEAM_ID)).thenReturn(mockSteamUser);
        when(steamAPIService.fetchOwnedGames(TEST_STEAM_ID)).thenReturn(Collections.emptyList());
        when(steamAPIService.fetchFriendList(TEST_STEAM_ID)).thenReturn(Collections.emptyList());
        when(steamAPIService.fetchRecentlyPlayedGames(TEST_STEAM_ID)).thenReturn(mockSteamUser.getRecentlyPlayed());
    }

    @AfterEach
    void cleanup() {
        // Remove any user created by the test (the controller's own commit)
        User user = sqlHandler.searchUserBySteamId(TEST_STEAM_ID);
        if (user != null) {
            sqlHandler.deleteUser(user);
        }
    }

    @Test
    void testFullSteamLoginFlow() throws Exception {
        mockMvc.perform(get("/auth/steam"))
                .andExpect(status().is3xxRedirection());

        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(get("/auth/steam/callback")
                        .param("openid.ns", "http://specs.openid.net/auth/2.0")
                        .param("openid.mode", "id_res")
                        .param("openid.claimed_id", "https://steamcommunity.com/openid/id/" + TEST_STEAM_ID)
                        .param("openid.identity", "https://steamcommunity.com/openid/id/" + TEST_STEAM_ID)
                        .param("openid.return_to", "http://localhost:8080/auth/steam/callback")
                        .session(session))
                .andExpect(status().is3xxRedirection());

        User dbUser = sqlHandler.searchUserBySteamId(TEST_STEAM_ID);
        assertNotNull(dbUser, "Database user should have been created");
        assertEquals(TEST_STEAM_ID, String.valueOf(dbUser.getSteamID()));
        assertEquals(TEST_PERSONA_NAME, dbUser.getPersonaName());
        assertNotNull(dbUser.getUserProfile());

        mockMvc.perform(get("/dashboard").session(session))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("isSteam"));

        mockMvc.perform(get("/logout").session(session))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void testSteamCallbackWithInvalidParams() throws Exception {
        mockMvc.perform(get("/auth/steam/callback")
                        .param("openid.mode", "id_res"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("error"));
    }

    @Test
    void testDashboardWithoutAuthRedirects() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection());
    }
}