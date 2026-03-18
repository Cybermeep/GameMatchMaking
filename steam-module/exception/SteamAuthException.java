package com.gamematchmaker.steam.exception;

/**
 * Thrown when the Steam OpenID authentication flow fails —
 * either due to a network error communicating with Steam's
 * OpenID/OAuth endpoints, or because Steam returned an invalid
 * or forged callback response.
 *
 * Callers should catch this and redirect the user to a friendly
 * error page (SRS §3.2.2 — handle errors with tact).
 */
public class SteamAuthException extends Exception {

    public SteamAuthException(String message) {
        super(message);
    }

    public SteamAuthException(String message, Throwable cause) {
        super(message, cause);
    }
}
