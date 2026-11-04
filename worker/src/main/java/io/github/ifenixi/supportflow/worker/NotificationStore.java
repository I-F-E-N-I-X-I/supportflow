package io.github.ifenixi.supportflow.worker;
import java.util.*;
import java.time.Instant;
import java.sql.Timestamp;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class NotificationStore {
 public record Notification(UUID eventId,long ticketId,String status,Instant occurredAt){}
 public record Inbox(List<Notification> items,int page,int size){}
 private final JdbcTemplate jdbc;
 public NotificationStore(JdbcTemplate jdbc){this.jdbc=jdbc;}
 @Transactional public void accept(StatusEvent event){
  if(event==null || event.eventId()==null || event.ticketId()<1 || event.clientId()<1 || event.occurredAt()==null || !Set.of("NEW","IN_PROGRESS","RESOLVED","CLOSED").contains(event.status()))throw new IllegalArgumentException("Invalid event");
  int inserted=jdbc.update("INSERT INTO notification(event_id,ticket_id,client_id,status,occurred_at) VALUES (?,?,?,?,?) ON CONFLICT(event_id) DO NOTHING",event.eventId(),event.ticketId(),event.clientId(),event.status(),Timestamp.from(event.occurredAt()));
  if(inserted==0){
   boolean same=Boolean.TRUE.equals(jdbc.queryForObject("SELECT ticket_id=? AND client_id=? AND status=? FROM notification WHERE event_id=?",Boolean.class,event.ticketId(),event.clientId(),event.status(),event.eventId()));
   if(!same)throw new IllegalArgumentException("Conflicting event id");
  }
 }
 public Inbox inbox(long clientId,int page,int size){
  if(clientId<1 || page<0 || page>10000 || size<1 || size>100)throw new IllegalArgumentException("Invalid pagination");
  return new Inbox(jdbc.query("SELECT * FROM notification WHERE client_id=? ORDER BY occurred_at DESC,event_id LIMIT ? OFFSET ?",(r,n)->new Notification(r.getObject("event_id",UUID.class),r.getLong("ticket_id"),r.getString("status"),r.getTimestamp("occurred_at").toInstant()),clientId,size,page*size),page,size);
 }
}
