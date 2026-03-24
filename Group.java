import java.util.ArrayList;

public class Group
{
    //attributes
    int groupID;
    ArrayList<User> members;
    ArrayList<Game> games;
    User groupOwner;

    //constructor
    public Group(int ID, User owner)
    {
        this.groupID = ID;
        this.groupOwner = owner;
        this.games = new ArrayList<Game>();
        this.members = new ArrayList<User>();
    }

    //additional methods
    public void createGroup()
    {
    }

    public void deleteGroup()
    {
        this.groupID = 0;
        this.groupOwner = null;
        this.games = null;
        this.members = null;
    }

    public boolean transferGroupOwnership(User newOwner)
    {
        this.groupOwner = newOwner;
        return true;
    }

    public String generateGroupInviteLink()
    {
        return "";
    }

    public boolean removeGroupMember(User member)
    {
        if(members.contains(member))
        {
            members.remove(member);
            return true;
        }
        else
        {
            return false;
        }
    }

    public boolean addGroupMember(User member)
    {
        members.add(member);
        return true;
    }

    private void getGameData(Game game, double[] totalHours, double[] recentHours, double[] daysSince)
    {
        // pulls user data for the game and stores it in the corresponding array
    }
    /***
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
    /***
     * Takes arrays containing doubles of all group members user data and passes them
     * for normalization and then finds the average value after. To calculate the final
     * score, averages are weighted and then added together.
     * 
     * @param totalHours array of doubles containing each group members total hours
     * @param recentHours array of doubles containing each group members recent hours
     * @param daysSince array of doubles containing each group members days since last played
     * @return final game score
     */
    private double calcGameScore(Game game)
    {
        int numMembers = members.size();
        double avgTotalHours = 0; double avgRecentHours = 0; double avgDaysSince = 0;

        double[] totalHours = new double[numMembers];
        double[] recentHours = new double[numMembers];
        double[] daysSince = new double[numMembers];
        getGameData(game, totalHours, recentHours, daysSince); // fills arrays with relevant data

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
    /***
     * Scores each game based on total hours, recent hours, and days since last played
     * and stores the scores in a ranking array, then uses that to sort the groups games list.
     */
    public void rankList()
    {
        int numGames = games.size();
        ArrayList<Double> ranks = new ArrayList<>();

        for (int i = 0; i < numGames; i++)
        {
            ranks.add(calcGameScore(games.get(i)));
        }

        for (int i = 0; i < numGames; i++)
        {
            int maxRank = i;
            for (int j = i + 1; j < numGames; j++)
            {
                if (ranks.get(j) > ranks.get(maxRank)) maxRank = j;
            }

            Collections.swap(games, i, maxRank);
            Collections.swap(ranks, i, maxRank);
        }
    }
}
