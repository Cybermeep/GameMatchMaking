package com.gamematchmaker.steam.auth;

import com.gamematchmaker.steam.exception.SteamAuthException;
import com.gamematchmaker.steam.util.SteamConstants;
import org.junit.jupiter.api.*;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SteamAuthServiceTest {

    private static final String FAKE_API_KEY   = "AAAA1111BBBB2222CCCC3333DDDD4444";
    private static final String FAKE_CALLBACK  = "https://app.example.com/auth/steam/callback";

    private SteamAuthService service;

    @BeforeEach
    void setUp() {
        service = new SteamAuthService(FAKE_API_KEY, FAKE_CALLBACK);
    }

    // ── Constructor validation ────────────────────────────────────────────────

    @Test
    void constructor_throwsOnNullApiKey() {
        assertThrows(IllegalArgumentException.class,
            () -> new SteamAuthService(null, FAKE_CALLBACK));
    }

    @Test
    void constructor_throwsOnBlankApiKey() {
        assertThrows(IllegalArgumentException.class,
            () -> new SteamAuthService("   ", FAKE_CALLBACK));
    }

    @Test
    void constructor_throwsOnNullCallbackUrl() {
        assertThrows(IllegalArgumentException.class,
            () -> new SteamAuthService(FAKE_API_KEY, null));
    }

    @Test
    void constructor_throwsOnBlankCallbackUrl() {
        assertThrows(IllegalArgumentException.class,
            () -> new SteamAuthService(FAKE_API_KEY, ""));
    }

    // ── buildLoginUrl ─────────────────────────────────────────────────────────

    @Test
    void buildLoginUrl_startsWithSteamOpenIdEndpoint() {
        String url = service.buildLoginUrl();
        assertTrue(url.startsWith(SteamConstants.OPENID_ENDPOINT));
    }

    @Test
    void buildLoginUrl_containsOpenIdMode() {
        String url = service.buildLoginUrl();
        assertTrue(url.contains("openid.mode=checkid_setup"));
    }

    @Test
    void buildLoginUrl_containsOpenIdNs() {
        String url = service.buildLoginUrl();
        assertTrue(url.contains("openid.ns="));
    }

    @Test
    void buildLoginUrl_containsReturnToParam() {
        String url = service.buildLoginUrl();
        assertTrue(url.contains("openid.return_to="));
    }

    @Test
    void buildLoginUrl_containsEncodedCallbackUrl() {
        String url = service.buildLoginUrl();
        // callback URL should be URL-encoded in the query string
        assertTrue(url.contains("app.example.com"));
    }

    // ── validateCallback — missing fields (no network required) ───────────────

    @Test
    void validateCallback_returnsNullWhenMissingMode() throws SteamAuthException {
        Map<String, String> params = new HashMap<>();
        params.put("openid.claimed_id",
                SteamConstants.OPENID_CLAIMED_ID_PREFIX + "76561198000000001");
        // openid.mode intentionally absent
        assertNull(service.validateCallback(params));
    }

    @Test
    void validateCallback_returnsNullWhenMissingClaimedId() throws SteamAuthException {
        Map<String, String> params = new HashMap<>();
        params.put("openid.mode", "id_res");
        // openid.claimed_id intentionally absent
        assertNull(service.validateCallback(params));
    }

    @Test
    void validateCallback_returnsNullOnEmptyParams() throws SteamAuthException {
        assertNull(service.validateCallback(new HashMap<>()));
    }
}
