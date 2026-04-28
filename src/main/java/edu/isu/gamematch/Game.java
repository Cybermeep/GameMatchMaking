package edu.isu.gamematch;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "games")
public class Game{
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "game_seq")
    @SequenceGenerator(name = "game_seq", sequenceName = "GAME_SEQ", allocationSize = 1)
    @Column(name = "game_id")
    private int gameID;

    @Column(name = "game_name", nullable = false)
    private String gameName;

    @Column(name = "playtime_forever")
    private int playtimeForever;

    @Column(name = "playtime_last_two_weeks")
    private int playtimeLastTwoWeeks;

    @ElementCollection
    @CollectionTable(name = "game_genres", joinColumns = @JoinColumn(name = "game_id"))
    @Column(name = "genre_name")
    private List<String> genres;

    @Column(name = "playtime_forever")
    private int playtimeForever;

    @Column(name = "steam_app_url")
    private String steamAppURL;

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL)
    private List<Tag> tags;

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL)
    private List<GameAchievement> gameAchievements = new ArrayList<>();

    public Game(){
        this.genres = new ArrayList<String>();
        this.tags = new ArrayList<Tag>();
        this.gameAchievements = new ArrayList<GameAchievement>();
    }

    public Game(int gameID, String gameName, int playtimeForever) {
        this.gameID = gameID;
        this.gameName = gameName;
        this.playtimeForever = playtimeForever;
        this.genres = new ArrayList<String>();
        this.tags = new ArrayList<Tag>();
        this.gameAchievements = new ArrayList<GameAchievement>();
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

    public int getPlaytime() {
        return playtimeForever;
    }

    public void setPlaytime(int playtimeForever) {
        this.playtimeForever = playtimeForever;
    }

    public int getPlaytimeLastTwoWeeks() {
        return playtimeLastTwoWeeks;
    }

    public void setPlaytimeMinutes(int playtimeLastTwoWeeks) {
        this.playtimeLastTwoWeeks = playtimeLastTwoWeeks;
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