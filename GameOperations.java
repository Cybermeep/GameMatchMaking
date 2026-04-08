import java.util.List;

public class GameOperations{

    //game methods

    /*adds tag to game (assuming it's user generated tags I forgot what Tag was supposed to be for)
    @param tag The tag to add
    */
    public boolean addTag(Game game, Tag tag){
        if(game == null || tag == null){
            return false;
        }
        for(int i = 0; i < tags.size(); i++){
            if(tags.get(i).equals(tag)){
                return false; //already in
            }
        }
        tags.add(tag);
        return true;
    }

    /*
    removes tag from game
    @param tag The tag to remove
    @return String
    */
    public String removeTag(Game game, String tagName){
        if(game == null || tagName == null){
            return null;
        }        
        List<Tag> tags = game.getTags();
        for(int i = 0; i < tags.size(); i++){
            if(tags.get(i).getTagName().equalsIgnoreCase(tagName)){
                String removed = tags.get(i).getTagName();
                tags.remove(i);
                return removed;
            }
        }
        return null;
    }

    /*
    checks if game has tag matching given tagname
    @param tagName 
    @return boolean
    */
    public boolean hasTag(Game game, String tagName){
        if(game == null || tagName == null){
            return false;
        }      
        List<Tag> tags = game.getTags();       
        for(int i = 0; i < tags.size(); i++){
            if(tags.get(i).getTagName().equalsIgnoreCase(tagName)){
                return true;
            }
        }
        return false;
    }

    /*
    Returns playtime in hours 
    @return double
    */
    public double getPlaytimeHours(Game game){
        if(game == null){
            return 0.0;
        }
        return game.getPlaytimeForever() / 60.0;
    }

}