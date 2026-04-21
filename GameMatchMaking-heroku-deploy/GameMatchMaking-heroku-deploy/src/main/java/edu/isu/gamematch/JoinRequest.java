package edu.isu.gamematch;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "join_requests",
       uniqueConstraints = @UniqueConstraint(columnNames = {"group_id", "requester_id"}))
public class JoinRequest {

    public enum Status { PENDING, ACCEPTED, DECLINED }

@Id
@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "join_req_seq")
@SequenceGenerator(name = "join_req_seq", sequenceName = "JOIN_REQ_SEQ", allocationSize = 1)
@Column(name = "request_id")
private int requestId;

    @ManyToOne
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @ManyToOne
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status = Status.PENDING;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt = LocalDateTime.now();

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    public JoinRequest() {}

    public JoinRequest(Group group, User requester) {
        this.group = group;
        this.requester = requester;
    }

    public int getRequestId() { return requestId; }
    public Group getGroup() { return group; }
    public void setGroup(Group group) { this.group = group; }
    public User getRequester() { return requester; }
    public void setRequester(User requester) { this.requester = requester; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public LocalDateTime getRequestedAt() { return requestedAt; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
}
