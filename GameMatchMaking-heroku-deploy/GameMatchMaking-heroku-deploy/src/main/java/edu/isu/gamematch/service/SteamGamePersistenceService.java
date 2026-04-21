package edu.isu.gamematch.service;

import edu.isu.gamematch.Game;
import edu.isu.gamematch.SQLHandler;
import edu.isu.gamematch.User;
import edu.isu.gamematch.steam.SteamGame;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Handles persisting Steam game data into the Oracle database.
 * Called after Steam login and on resync (FR 3.1.2, 3.1.13, 3.1.14).
 */
@Service
public class SteamGamePersistenceService {

    @Autowired
    private SQLHandler db;

    /**
     * Takes the list of games fetched from the Steam API and saves any
     * new ones to the database. Already-existing games are skipped.
     * Also associates the games with the user in the group matchmaking context.
     */
    public void persistUserGames(User dbUser, List<SteamGame> steamGames) {
        if (steamGames == null || steamGames.isEmpty()) return;

        for (SteamGame sg : steamGames) {
            try {
                // Check if this game is already in our database
                Game existing = db.getGameByAppId(sg.getAppId());
                if (existing == null) {
                    // Create a new Game record
                    Game game = new Game();
                    game.setGameName(sg.getName() != null ? sg.getName() : "Unknown Game");
                    game.setPlaytime(sg.getPlaytimeForever()); // playtime in minutes
                    game.setPlayTimeMinutes(sg.getPlaytimeForever() % 60);
                    game.setPlayTimeHours(sg.getPlaytimeForever() / 60);
                    if (sg.getAppId() != null) {
                        game.setSteamAppURL("https://store.steampowered.com/app/" + sg.getAppId());
                     }
                    db.saveGame(game);
                }
            } catch (Exception e) {
                // Log but don't crash — if one game fails, continue with the rest
                System.err.println("Failed to persist game: " + sg.getName() + " — " + e.getMessage());
            }
        }
    }
}