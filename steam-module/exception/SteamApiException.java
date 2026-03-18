package com.gamematchmaker.steam.exception;

/*
 * SteamApiException.java

 * SRP (Single Responsibility Principle) [IT 326 Topic 10-3, Slide 3]
 *   One job: represent a Steam Web API call failure. This is intentionally
 *   separate from SteamAuthException (OpenID login failures). Each exception
 *   class has exactly one reason to change — if the API error model changes,
 *   only this file needs updating.
 */
public class SteamApiException extends Exception {

    // The HTTP status code Steam returned, or -1 if we never got a response
    // (e.g. the connection timed out before any status was received)
    private final int httpStatus;

    // Use when we have a message but no HTTP code (e.g. a network timeout)
    public SteamApiException(String message) {
        super(message);
        this.httpStatus = -1;
    }

    // Use when Steam returned a specific HTTP error response
    public SteamApiException(String message, int httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    // Use when wrapping a lower-level exception (preserves the original cause)
    public SteamApiException(String message, Throwable cause) {
        super(message, cause);
        this.httpStatus = -1;
    }

    // Returns the HTTP status code, or -1 if one wasn't available
    public int getHttpStatus() {
        return httpStatus;
    }

    /**
     * Returns true when Steam replied with 401 Unauthorized.
     * This usually means the API key is wrong, or the user's Steam
     * profile/library is set to Private.
     * Callers can show a specific helpful message instead of a generic error.
     */
    public boolean isUnauthorized() {
        return httpStatus == 401;
    }

    /**
     * Returns true when Steam replied with 429 Too Many Requests.
     * We are sending requests too fast and Steam is throttling us.
     */
    public boolean isRateLimited() {
        return httpStatus == 429;
    }
}
