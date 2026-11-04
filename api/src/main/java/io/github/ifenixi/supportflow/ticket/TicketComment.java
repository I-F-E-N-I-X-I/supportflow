package io.github.ifenixi.supportflow.ticket;
import jakarta.persistence.*;
import java.time.Instant;
import io.github.ifenixi.supportflow.identity.Account;
@Entity @Table(name="ticket_comment")
public class TicketComment {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="ticket_id") private Ticket ticket;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="author_id") private Account author;
 @Column(nullable=false,length=2000) private String body;
 @Column(nullable=false) private boolean internal;
 @Column(nullable=false) private Instant createdAt;
 protected TicketComment(){}
 public TicketComment(Ticket ticket,Account author,String body,boolean internal){this.ticket=ticket;this.author=author;this.body=body;this.internal=internal;createdAt=Instant.now();}
 public Long getId(){return id;}public Account getAuthor(){return author;}public String getBody(){return body;}public boolean isInternal(){return internal;}public Instant getCreatedAt(){return createdAt;}
}
