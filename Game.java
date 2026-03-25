/*
    Game Class
*/

import java.util.ArrayList;
import java.util.List;

public class Game{
    private int gameID;
    private String gameName;
    private int playtimeForever; //in mins
    private int playtimeLastTwoWeeks; //use ts for recent playtime calcs
    private String steamAppURL;
    private List<String> genres;
    private List<Tag> tags; //user g enerated tags

    public Game(){
        this.genres = new ArrayList<String>();
        this.tags = new ArrayList<Tag>();
    }

    public Game(int gameID, String gameName, int playtimeForever){
        this.gameID = gameID;
        this.gameName = gameName;
        this.playtimeForever = playtimeForever;
        this.genres = new ArrayList<String>();
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

    public int getPlaytimeForever(){
        return playtimeForever;
    }

    public void setPlaytimeForever(int playtimeForever){
        this.playtimeForever = playtimeForever;
    }

    public int getPlaytimeLastTwoWeeks(){
        return playtimeLastTwoWeeks;
    }

    public void setPlaytimeLastTwoWeeks(int playtimeLastTwoWeeks){
        this.playtimeLastTwoWeeks = playtimeLastTwoWeeks;
    }

    public String getSteamAppURL(){
        return steamAppURL;
    }

    public void setSteamAppURL(String steamAppURL){
        this.steamAppURL = steamAppURL;
    }

    public List<String> getGenres(){
        return genres;
    }

    public void setGenres(List<String> genres){
        this.genres = genres;
    }

    public List<Tag> getTags(){
        return tags;
    }

    public void setTags(List<Tag> tags){
        this.tags = tags;
    }

    @Override
    public String toString(){
        return "Game{" + "gameID=" + gameID + ", gameName'" + gameName + '\'' + ", playtimeForever=" + playtimeForever + " min" + ", playtimeLastTwoWeeks=" + playtimeLastTwoWeeks + " min" + ", genres=" + genres + "}";
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