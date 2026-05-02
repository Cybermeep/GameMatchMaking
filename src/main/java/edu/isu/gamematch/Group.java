package edu.isu.gamematch;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "groups")
public class Group {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "group_seq")
    @SequenceGenerator(name = "group_seq", sequenceName = "GROUP_SEQ", allocationSize = 1)
    @Column(name = "group_id")
    private int groupID;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    private User groupOwner;

    @ManyToMany
    @JoinTable(
        name = "group_members",
        joinColumns = @JoinColumn(name = "group_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> members = new LinkedHashSet<>();

    @ManyToMany
    @JoinTable(
        name = "group_games",
        joinColumns = @JoinColumn(name = "group_id"),
        inverseJoinColumns = @JoinColumn(name = "game_id")
    )
    private List<Game> games = new ArrayList<>();

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<GroupSession> sessions = new LinkedHashSet<>();

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GroupVote> votes = new ArrayList<>();

    @Column(name = "group_name", nullable = false)
    private String groupName;

    @Column(name = "min_playtime_requirement")
    private int minPlaytimeRequirement = 0;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GroupJoinRequest> joinRequests = new ArrayList<>();

    @Column(name = "voting_closed")
    private boolean votingClosed = false;

    @Column(name = "share_token", unique = true)
    private String shareToken;

    // Constructors
    public Group() {}
    public Group(int ID, User owner) {
        this.groupID = ID;
        this.groupOwner = owner;
        this.games = new ArrayList<>();
        this.members = new LinkedHashSet<>();
        this.sessions = new LinkedHashSet<>();
    }

    // Getters and setters (all existing + new)
    public int getGroupID() { return groupID; }
    public Group setGroupID(int groupID) { this.groupID = groupID; return this; }
    public User getGroupOwner() { return groupOwner; }
    public Group setGroupOwner(User newOwner) { this.groupOwner = newOwner; return this; }
    public Set<User> getMembers() { return members; }
    public Group setMembers(Set<User> members) { this.members = members; return this; }
    public User addGroupMember(User member) { members.add(member); return member; }
    public User removeGroupMember(User member) { members.remove(member); return member; }
    public List<Game> getGames() { return games; }
    public Group setGames(List<Game> games) { this.games = games; return this; }
    public Game addGame(Game game) { games.add(game); return game; }
    public Game removeGame(Game game) { games.remove(game); return game; }
    public Set<GroupSession> getSessions() { return sessions; }
    public Group setSessions(Set<GroupSession> sessions) { this.sessions = sessions; return this; }
    public GroupSession addGroupSession(GroupSession session) { sessions.add(session); return session; }
    public GroupSession removeGroupSession(GroupSession session) { sessions.remove(session); return session; }
    public List<GroupVote> getVotes() { return votes; }
    public void setVotes(List<GroupVote> votes) { this.votes = votes; }
    public void addVote(GroupVote vote) { votes.add(vote); vote.setGroup(this); }
    public void removeVote(GroupVote vote) { votes.remove(vote); vote.setGroup(null); }
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    public int getMinPlaytimeRequirement() { return minPlaytimeRequirement; }
    public void setMinPlaytimeRequirement(int minPlaytimeRequirement) { this.minPlaytimeRequirement = minPlaytimeRequirement; }
    public List<GroupJoinRequest> getJoinRequests() { return joinRequests; }
    public void setJoinRequests(List<GroupJoinRequest> joinRequests) { this.joinRequests = joinRequests; }
    public void addJoinRequest(GroupJoinRequest request) { joinRequests.add(request); request.setGroup(this); }
    public void removeJoinRequest(GroupJoinRequest request) { joinRequests.remove(request); request.setGroup(null); }
    public String generateInviteToken() { return java.util.UUID.randomUUID().toString(); }
    public boolean isVotingClosed() { return votingClosed; }
    public void setVotingClosed(boolean votingClosed) { this.votingClosed = votingClosed; }

    public String getShareToken() { return shareToken; }
    public void setShareToken(String shareToken) { this.shareToken = shareToken; }

    public void createGroup() {}
    public void deleteGroup() { /* required by old code */ }
}