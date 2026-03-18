package com.gamematchmaker.steam.exception;

/*
 * SteamAuthException.java
 *
 *   This exception represents one specific failure category: the Steam
 *   OpenID login flow failed. It is separate from SteamApiException
 *   (which covers API call failures). Each has one reason to change.
 */
public class SteamAuthException extends Exception {

    // Simple constructor for when we just have a message
    public SteamAuthException(String message) {
        super(message);
    }

    // Constructor that wraps another exception (cause chaining).
    // "cause" lets us preserve the original IOException or SteamApiException
    // without forcing callers to know about those internal types.
    public SteamAuthException(String message, Throwable cause) {
        super(message, cause);
    }
}
