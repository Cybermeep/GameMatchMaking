/*
    Tag Class
*/
public class Tag{
    private String tagName;
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


}