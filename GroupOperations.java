/*
    GroupOperations class
*/

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Manages operations on the Group class, including its related classes like GroupSession, GroupVote, etc...
 */
public class GroupOperations{
    private List<Game> gameList;

    public GroupOperations(){
        this.gameList = new ArrayList<Game>();
    }
    public GroupOperations(List<Game> gameList){
        this.gameList = gameList;
    }

    public List<Game> getGameList(){
        return gameList;
    }
    public void setGameList(List<Game> gameList){
        this.gameList = gameList;
    }

 //methods

    //todo group operations [MORE METHODS HERE]


    /**
     * Generates a unique invite link for the group that can be shared with others to join the group.
     * @return a unique invite link as a String
     */
    public String generateGroupInviteLink()
    {
        return "";
    }

    // ==================================================
    //              GAME RANKING OPERATIONS
    // ==================================================
    //#region

    /**
     * Fills passed arrays with each group members data for the provided game.
     * @param group group being operated on
     * @param game game to pull data for
     * @param totalHours each members total hours on the provided game
     * @param recentHours each members recent hours (two weeks) on the provided game
     * @param daysSince each members time since last played for the provided game
     */
    private void getGameData(Group group, Game game, double[] totalHours, double[] recentHours, double[] daysSince)
    {
        // this implementation relies on fully implementing the UserGame class, including
        // a method in User to pull the UserGame data for a particular game, which is not currently implemented
        
        int i = 0;
        for (User member : group.getMembers())
        {
            UserGame userGameData = member.getUserGameData(game);
            totalHours[i] = userGameData.getTotalHours();
            recentHours[i] = userGameData.getRecentHours();
            daysSince[i] = userGameData.getDaysSincePlayed();
            i++;
        }
    }
    /**
     * Normalizes an array of doubles:
     * 1. Optional: Log transforms data to reduce impact of extremely large outliers
     * 2. Find min/max values in data
     * 3. Equalizes data scale using min/max scaling
     * 4. Optional: Inverts scale
     * 
     * @param data array of doubles to be normalized
     * @param logTransform t/f indicates if data should be log transformed
     * @param invert t/f indicates if scale should be inverted
     */
    private void normalize(double[] data, boolean logTransform, boolean invert)
    {
        int numMembers = members.size();
        double max = -999999; double min = 999999;
        
        for (int i = 0; i < numMembers; i++)
        {
            if (logTransform) data[i] = Math.log1p(data[i]); // not sure if this helps
            min = Math.min(min, data[i]);
            max = Math.max(max, data[i]);
        }
        
        double range = max - min;
        if (range == 0) range += 1;
        
        for (int i = 0; i < numMembers; i++)
        {
            data[i] = (data[i] - min) / range;
            if (invert) data[i] = 1.0 - data[i];
        }
    }
    /**
     * Takes arrays containing doubles of all group members user data and passes them
     * for normalization and then finds the average value after. To calculate the final
     * score, averages are weighted and then added together.
     * 
     * @param group group being operated on
     * @param game game being scored
     * @return final game score
     */
    private double calcGameScore(Group group, Game game)
    {
        int numMembers = group.getMembers().size();
        double avgTotalHours = 0; double avgRecentHours = 0; double avgDaysSince = 0;

        double[] totalHours = new double[numMembers];
        double[] recentHours = new double[numMembers];
        double[] daysSince = new double[numMembers];
        getGameData(group, game, totalHours, recentHours, daysSince); // fills arrays with relevant data

        // performs min/max scaling so each contributes equally before weighting, and optionally
        // log transforms? or inverts the scale
        normalize(totalHours, true, false);
        normalize(recentHours, true, false);
        normalize(daysSince, false, true);

        for (int i = 0; i < numMembers; i++)
        {
            avgTotalHours += totalHours[i];
            avgRecentHours += recentHours[i];
            avgDaysSince += daysSince[i];
        }
        avgTotalHours /= numMembers;
        avgRecentHours /= numMembers;
        avgDaysSince /= numMembers;

        // placeholder weighting values
        double weightedScore = 0.5*avgTotalHours + 0.25*avgRecentHours + 0.25*avgDaysSince;

        return weightedScore;
    }
    /**
     * Scores each game based on total hours, recent hours, and days since last played
     * and stores the scores in a HashMap, then uses that to sort the groups games list.
     */
    public void rankList(Group group)
    {
        Map<Game, Double> gameScores = new HashMap<>();
        List<Game> games = group.getGames();

        for (Game game : games)
        {
            double score = calcGameScore(group, game);
            gameScores.put(game, score);
        }

        // sorts games list based on scores, then alphabetically for ties
        games.sort(Comparator.comparingDouble((Game game) -> gameScores.get(game))
                             .reversed()
                             .thenComparing(Game::getGameName));
    }
    //#endregion

