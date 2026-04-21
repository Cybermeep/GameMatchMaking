package edu.isu.gamematch;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "group_sessions")
/**
 * Holds data pertaining to a specific group session.
 */
public class GroupSession {
 @Id
@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "group_session_seq")
@SequenceGenerator(name = "group_session_seq", sequenceName = "GROUP_SESSION_SEQ", allocationSize = 1)
@Column(name = "session_id")
private int sessionID;

    @ManyToOne
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @ManyToOne
    @JoinColumn(name = "game_id")
    private Game game;

    @ManyToMany
    @JoinTable(
        name = "session_members",
        joinColumns = @JoinColumn(name = "session_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> members = new LinkedHashSet<>();

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "scheduled_date")
    private LocalDateTime scheduledDate;

    @Column(name = "duration_minutes")
    private int duration;

    @Column(name = "active")
    private boolean active;

    // Default constructor required by JPA
    public GroupSession() {
    }

    public GroupSession(int sessionID, Group group, Game game, Set<User> members, 
                       LocalDateTime scheduledDate, int duration) {
        this.sessionID = sessionID;
        this.group = group;
        this.game = game;
        this.members = members;
        this.scheduledDate = scheduledDate;
        this.duration = duration;
        this.active = true;
    }

    public GroupSession(Group group) {
        this.group = group;
    }

    // getters and setters
    public int getSessionID() {
        return sessionID;
    }
    public GroupSession setSessionID(int sessionID) {
        this.sessionID = sessionID;
        return this;
    }
    public Group getGroup() {
        return group;
    }
    public GroupSession setGroup(Group group) {
        if (this.group != null) {
            this.group.getSessions().remove(this);
        }
        this.group = group;
        if (group != null && !group.getSessions().contains(this)) {
            group.getSessions().add(this);
        }
        return this;
    }
    public Game getGame() {
        return game;
    }
    public GroupSession setGame(Game game) {
        this.game = game;
        return this;
    }
    public Set<User> getMembers() {
        return members;
    }
    public GroupSession setMembers(Set<User> members) {
        this.members = members;
        return this;
    }
    public User addMember(User member) {
        members.add(member);
        return member;
    }
    public User removeMember(User member) {
        members.remove(member);
        return member;
    }
    public LocalDateTime getStartTime() {
        return startTime;
    }
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }
    public LocalDateTime getScheduledDate() {
        return scheduledDate;
    }
    public GroupSession setScheduledDate(LocalDateTime scheduledDate) {
        this.scheduledDate = scheduledDate;
        return this;
    }
    public int getDuration() {
        return duration;
    }
    public GroupSession setDuration(int duration) {
        this.duration = duration;
        return this;
    }
    public boolean isActive() {
        return active;
    }
    public GroupSession setActive(boolean active) {
        this.active = active;
        return this;
    } 

    public LocalDateTime getEndTime() {
        if (startTime != null) {
            return startTime.plusMinutes(duration);
        }
        return null;
    }

    public boolean isInProgress() {
        if (startTime != null) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime endTime = getEndTime();
            return !now.isBefore(startTime) && (endTime == null || now.isBefore(endTime));
        }
        return false;
    }

    @Override
    public String toString() {
        return "GroupSession{" +
            "sessionID=" + sessionID +
            ", group=" + (group != null ? group.getGroupID() : "null") +
            ", game=" + (game != null ? game.getGameName() : "null") +
            ", startTime=" + startTime +
            ", duration=" + duration + " minutes" +
            '}';
    }
}