package edu.isu.gamematch.service;

import edu.isu.gamematch.*;
import edu.isu.gamematch.steam.SteamAPIService;
import edu.isu.gamematch.steam.SteamGame;
import edu.isu.gamematch.steam.SteamUser;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SteamImportService {

    private static final Logger logger = LoggerFactory.getLogger(SteamImportService.class);

    @Autowired
    private SteamAPIService apiService;

    @Async
    public void importFullLibraryToDb(String steamId, User dbUser) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            User attachedUser = session.get(User.class, dbUser.getUserID());
            if (attachedUser == null) {
                attachedUser = dbUser;
                session.save(attachedUser);
            }

            List<SteamGame> steamGames = apiService.fetchOwnedGames(steamId);
            if (steamGames == null || steamGames.isEmpty()) {
                tx.commit();
                return;
            }

            Set<Integer> existingGameIds = attachedUser.getAchievementData().stream()
                    .map(GameAchievement::getGame)
                    .filter(Objects::nonNull)
                    .map(Game::getGameID)
                    .collect(Collectors.toSet());

            for (SteamGame sg : steamGames) {
                Game game = session.createQuery("FROM Game WHERE gameName = :name", Game.class)
                        .setParameter("name", sg.getName())
                        .uniqueResult();

                if (game == null) {
                    game = new Game();
                    game.setGameName(sg.getName());
                    game.setHasAchievements(sg.isHasAchievements());
                    game.setPlaytime(sg.getPlaytimeForever());
                    game.setSteamAppURL("https://store.steampowered.com/app/" + sg.getAppId());
                    game.setAppId(sg.getAppId());
                    game.setGenre("Unknown");
                    session.save(game);
                } else {
                    game.setPlaytime(sg.getPlaytimeForever());
                    if (game.getAppId() == null || game.getAppId().isEmpty()) {
                        game.setAppId(sg.getAppId());
                    }
                    session.update(game);
                }

                if (!existingGameIds.contains(game.getGameID())) {
                    Achievement placeholder = session.createQuery(
                            "FROM Achievement WHERE achievementName = 'Game Owned'", Achievement.class)
                            .uniqueResult();
                    if (placeholder == null) {
                        placeholder = new Achievement("Game Owned", "Player owns this game on Steam");
                        session.save(placeholder);
                    }

                    GameAchievement ga = new GameAchievement(game, placeholder, attachedUser);
                    session.save(ga);
                    existingGameIds.add(game.getGameID());
                }
            }
            session.update(attachedUser);
            tx.commit();
            logger.info("Persisted {} games for user {}", steamGames.size(), steamId);
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            logger.error("Error persisting game library for {}", steamId, e);
        } finally {
            session.close();
        }
    }

    @Async
    public void importFriendListToDb(String steamId, User dbUser) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            User attachedUser = session.get(User.class, dbUser.getUserID());
            if (attachedUser == null) {
                attachedUser = dbUser;
                session.save(attachedUser);
            }
            List<SteamUser> steamFriends = apiService.fetchFriendList(steamId);
            if (steamFriends == null) { tx.commit(); return; }
            attachedUser.getFriends().clear();
            for (SteamUser sf : steamFriends) {
                User friend = session.createQuery("FROM User WHERE steamID = :sid", User.class)
                        .setParameter("sid", Long.parseLong(sf.getSteamId()))
                        .uniqueResult();
                if (friend == null) {
                    friend = new User();
                    friend.setSteamID(Long.parseLong(sf.getSteamId()));
                    friend.setPersonaName(sf.getPersonaName());
                    friend.setUserProfile(new UserProfile(sf.getPersonaName(), friend));
                    session.save(friend);
                }
                attachedUser.getFriends().add(friend);
            }
            session.update(attachedUser);
            tx.commit();
            logger.info("Imported {} friends for user {}", steamFriends.size(), steamId);
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            logger.error("Error importing friend list for {}", steamId, e);
        } finally { session.close(); }
    }
}