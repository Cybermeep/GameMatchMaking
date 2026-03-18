package com.gamematchmaker.steam.auth;

import com.gamematchmaker.steam.exception.SteamAuthException;
import com.gamematchmaker.steam.util.SteamConstants;
import org.junit.jupiter.api.*;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/*
 * SteamAuthServiceTest.java
 *
 *
 * SRP (Single Responsibility Principle) [IT 326 Topic 10-3, Slide 3]
 *   This test class has one responsibility: verify the behaviour of
 *   SteamAuthService. It does not test SteamApiClient, SteamSessionManager,
 *   or any servlet — each of those has its own test class.

 */
class SteamAuthServiceTest {

    // Fake values — not real keys, just valid non-blank strings for testing
    private static final String FAKE_API_KEY  = "AAAA1111BBBB2222CCCC3333DDDD4444";
    private static final String FAKE_CALLBACK = "https://app.example.com/auth/steam/callback";

    private SteamAuthService service;

    // @BeforeEach runs before every @Test method — creates a fresh service each time
    // so test order cannot affect results
    @BeforeEach
    void setUp() {
        service = new SteamAuthService(FAKE_API_KEY, FAKE_CALLBACK);
    }

    // Constructor validation tests
    // Tests that bad inputs fail fast at construction 

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
    void constructor_throwsOnEmptyCallbackUrl() {
        assertThrows(IllegalArgumentException.class,
            () -> new SteamAuthService(FAKE_API_KEY, ""));
    }

    // buildLoginUrl() tests
    // No network access needed — just validates the URL structure

    @Test
    void buildLoginUrl_startsWithSteamOpenIdEndpoint() {
        String url = service.buildLoginUrl();
        assertTrue(url.startsWith(SteamConstants.OPENID_ENDPOINT),
            "Login URL must point to Steam's OpenID endpoint");
    }

    @Test
    void buildLoginUrl_containsCheckidSetupMode() {
        String url = service.buildLoginUrl();
        // "checkid_setup" is the OpenID mode required to initiate a login
        assertTrue(url.contains("openid.mode=checkid_setup"),
            "Login URL must include checkid_setup mode");
    }

    @Test
    void buildLoginUrl_containsOpenIdNamespace() {
        String url = service.buildLoginUrl();
        assertTrue(url.contains("openid.ns="),
            "Login URL must include the openid.ns parameter");
    }

    @Test
    void buildLoginUrl_containsReturnToParameter() {
        String url = service.buildLoginUrl();
        // return_to tells Steam where to redirect the user after login
        assertTrue(url.contains("openid.return_to="),
            "Login URL must include openid.return_to (our callback URL)");
    }

    @Test
    void buildLoginUrl_includesOurCallbackDomain() {
        String url = service.buildLoginUrl();
        // The callback URL should be URL-encoded somewhere in the query string
        assertTrue(url.contains("app.example.com"),
            "Login URL should contain our registered callback domain");
    }

    // validateCallback() tests — cases that short-circuit before network

    @Test
    void validateCallback_returnsNullWhenMissingOpenIdMode() throws SteamAuthException {
        Map<String, String> params = new HashMap<>();
        // Has claimed_id but NOT mode — should return null immediately, no network call
        params.put("openid.claimed_id",
                SteamConstants.OPENID_CLAIMED_ID_PREFIX + "76561198000000001");

        assertNull(service.validateCallback(params),
            "Missing openid.mode should cause validateCallback to return null");
    }

    @Test
    void validateCallback_returnsNullWhenMissingClaimedId() throws SteamAuthException {
        Map<String, String> params = new HashMap<>();
        // Has mode but NOT claimed_id
        params.put("openid.mode", "id_res");

        assertNull(service.validateCallback(params),
            "Missing openid.claimed_id should cause validateCallback to return null");
    }

    @Test
    void validateCallback_returnsNullForCompletelyEmptyParams() throws SteamAuthException {
        assertNull(service.validateCallback(new HashMap<>()),
            "Empty params should return null without crashing");
    }
}
