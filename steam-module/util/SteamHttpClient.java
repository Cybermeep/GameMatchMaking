package com.gamematchmaker.steam.util;

import com.gamematchmaker.steam.exception.SteamApiException;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.StringJoiner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Shared HTTP utility for all Steam API and OpenID communication.
 *
 * Centralises connection setup, timeout config, response reading,
 * and error handling so that {@code SteamAuthService}, {@code SteamApiClient},
 * and the importers all use exactly the same networking code.
 *
 * All methods are package-accessible; external callers go through the
 * higher-level service classes.
 */
public final class SteamHttpClient {

    private static final Logger LOGGER = Logger.getLogger(SteamHttpClient.class.getName());

    private SteamHttpClient() {}

    // =========================================================================
    // GET
    // =========================================================================

    /**
     * Performs an HTTP GET to the given URL and returns the raw response body.
     *
     * @param url Full URL including any query parameters.
     * @return Response body as a UTF-8 string.
     * @throws SteamApiException on any HTTP error or IO failure.
     */
    public static String get(String url) throws SteamApiException {
        try {
            HttpURLConnection conn = openConnection(url, "GET");
            conn.setRequestProperty("Accept", "application/json");
            return readResponse(conn, url);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "IO error on GET: " + url, e);
            throw new SteamApiException("Network error calling Steam API: " + url, e);
        }
    }

    // =========================================================================
    // POST (used for OpenID check_authentication)
    // =========================================================================

    /**
     * Performs an HTTP POST with a URL-encoded body and returns the response body.
     *
     * @param url     Target URL.
     * @param params  Map of form parameters to URL-encode and send as the POST body.
     * @return Response body as a UTF-8 string.
     * @throws SteamApiException on any HTTP error or IO failure.
     */
    public static String post(String url, Map<String, String> params) throws SteamApiException {
        String payload = buildQueryString(params);
        try {
            HttpURLConnection conn = openConnection(url, "POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Accept", "text/plain");
            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload.getBytes(StandardCharsets.UTF_8));
            }
            return readResponse(conn, url);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "IO error on POST: " + url, e);
            throw new SteamApiException("Network error POSTing to Steam: " + url, e);
        }
    }

    // =========================================================================
    // URL building helpers
    // =========================================================================

    /**
     * Appends a map of parameters to a base URL as a query string.
     * Safe to call even when params is empty.
     */
    public static String appendParams(String baseUrl, Map<String, String> params) {
        if (params == null || params.isEmpty()) return baseUrl;
        return baseUrl + "?" + buildQueryString(params);
    }

    /**
     * URL-encodes a single value using UTF-8.
     */
    public static String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    /**
     * Builds an {@code application/x-www-form-urlencoded} query string
     * from a map, preserving insertion order.
     */
    public static String buildQueryString(Map<String, String> params) {
        StringJoiner joiner = new StringJoiner("&");
        params.forEach((k, v) -> joiner.add(encode(k) + "=" + encode(v)));
        return joiner.toString();
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private static HttpURLConnection openConnection(String url, String method) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(SteamConstants.HTTP_TIMEOUT_MS);
        conn.setReadTimeout(SteamConstants.HTTP_TIMEOUT_MS);
        conn.setInstanceFollowRedirects(true);
        return conn;
    }

    private static String readResponse(HttpURLConnection conn, String url)
            throws IOException, SteamApiException {

        int status = conn.getResponseCode();

        if (status == 401) {
            throw new SteamApiException(
                "Steam API returned 401 Unauthorized for: " + url +
                " — check your API key or the user's profile privacy settings.", 401);
        }
        if (status == 429) {
            throw new SteamApiException("Steam API rate limit exceeded (429).", 429);
        }
        if (status < 200 || status >= 300) {
            LOGGER.warning("Steam API returned HTTP " + status + " for: " + url);
            throw new SteamApiException("Unexpected HTTP " + status + " from Steam API.", status);
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return sb.toString();
        }
    }
}
