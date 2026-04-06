package edu.isu.gamematch;
/*
    Game Class
*/

import java.util.ArrayList;
import java.util.List;

public class Game{
    private int gameID;
    private String gameName;
    private String genre;
    private int playtimeForever;
    private String steamAppURL;
    private List<Tag> tags;

    public Game(){
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

    //methods

    /*adds tag to game (assuming it's user generated tags I forgot what Tag was supposed to be for)
    @param tag The tag to add
    */
    public void addTag(Tag tag){
        if(tag == null){
            return;
        }
        for(int i = 0; i < tags.size(); i++){
            if(tags.get(i).equals(tag)){
                return; //already in
            }
        }
        tags.add(tag);
    }

    /*
    removes tag from game
    @param tag The tag to remove
    @return boolean
    */
    public boolean removeTag(Tag tag){
        return tags.remove(tag);
    }

    /*
    checks if game has tag matching given tagname
    @param tagName 
    @return boolean
    */
    public boolean hasTag(String tagName){
        for(int i = 0; i < tags.size(); i++){
            if(tags.get(i).getTagName().equalsIgnoreCase(tagName)){
                return true;
            }
        }
        return false;
    }

    /*
    Returns playtime in hours 
    @return int
    */
    public int getPlaytimeHours(){
        return playtimeForever / 60;
    }

    //make game from steamGame (factory)
    public static Game fromSteamGame(Object steamGame) {
        return null;
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