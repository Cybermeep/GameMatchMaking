package edu.isu.gamematch;

import edu.isu.gamematch.Game;
import edu.isu.gamematch.Tag;

import java.util.List;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashMap;
import java.sql.Timestamp;
import java.time.LocalDateTime;

public class GameOperations{

    //game methods

    /*adds tag to game (assuming it's user generated tags I forgot what Tag was supposed to be for)
    @param Tag The tag to add
    @return Tag
    */
    public Tag addTag(Game game, Tag tag){
        if(game == null || tag == null){
            return null;
        }
        List<Tag> tags = game.getTags();
        for(int i = 0; i < tags.size(); i++){
            if(tags.get(i).equals(tag)){
                return null; //already in
            }
        }
        tags.add(tag);
        return tag;
    }

    /*
    removes tag from game
    @param tag The tag to remove
    @return Tag
    */
   public Tag removeTag(Game game, String tagName) {  // Changed parameter from Tag to String
    if(game == null || tagName == null){
        return null;
    }        
    List<Tag> tags = game.getTags();
    for(int i = 0; i < tags.size(); i++){
        if(tags.get(i).getTagName().equalsIgnoreCase(tagName)){  // Compare by name
            Tag removed = tags.get(i);
            tags.remove(i);
            return removed;
        }
    }
    return null;
}

    /*
    checks if game has tag matching given tagname
    @param tagName 
    @return Tag
    */
    public Tag hasTag(Game game, String tagName){  // Changed parameter from Tag to String
    if(game == null || tagName == null){
        return null;
    }
    List<Tag> tags = game.getTags();
    for(int i = 0; i < tags.size(); i++){
        if(tags.get(i).getTagName().equalsIgnoreCase(tagName)){
            return tags.get(i);
        }
    }
    return null;
}

    /*
    Returns playtime in hours 
    @return double
    */
    public double getPlaytimeHours(Game game){
        if(game == null){
            return 0.0;
        }
        return game.getPlaytime() / 60.0;
    }

}