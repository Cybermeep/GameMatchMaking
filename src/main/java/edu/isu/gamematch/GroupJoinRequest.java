package edu.isu.gamematch;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "group_join_requests")
public class GroupJoinRequest {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_id")
    private int requestId;
    
    @ManyToOne
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User requestingUser;
    
    @Column(name = "status", nullable = false)
    private String status; // "PENDING", "ACCEPTED", "DECLINED"
    
    @Column(name = "requested_at", nullable = false)
    private Timestamp requestedAt;
    
    @Column(name = "invite_token")
    private String inviteToken;
    
    public GroupJoinRequest() {}
    
    public GroupJoinRequest(Group group, User requestingUser, String inviteToken) {
        this.group = group;
        this.requestingUser = requestingUser;
        this.inviteToken = inviteToken;
        this.status = "PENDING";
        this.requestedAt = new Timestamp(System.currentTimeMillis());
    }
    
    // Getters and Setters
    public int getRequestId() { return requestId; }
    public void setRequestId(int requestId) { this.requestId = requestId; }
    
    public Group getGroup() { return group; }
    public void setGroup(Group group) { this.group = group; }
    
    public User getRequestingUser() { return requestingUser; }
    public void setRequestingUser(User requestingUser) { this.requestingUser = requestingUser; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public Timestamp getRequestedAt() { return requestedAt; }
    public void setRequestedAt(Timestamp requestedAt) { this.requestedAt = requestedAt; }
    
    public String getInviteToken() { return inviteToken; }
    public void setInviteToken(String inviteToken) { this.inviteToken = inviteToken; }
}