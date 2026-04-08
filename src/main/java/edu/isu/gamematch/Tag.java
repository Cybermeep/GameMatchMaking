package edu.isu.gamematch;

import javax.persistence.*;

@Entity
@Table(name = "tags")
public class Tag{

    @Id
    @Column(name = "tag_name")
    private String tagName;

    @ManyToOne
    @JoinColumn(name = "game_id")
    private Game game;

    public Tag(){}
    
    public Tag(String tagName, Game game){
        this.tagName = tagName;
        this.game = game;
    }
    
    //getters and setters
    public String getTagName(){
        return tagName;
    }

    public void setTagName(String tagName){
        this.tagName = tagName;
    }

    public Game getGame(){
        return game;
    }

    public void setGame(Game game){
        this.game = game;
    }

    //methods
    @Override
    public String toString(){
        String gameLabel = "null";
        if(game !=null){
            gameLabel = game.getGameName();
        }
        return "Tag{tagName='" + tagName + '\'' + ", game=" + gameLabel + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o){
            return true;
        }
        if (!(o instanceof Tag)){
            return false;
        }
        Tag other = (Tag) o;

        boolean namesMatch = false;
        if (tagName != null && other.tagName != null){
            namesMatch = tagName.equalsIgnoreCase(other.tagName);
        }

        boolean gamesMatch = false;
        if (game != null && other.game != null){
            gamesMatch = game.equals(other.game);
        }

        return namesMatch && gamesMatch;
    }

    @Override
    public int hashCode(){
        int nameHash = 0;
        if (tagName != null){
            nameHash = tagName.toLowerCase().hashCode();
        }
        int gameHash = 0;
        if (game != null){
            gameHash = game.hashCode();
        }
        return 31 * nameHash + gameHash;
    }

}