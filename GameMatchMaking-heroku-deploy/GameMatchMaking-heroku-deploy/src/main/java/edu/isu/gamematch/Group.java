package edu.isu.gamematch;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "groups")
public class Group {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "group_seq")
    @SequenceGenerator(name = "group_seq", sequenceName = "GROUP_SEQ", allocationSize = 1)
    @Column(name = "group_id")
    private int groupID;

    @Column(name = "group_name")
    private String groupName;

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

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JoinRequest> joinRequests = new ArrayList<>();

    /** Minimum playtime (minutes) a game must have to appear in filtered list. 0 = no filter. */
    @Column(name = "min_playtime_minutes")
    private int minPlaytimeMinutes = 0;

    /** Comma-separated genre tags the group prefers (voted on). */
    @Column(name = "preferred_genres", length = 1024)
    private String preferredGenres = "";

    /** Token used for invite links (3.1.7). */
    @Column(name = "invite_token", unique = true)
    private String inviteToken;

    /** Token used for shareable final-games-list permalink (3.1.29). */
    @Column(name = "share_token", unique = true)
    private String shareToken;

    public Group() {}

    public Group(int ID, User owner) {
        this.groupID = ID;
        this.groupOwner = owner;
        this.games = new ArrayList<>();
        this.members = new LinkedHashSet<>();
        this.sessions = new LinkedHashSet<>();
        this.inviteToken = UUID.randomUUID().toString();
    }

    public void createGroup() {}

    public void deleteGroup() {
        this.groupID = 0;
        this.groupOwner = null;
        this.games = null;
        this.members = null;
    }

    // --- basic getters/setters ---
    public int getGroupID() { return groupID; }
    public Group setGroupID(int groupID) { this.groupID = groupID; return this; }

    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }

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

    public List<JoinRequest> getJoinRequests() { return joinRequests; }
    public void setJoinRequests(List<JoinRequest> joinRequests) { this.joinRequests = joinRequests; }

    public int getMinPlaytimeMinutes() { return minPlaytimeMinutes; }
    public void setMinPlaytimeMinutes(int minPlaytimeMinutes) { this.minPlaytimeMinutes = minPlaytimeMinutes; }

    public String getPreferredGenres() { return preferredGenres; }
    public void setPreferredGenres(String preferredGenres) { this.preferredGenres = preferredGenres; }

    public String getInviteToken() { return inviteToken; }
    public void setInviteToken(String inviteToken) { this.inviteToken = inviteToken; }

    public String getShareToken() { return shareToken; }
    public void setShareToken(String shareToken) { this.shareToken = shareToken; }
}
