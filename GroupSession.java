import java.time.LocalDateTime;
import edu.isu.gamematch.Group;
import edu.isu.gamematch.Game;

/**
 * Holds data pertaining to a specific group session.
 */
public class GroupSession
{
    private int sessionID;
    private Group group;
    private Game game;
    private Set<User> members;
    private DateTime scheduledDate;
    private int duration;
    private boolean active;
    
    public GroupSession(int sessionID, Group group, Game game, Set<User> members, DateTime scheduledDate, int duration) {
        this.sessionID = sessionID;
        this.group = group;
        this.game = game;
        this.members = members;
        this.scheduledDate = scheduledDate;
        this.duration = duration;
        this.active = true;
    }

    // getters and setters, excluding setters for sessionID and group since those should be immutable after creation
    public int getSessionID() {
        return sessionID;
    }
    public Group getGroup() {
        return group;
    }
    public Game getGame() {
        return game;
    }
    public void setGame(Game game) {
        this.game = game;
    }
    public Set<User> getMembers() {
        return members;
    }
    public User addMember(User member) {
        members.add(member);
        return member;
    }
    public User removeMember(User member) {
        members.remove(member);
        return member;
    }
    public DateTime getScheduledDate() {
        return scheduledDate;
    }
    public void setScheduledDate(DateTime scheduledDate) {
        this.scheduledDate = scheduledDate;
    }
    public int getDuration() {
        return duration;
    }
    public void setDuration(int duration) {
        this.duration = duration;
    }
    public boolean isActive() {
        return active;
    }
    public void setActive(boolean active) {
        this.active = active;
    } 
}