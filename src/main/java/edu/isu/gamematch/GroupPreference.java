package edu.isu.gamematch;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.*;

@Entity
@Table(name = "group_preferences")
public class GroupPreference {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "group_pref_seq")
    @SequenceGenerator(name = "group_pref_seq", sequenceName = "GROUP_PREF_SEQ", allocationSize = 1)
    @Column(name = "preference_id")
    private int preferenceId;

    @ManyToOne
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @Column(name = "preference_name")
    private String preferenceName;

    @Column(name = "voting_closed")
    private boolean votingClosed = false;

    @OneToMany(mappedBy = "groupPreference", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<GroupPreferenceVote> votes = new HashSet<>();

    // constructors, getters, setters
    public GroupPreference() {}
    public GroupPreference(Group group, String preferenceName) {
        this.group = group;
        this.preferenceName = preferenceName;
    }
    // ... standard getters/setters ...
    public int getPreferenceId() { return preferenceId; }
    public void setPreferenceId(int id) { this.preferenceId = id; }
    public Group getGroup() { return group; }
    public void setGroup(Group group) { this.group = group; }
    public String getPreferenceName() { return preferenceName; }
    public void setPreferenceName(String name) { this.preferenceName = name; }
    public boolean isVotingClosed() { return votingClosed; }
    public void setVotingClosed(boolean closed) { this.votingClosed = closed; }
    public Set<GroupPreferenceVote> getVotes() { return votes; }
    public void setVotes(Set<GroupPreferenceVote> votes) { this.votes = votes; }
}