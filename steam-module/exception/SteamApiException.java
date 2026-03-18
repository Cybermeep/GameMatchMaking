package com.gamematchmaker.steam.exception;

/**
 * Thrown when a Steam Web API call fails — distinct from authentication
 * failures ({@link SteamAuthException}). This covers network timeouts,
 * HTTP error responses, and malformed API responses.
 *
 * Used by all classes in the {@code steam.api} and {@code steam.importer}
 * packages. Callers should handle this to satisfy SRS §3.2.2 (Reliability):
 * "The system should be able to handle errors with tact including but not
 * limited to temporary Steam API unavailability."
 */
public class SteamApiException extends Exception {

    /** HTTP status code from Steam, or -1 if unavailable. */
    private final int httpStatus;

    public SteamApiException(String message) {
        super(message);
        this.httpStatus = -1;
    }

    public SteamApiException(String message, int httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public SteamApiException(String message, Throwable cause) {
        super(message, cause);
        this.httpStatus = -1;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    /** Returns true when Steam returned HTTP 401 — usually a bad API key or private profile. */
    public boolean isUnauthorized() {
        return httpStatus == 401;
    }

    /** Returns true when Steam returned HTTP 429 — rate limit exceeded. */
    public boolean isRateLimited() {
        return httpStatus == 429;
    }
}
