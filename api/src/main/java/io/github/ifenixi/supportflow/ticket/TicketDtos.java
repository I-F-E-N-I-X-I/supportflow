package io.github.ifenixi.supportflow.ticket;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
public final class TicketDtos {
 private TicketDtos(){}
 public record Draft(@NotBlank @Size(max=160) String title,@NotBlank @Size(max=4000) String description,
  @NotNull Priority priority,@NotNull @Size(max=10) Set<@NotNull @Positive Long> tagIds){}
 public record Revision(@NotNull @PositiveOrZero Long version,@NotBlank @Size(max=160) String title,
  @NotBlank @Size(max=4000) String description,@NotNull Priority priority,@NotNull @Size(max=10) Set<@NotNull @Positive Long> tagIds){}
 public record Transition(@NotNull Status status){}
 public record Assignment(@NotNull @Positive Long assigneeId){}
 public record Compensation(@NotNull @DecimalMin("0.00") @DecimalMax("9999999999.99") @Digits(integer=10,fraction=2) BigDecimal amount){}
 public record AddComment(@NotBlank @Size(max=2000) String body,boolean internal){}
 public record TagView(long id,String name){}
 public record TicketView(long id,long version,long clientId,Long assigneeId,String title,String description,Status status,Priority priority,
  BigDecimal compensation,Instant createdAt,Instant updatedAt,Instant resolvedAt) {
  public static TicketView from(Ticket t){return new TicketView(t.getId(),t.getVersion(),t.getClient().getId(),t.getAssignee()==null?null:t.getAssignee().getId(),t.getTitle(),t.getDescription(),t.getStatus(),t.getPriority(),t.getCompensation(),t.getCreatedAt(),t.getUpdatedAt(),t.getResolvedAt());}
 }
 public record CommentView(long id,long authorId,String body,boolean internal,Instant createdAt){
  public static CommentView from(TicketComment c){return new CommentView(c.getId(),c.getAuthor().getId(),c.getBody(),c.isInternal(),c.getCreatedAt());}
 }
 public record History(long id,long actorId,String fromStatus,String toStatus,Instant changedAt){}
}
