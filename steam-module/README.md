# Steam Module
**Game Match Maker — IT 326 Principles of Software Engineering | SRS v1.0**

Standalone Java module for all Steam OpenID authentication and Steam Web API
integration. Drop it into any Java web application as a dependency.

---

## Package Architecture

```
com.gamematchmaker.steam/
│
├── auth/                          Authentication layer
│   ├── SteamAuthService.java      Core OpenID 2.0 flow (3 steps)
│   ├── SteamSessionManager.java   HTTP session creation, reading, logout
│   ├── SteamAuthFilter.java       Servlet filter — guards all protected routes
│   └── SteamAuthServlets.java     /auth/steam/login · /callback · /logout
│
├── api/                           Steam Web API layer
│   └── SteamApiClient.java        All Steam API calls — profiles, games,
│                                  achievements, friends
│
├── importer/                      Data import layer
│   ├── ImportSteam.java           Per-user importer (matches SRS class diagram)
│   └── ImportLocal.java           Local Steam filesystem scanner
│
├── model/                         Steam API data transfer objects
│   └── SteamModels.java           SteamProfile · SteamGame · SteamAchievement
│                                  · SteamFriend (all inner classes)
│
├── exception/                     Module-specific exceptions
│   ├── SteamAuthException.java    OpenID authentication failures
│   └── SteamApiException.java     Steam Web API call failures
│
└── util/                          Shared utilities
    ├── SteamConstants.java        All URLs, keys, and config in one place
    ├── SteamHttpClient.java       Shared HTTP GET / POST logic
    └── JsonParser.java            Lightweight Steam JSON field extractor
```

---

---

## SRS Functional Requirements Coverage

| Class | FR | Description |
| `SteamAuthService` + `SteamAuthServlets` | 3.1.1 | Authenticate Steam OAuth Login |
| `ImportSteam.fetchOwnedGames()` | 3.1.2 | Import Steam Game Library |
| `ImportSteam.fetchOwnedGames()` | 3.1.13 | Resynchronize Game Library |
| `ImportLocal.getInstalledGames()` | 3.1.14 | Resynchronize Installed Games |
| `SteamApiClient.fetchProfiles()` | 3.1.15 | Retrieve another user's profile |
| `ImportSteam.fetchAchievements()` | 3.1.22 | Compare Achievements in a Group |
| `ImportSteam.fetchFriends()` | 3.1.25 | Retrieve Mutual Friends |

---

## Quick Start

### 1. Prerequisites

- Java 17+
- Maven 3.8+
- Steam Web API key → https://steamcommunity.com/dev/apikey

### 2. Environment variables

```bash
export STEAM_API_KEY="YOUR_32_CHAR_API_KEY_HERE"
export STEAM_CALLBACK_URL="https://yourdomain.com/auth/steam/callback"
```

Never hard-code these values (SRS §3.2.3 — Security).

### 3. Build

```bash
mvn clean package
```

### 4. Run tests

```bash
mvn test
```

### 5. Typical usage in a servlet application

**Register the auth filter in `web.xml`:**

```xml
<filter>
    <filter-name>SteamAuthFilter</filter-name>
    <filter-class>com.gamematchmaker.steam.auth.SteamAuthFilter</filter-class>
</filter>
<filter-mapping>
    <filter-name>SteamAuthFilter</filter-name>
    <url-pattern>/api/*</url-pattern>
    <url-pattern>/dashboard/*</url-pattern>
</filter-mapping>
```

**Initiate login from a JSP or HTML page:**

```html
<a href="/auth/steam/login">Sign in through Steam</a>
```

**Read the authenticated user in any protected servlet:**

```java
import com.gamematchmaker.steam.auth.SteamSessionManager;
import com.gamematchmaker.steam.model.SteamModels.SteamProfile;

SteamProfile profile = SteamSessionManager.getCurrentProfile(request);
// profile.steamId, profile.personaName, profile.avatarFull ...
```

**Import a user's game library:**

```java
import com.gamematchmaker.steam.importer.ImportSteam;
import com.gamematchmaker.steam.model.SteamModels.SteamGame;

ImportSteam importer = new ImportSteam(profile.steamId, System.getenv("STEAM_API_KEY"));
List<SteamGame> games = importer.fetchOwnedGames(true, true);
```

**Check locally installed games:**

```java
import com.gamematchmaker.steam.importer.ImportLocal;
import com.gamematchmaker.steam.model.SteamModels.SteamGame;

ImportLocal local = new ImportLocal(ImportLocal.detectSteamPath(), "appmanifest_*.acf");
List<SteamGame> installed = local.getInstalledGames();
```

---

## Key Design Decisions

**No external dependencies.** The module uses only the Java standard library
plus the Servlet API (provided by Tomcat). This keeps the JAR small and avoids
version conflicts in the parent project.



**One HTTP client, used everywhere.** `SteamHttpClient` is the only class that
opens `HttpURLConnection`. All other classes call it rather than doing
networking themselves, making it easy to swap for a mock in tests.

**Models are DTOs, not domain objects.** `SteamModels` classes (SteamProfile,
SteamGame, etc.) represent exactly what Steam returns. The application's domain
models (User, Game, etc.) are separate and live in the main application layer.

---

## GitHub
https://github.com/Cybermeep/GameMatchMaking
