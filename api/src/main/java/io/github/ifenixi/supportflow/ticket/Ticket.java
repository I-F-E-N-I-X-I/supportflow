package io.github.ifenixi.supportflow.ticket;

import io.github.ifenixi.supportflow.identity.Account;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Entity
public class Ticket {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Version private long version;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "client_id")
  private Account client;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "assignee_id")
  private Account assignee;

  @Column(nullable = false, length = 160)
  private String title;

  @Column(nullable = false, length = 4000)
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private Status status;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private Priority priority;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal compensation = BigDecimal.ZERO;

  @Column(nullable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Instant updatedAt;

  private Instant resolvedAt;

  @Column(nullable = false, length = 80)
  private String requestKey;

  @Column(nullable = false, length = 64)
  private String requestHash;

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = "ticket_tag",
      joinColumns = @JoinColumn(name = "ticket_id"),
      inverseJoinColumns = @JoinColumn(name = "tag_id"))
  private Set<Tag> tags = new HashSet<>();

  @OneToMany(mappedBy = "ticket", fetch = FetchType.LAZY)
  private List<TicketComment> comments = new ArrayList<>();

  protected Ticket() {}

  public Ticket(
      Account client,
      String title,
      String description,
      Priority priority,
      String key,
      String hash,
      Set<Tag> tags) {
    this.client = client;
    this.title = title;
    this.description = description;
    this.priority = priority;
    requestKey = key;
    requestHash = hash;
    this.tags = new HashSet<>(tags);
    status = Status.NEW;
    createdAt = Instant.now();
    updatedAt = createdAt;
  }

  public void revise(String title, String description, Priority priority, Set<Tag> tags) {
    this.title = title;
    this.description = description;
    this.priority = priority;
    this.tags = new HashSet<>(tags);
    updatedAt = Instant.now();
  }

  public void assign(Account account) {
    assignee = account;
    updatedAt = Instant.now();
  }

  public void changeStatus(Status next) {
    status.transitionTo(next);
    status = next;
    updatedAt = Instant.now();
    resolvedAt =
        next == Status.RESOLVED ? updatedAt : next == Status.IN_PROGRESS ? null : resolvedAt;
  }

  public void compensate(BigDecimal amount) {
    compensation = amount;
    updatedAt = Instant.now();
  }

  public Long getId() {
    return id;
  }

  public long getVersion() {
    return version;
  }

  public Account getClient() {
    return client;
  }

  public Account getAssignee() {
    return assignee;
  }

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return description;
  }

  public Status getStatus() {
    return status;
  }

  public Priority getPriority() {
    return priority;
  }

  public BigDecimal getCompensation() {
    return compensation;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public Instant getResolvedAt() {
    return resolvedAt;
  }

  public Set<Tag> getTags() {
    return Collections.unmodifiableSet(tags);
  }

  public String getRequestHash() {
    return requestHash;
  }
}
