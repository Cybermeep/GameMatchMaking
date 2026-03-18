package com.gamematchmaker.steam.util;

import com.gamematchmaker.steam.exception.SteamApiException;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.StringJoiner;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * SteamHttpClient.java
 *
 *   This class has one and only one responsibility: send HTTP requests
 *   to Steam and return the raw response text. It does NOT parse the
 *   response, it does NOT know what the response means — that belongs
 *   to SteamApiClient. One reason to change this class: if the networking
 *   library changes or timeout behaviour needs tweaking.

 */
public final class SteamHttpClient {

    private static final Logger LOGGER = Logger.getLogger(SteamHttpClient.class.getName());

    private SteamHttpClient() {}

    // GET request

    /**
     * Sends an HTTP GET to the given URL and returns the response body.
     * Most Steam API calls are GET requests — we build a URL with parameters
     * baked in and read back the JSON response.
     */
    public static String get(String url) throws SteamApiException {
        try {
            HttpURLConnection conn = openConnection(url, "GET");
            conn.setRequestProperty("Accept", "application/json");
            return readResponse(conn, url);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Network error on GET: " + url, e);
            throw new SteamApiException("Network error calling Steam API: " + url, e);
        }
    }

    // POST request

    /**
     * Sends an HTTP POST with URL-encoded form data in the request body.
     * Used by the OpenID verification step — we post Steam's callback
     * parameters back to Steam so it can confirm they're genuine.
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
            LOGGER.log(Level.SEVERE, "Network error on POST: " + url, e);
            throw new SteamApiException("Network error POSTing to Steam: " + url, e);
        }
    }

    // URL building helpers

    /**
     * Appends a map of parameters onto a base URL as a query string.
     * Example: "https://api.steam.com/foo" + {key:"abc"} → "...foo?key=abc"
     */
    public static String appendParams(String baseUrl, Map<String, String> params) {
        if (params == null || params.isEmpty()) return baseUrl;
        return baseUrl + "?" + buildQueryString(params);
    }

    /**
     * URL-encodes a single value (converts spaces, &, etc. to %xx format).
     * Required so special characters don't break the URL structure.
     */
    public static String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    /**
     * Converts a map into a URL query string: "key1=val1&key2=val2".
     * Uses LinkedHashMap order so parameters stay predictable.
     */
    public static String buildQueryString(Map<String, String> params) {
        StringJoiner joiner = new StringJoiner("&");
        params.forEach((k, v) -> joiner.add(encode(k) + "=" + encode(v)));
        return joiner.toString();
    }

    // Private helpers (Encapsulation — hidden from all callers)

    // Opens an HttpURLConnection with our standard timeout and redirect settings
    private static HttpURLConnection openConnection(String url, String method) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(SteamConstants.HTTP_TIMEOUT_MS);
        conn.setReadTimeout(SteamConstants.HTTP_TIMEOUT_MS);
        conn.setInstanceFollowRedirects(true);
        return conn;
    }

    // Reads the response and throws typed exceptions for known error codes.
    // Returns the response body as a String on success (2xx).
    private static String readResponse(HttpURLConnection conn, String url)
            throws IOException, SteamApiException {

        int status = conn.getResponseCode();

        // 401 = bad API key or private Steam profile
        if (status == 401) {
            throw new SteamApiException(
                "Steam returned 401. Check your API key or the user's privacy settings.", 401);
        }
        // 429 = we're sending requests too fast
        if (status == 429) {
            throw new SteamApiException("Steam rate limited us (429). Slow down requests.", 429);
        }
        // Any non-2xx status is an error
        if (status < 200 || status >= 300) {
            LOGGER.warning("Steam returned HTTP " + status + " for: " + url);
            throw new SteamApiException("Unexpected HTTP " + status + " from Steam.", status);
        }

        // Read the response body into a single string
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
