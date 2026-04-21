/**
 * SteamAuthService Class
 * 
 * Handles Steam OpenID/OAuth 2.0 authentication. This service is responsible
 * for initiating the authentication flow, processing the callback, and
 * validating user sessions.
 * 
 * 
 * - Generate Steam authentication URL
 * - Process authentication callback
 * - Validate Steam OpenID responses
 * - Manage user authentication state

 */
package edu.isu.gamematch.steam;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SteamAuthService {
    
    private static final Logger logger = LoggerFactory.getLogger(SteamAuthService.class);
    
    @Value("${steam.oauth.url}")
    private String steamOAuthUrl;
    
    @Value("${steam.return.url}")
    private String returnUrl;

    
    // Store active sessions (in production, use a proper session store)
    private final Map<String, SteamUser> activeSessions;
    
    public SteamAuthService() {
        this.activeSessions = new ConcurrentHashMap<>();
    }
    
    /**
     * Generates the Steam OpenID authentication URL
     * 
     * @return The complete URL to redirect users to for Steam authentication
     */
    public String getSteamAuthUrl() {
        try {
            Map<String, String> params = new LinkedHashMap<>();
            params.put("openid.ns", "http://specs.openid.net/auth/2.0");
            params.put("openid.mode", "checkid_setup");
            String realm = returnUrl.substring(0, returnUrl.indexOf("/auth/steam/callback"));
            params.put("openid.realm", realm);
            //params.put("openid.realm", realm);  
            params.put("openid.return_to", returnUrl);
            params.put("openid.realm", returnUrl);
            params.put("openid.identity", "http://specs.openid.net/auth/2.0/identifier_select");
            params.put("openid.claimed_id", "http://specs.openid.net/auth/2.0/identifier_select");
            
            StringBuilder urlBuilder = new StringBuilder(steamOAuthUrl);
            urlBuilder.append("?");
            
            for (Map.Entry<String, String> param : params.entrySet()) {
                if (urlBuilder.length() > steamOAuthUrl.length() + 1) {
                    urlBuilder.append("&");
                }
                urlBuilder.append(param.getKey())
                    .append("=")
                    .append(URLEncoder.encode(param.getValue(), StandardCharsets.UTF_8.name()));
            }
            
            String authUrl = urlBuilder.toString();
            logger.debug("Generated Steam auth URL: {}", authUrl);
            return authUrl;
            
        } catch (UnsupportedEncodingException e) {
            logger.error("Failed to encode Steam auth URL parameters", e);
            return steamOAuthUrl;
        }
    }
    
    /**
     * Processes the Steam OpenID callback response
     * 
     * @param requestParams The request parameters from Steam callback
     * @return The Steam ID if validation succeeds, null otherwise
     */
    public String processCallback(Map<String, String[]> requestParams) {
        try {
            // Validate OpenID response
            if (!validateOpenIdResponse(requestParams)) {
                logger.warn("Invalid OpenID response received");
                return null;
            }
            
            // Extract Steam ID from the claimed_id
            String[] claimedIds = requestParams.get("openid.claimed_id");
            if (claimedIds != null && claimedIds.length > 0) {
                String claimedId = claimedIds[0];
                String[] parts = claimedId.split("/");
                String steamId = parts[parts.length - 1];
                
                logger.info("Successfully authenticated Steam user: {}", steamId);
                return steamId;
            }
        } catch (Exception e) {
            logger.error("Failed to process Steam callback", e);
        }
        return null;
    }
    
    /**
     * Validates the OpenID response from Steam
     * 
     * @param params The request parameters
     * @return True if validation passes
     */
    private boolean validateOpenIdResponse(Map<String, String[]> params) {
        // Check required OpenID parameters
        String[] modes = params.get("openid.mode");
        if (modes == null || modes.length == 0 || !"id_res".equals(modes[0])) {
            logger.warn("Invalid openid_mode: {}", modes != null ? modes[0] : "null");
            return false;
        }
        
        String[] claimedIds = params.get("openid.claimed_id");
        if (claimedIds == null || claimedIds.length == 0) {
            logger.warn("Missing openid_claimed_id");
            return false;
        }
        
        String claimedId = claimedIds[0];
        if (!claimedId.startsWith("https://steamcommunity.com/openid/id/")) {
            logger.warn("Invalid claimed_id format: {}", claimedId);
            return false;
        }
        
        return true;
    }
    
    /**
     * Creates a new session for an authenticated user
     * 
     * @param user The authenticated Steam user
     * @return Session token for the user
     */
    public String createSession(SteamUser user) {
        String sessionToken = UUID.randomUUID().toString();
        user.setSessionToken(sessionToken);
        user.setAuthenticated(true);
        user.updateLastActive();
        activeSessions.put(sessionToken, user);
        
        logger.info("Created session for user: {}", user.getPersonaName());
        return sessionToken;
    }
    
    /**
     * Validates a session token and returns the associated user
     * 
     * @param sessionToken The session token to validate
     * @return The SteamUser if valid, null otherwise
     */
    public SteamUser validateSession(String sessionToken) {
        SteamUser user = activeSessions.get(sessionToken);
        if (user != null && user.isAuthenticated()) {
            user.updateLastActive();
            return user;
        }
        return null;
    }
    
    /**
     * Invalidates a user session (logout)
     * 
     * @param sessionToken The session token to invalidate
     */
    public void invalidateSession(String sessionToken) {
        SteamUser user = activeSessions.remove(sessionToken);
        if (user != null) {
            user.setAuthenticated(false);
            logger.info("Invalidated session for user: {}", user.getPersonaName());
        }
    }
    
    /**
     * Gets the currently authenticated user for a session
     * 
     * @param sessionToken The session token
     * @return The authenticated user, or null if not found
     */
    public SteamUser getAuthenticatedUser(String sessionToken) {
        return validateSession(sessionToken);
    }
    
    /**
     * Checks if a session is valid and user is authenticated
     * 
     * @param sessionToken The session token to check
     * @return True if session is valid
     */
    public boolean isAuthenticated(String sessionToken) {
        return validateSession(sessionToken) != null;
    }
}