    // ==================================================
    //             GROUP SESSION OPERATIONS
    // ==================================================
    //#region

    /**
     * Returns a set of all sessions for the provided group.
     * @param group group being operated on
     * @return set of GroupSession objects for the provided group
     */
    public Set<GroupSession> retrieveGroupSessionHistory(Group group)
    {
        return group.getSessions();
    }
    /**
     * Schedules a new group session for the provided group with the game, members, date, and duration. Creates a new GroupSession object and adds it to the groups sessions list.
     * @param group group being operated on
     * @param game game scheduled for session
     * @param members set of members participating in session
     * @param scheduledDate time scheduled for session
     * @param duration scheduled duration of session in minutes
     * @return newly created GroupSession object
     */
    public GroupSession scheduleSession(Group group, Game game, Set<User> members, DateTime scheduledDate, int duration)
    {
        int sessionID = group.getSessions().size() + 1;
        
        GroupSession newSession = new GroupSession(sessionID, group, game, members, scheduledDate, duration);
        group.addSession(newSession);
        
        return newSession;
    }
    /**
     * Cancels a scheduled session by setting its active status to false. Session remains in groups session history but is marked as inactive.
     * @param group group being operated on
     * @param session session being cancelled
     * @return updated GroupSession object with active status set to false
     */
    public GroupSession cancelSession(Group group, GroupSession session)
    {
        session.setActive(false);
        return session;
    }
    //#endregion

    // ==================================================
    //               GROUP VOTE OPERATIONS
    // ==================================================
    //#region
    /*
    records vote for given game along with timestamp. logic is to only cast once and update with updateVote.
    @param selectedGame - selectedGame to vote for
    @return boolean
    */
    public boolean castVote(GroupVote vote, Game selectedGame){
        if(vote == null){
            return false;
        }
        if(vote.getGame() != null){
            System.out.println("Vote already cast. Use updateVote() to change it.");
            return false;
        }
        if(selectedGame == null){
            System.out.println("Game not found");
            return false;
        }
        vote.setGame(selectedGame);
        vote.setTimestamp(new Timestamp(System.currentTimeMillis()));
        System.out.println("Vote cast for: " + selectedGame.getGameName());
        return true;
    }
    /**
    changes existing vote to different game and updates timestamp.
    @param newGame - newGame to vote for
    @return boolean
     */
    public boolean updateVote(GroupVote vote, Game newGame){
        if(vote == null){
            return false;
        }
        if(vote.getGame() == null){
            System.out.println("No vote to update. Use castVote() first.");
            return false;
        }
        if(newGame == null){
            System.out.println("Game not found");
            return false;
        }
        System.out.println("Vote changed from '" + vote.getGame().getGameName()+ "' to '" + newGame.getGameName() + "'.");
        vote.setGame(newGame);
        vote.setTimestamp(new Timestamp(System.currentTimeMillis()));
        return true;
    }
    /**
    tallies all votes in list, returns map of Game to vote count
    @param votes - list of GroupVotes to tally
    @return Map
     */
    public static Map<Game, Integer> tallyVotes(List<GroupVote> votes){
        Map<Game, Integer> tally = new HashMap<Game,Integer>();
        if(votes == null || votes.isEmpty()){
            System.out.println("No votes to tally");
            return tally;
        }
        for(int i = 0; i < votes.size(); i++){
            GroupVote vote = votes.get(i);
            if(vote.getGame() != null){
                Game votedGame = vote.getGame();
                if(tally.containsKey(votedGame)){
                    tally.put(votedGame, tally.get(votedGame) + 1);
                }
                else{
                    tally.put(votedGame, 1);
                }
            }
        }
        System.out.println("Vote Tally:");
        for(Map.Entry<Game, Integer> entry : tally.entrySet()){
            System.out.println(entry.getKey().getGameName() + ": " + entry.getValue() + " vote(s)");
        }
        return tally;
    }
    /**
    determines winner from tally
    @param tally - result of tallyVotes
    @return Game
    */
    public static Game getWinner(Map<Game, Integer> tally){
        if(tally == null || tally.isEmpty()){
            return null;
        }

        Game winner = null;
        int topCount = 0;
        boolean tie = false;

        for(Map.Entry<Game, Integer> entry : tally.entrySet()){
            int count = entry.getValue();
            if(count > topCount){
                topCount = count;
                winner = entry.getKey();
                tie = false;
            }
            else if(count == topCount){
                tie = true;
            }
        }

        if(tie){
            System.out.println("Tie has occurred");
            return null;
        }
        return winner;
    }    
    //#endregion
}