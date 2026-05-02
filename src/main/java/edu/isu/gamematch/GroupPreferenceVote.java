package edu.isu.gamematch;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "group_preference_votes")
public class GroupPreferenceVote {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "group_pref_vote_seq")
    @SequenceGenerator(name = "group_pref_vote_seq", sequenceName = "GROUP_PREF_VOTE_SEQ", allocationSize = 1)
    private int voteId;

    @ManyToOne
    @JoinColumn(name = "preference_id")
    private GroupPreference groupPreference;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "vote_value")
    private boolean approved; // true = yes

    @Column(name = "vote_time")
    private Timestamp voteTime;

    // constructors, getters, setters
    public GroupPreferenceVote() {}
    public GroupPreferenceVote(GroupPreference pref, User user, boolean approved) {
        this.groupPreference = pref;
        this.user = user;
        this.approved = approved;
        this.voteTime = new Timestamp(System.currentTimeMillis());
    }
    // ... standard ...
    public int getVoteId() { return voteId; }
    public void setVoteId(int id) { this.voteId = id; }
    public GroupPreference getGroupPreference() { return groupPreference; }
    public void setGroupPreference(GroupPreference pref) { this.groupPreference = pref; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public boolean isApproved() { return approved; }
    public void setApproved(boolean approved) { this.approved = approved; }
    public Timestamp getVoteTime() { return voteTime; }
    public void setVoteTime(Timestamp t) { this.voteTime = t; }
}