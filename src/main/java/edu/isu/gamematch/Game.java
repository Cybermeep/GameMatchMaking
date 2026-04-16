package edu.isu.gamematch;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "games")
public class Game{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "game_id")
    private int gameID;

    @Column(name = "game_name", nullable = false)
    private String gameName;

    @Column(name = "play_time_hours")
    private int playTimeHours;

    @Column(name = "play_time_minutes")
    private int playTimeMinutes;

    @Column(name = "genre")
    private String genre;

    @Column(name = "playtime_forever")
    private int playtimeForever;

    @Column(name = "steam_app_url")
    private String steamAppURL;

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL)
    private List<Tag> tags;

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL)
    private List<GameAchievement> gameAchievements = new ArrayList<>();

    public Game(){
        this.tags = new ArrayList<Tag>();
    }

    public Game(String gameName, int playTimeHours, int playTimeMinutes){
        this.gameName = gameName;
        this.playTimeHours = playTimeHours;
        this.playTimeMinutes = playTimeMinutes;
        this.tags = new ArrayList<Tag>();
    }

    public Game(int gameID, String gameName, String genre){
        this.gameID = gameID;
        this.gameName = gameName;
        this.genre = genre;
        this.tags = new ArrayList<Tag>();
    }
    
    //getters and setters
    public int getGameID(){
        return gameID;
    }

    public void setGameID(int gameID){
        this.gameID = gameID;
    }

    public String getGameName(){
        return gameName;
    }

    public void setGameName(String gameName){
        this.gameName = gameName;
    }

    public String getGenre(){
        return genre;
    }

    public void setGenre(String genre){
        this.genre = genre;
    }

    public int getPlaytime(){
        return playtimeForever;
    }

    public void setPlaytime(int playtime){
        this.playtimeForever = playtime;
    }

    public String getSteamAppURL(){
        return steamAppURL;
    }

    public void setSteamAppURL(String steamAppURL){
        this.steamAppURL = steamAppURL;
    }

    public List<Tag> getTags(){
        return tags;
    }

    public void setTags(List<Tag> tags){
        this.tags = tags;
    }

    public int getPlayTimeHours() {
        return playTimeHours;
    }

    public void setPlayTimeHours(int playTimeHours) {
        this.playTimeHours = playTimeHours;
    }

    public int getPlayTimeMinutes() {
        return playTimeMinutes;
    }

    public void setPlayTimeMinutes(int playTimeMinutes) {
        this.playTimeMinutes = playTimeMinutes;
    }

    public List<GameAchievement> getGameAchievements() {
        return gameAchievements;
    }

    public void setGameAchievements(List<GameAchievement> gameAchievements) {
        this.gameAchievements = gameAchievements;
    }

    public void addGameAchievement(GameAchievement gameAchievement) {
        gameAchievements.add(gameAchievement);
        gameAchievement.setGame(this);
    }

    public void removeGameAchievement(GameAchievement gameAchievement) {
        gameAchievements.remove(gameAchievement);
        gameAchievement.setGame(null);
    }

    @Override
    public String toString(){
        return "Game{" + "gameID=" + gameID + ", gameName'" + gameName + '\'' + ", genre='" + genre + '\'' + ", playtimeForever=" + playtimeForever + " min" + "}";
    }

    @Override
    public boolean equals(Object o){
        if(this == o){
            return true;
        }
        if(!(o instanceof Game)){
            return false;
        }
        Game other = (Game) o;
        return this.gameID == other.gameID;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(gameID);
    }

}