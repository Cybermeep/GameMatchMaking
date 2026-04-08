package edu.isu.gamematch;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "group_sessions")
public class GroupSession
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_id")
    private int sessionID;

    @ManyToOne
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @ManyToOne
    @JoinColumn(name = "game_id")
    private Game game;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "scheduled_date")
    private LocalDateTime scheduledDate;

    @Column(name = "duration_minutes")
    private int duration;

    // Default constructor required by JPA
    public GroupSession() {
    }

    public GroupSession(Group group, Game game, LocalDateTime startTime, int duration) {
        this.group = group;
        this.game = game;
        this.startTime = startTime;
        this.duration = duration;
    }

    public GroupSession(Group group) {
        this.group = group;
    }

    // Getters and setters
    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public int getSessionID() {
        return sessionID;
    }

    public void setSessionID(int sessionID) {
        this.sessionID = sessionID;
    }

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        // Remove from previous group's sessions list
        if (this.group != null) {
            this.group.getSessions().remove(this);
        }

        // Set new group
        this.group = group;

        // Add to new group's sessions list
        if (group != null && !group.getSessions().contains(this)) {
            group.getSessions().add(this);
        }
    }

    public Game getGame() {
        return game;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public LocalDateTime getScheduledDate() {
        return scheduledDate;
    }

    public void setScheduledDate(LocalDateTime scheduledDate) {
        this.scheduledDate = scheduledDate;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    // Utility methods
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