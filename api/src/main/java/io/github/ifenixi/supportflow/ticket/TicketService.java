package io.github.ifenixi.supportflow.ticket;
import static io.github.ifenixi.supportflow.ticket.TicketDtos.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import io.github.ifenixi.supportflow.common.*;
import io.github.ifenixi.supportflow.identity.*;
import com.fasterxml.jackson.databind.ObjectMapper;
@Service @Transactional(readOnly=true)
public class TicketService {
 private final TicketRepository tickets;private final AccountRepository accounts;private final TagRepository tags;
 private final CommentRepository comments;private final Actor actor;private final JdbcTemplate jdbc;private final ObjectMapper json;
 public TicketService(TicketRepository tickets,AccountRepository accounts,TagRepository tags,CommentRepository comments,Actor actor,JdbcTemplate jdbc,ObjectMapper json){this.tickets=tickets;this.accounts=accounts;this.tags=tags;this.comments=comments;this.actor=actor;this.jdbc=jdbc;this.json=json;}
 @Transactional public TicketView create(Draft body,String key){
  if(key==null || !key.matches("[a-zA-Z0-9_-]{8,80}"))throw new IllegalArgumentException("Idempotency-Key required");
  var current=actor.current();
  // Lock the owner to serialize concurrent requests using the same idempotency key.
  var owner=accounts.lockById(current.getId()).orElseThrow(DomainException::missing);
  String hash=fingerprint(body);var existing=tickets.findByClientIdAndRequestKey(owner.getId(),key);
  if(existing.isPresent()){
   if(!existing.get().getRequestHash().equals(hash))throw DomainException.conflict("Idempotency key was used for another request");
   return TicketView.from(existing.get());
  }
  var ticket=tickets.saveAndFlush(new Ticket(owner,body.title().strip(),body.description().strip(),body.priority(),key,hash,resolveTags(body.tagIds())));
  event(ticket,owner,null);return TicketView.from(ticket);
 }
 public TicketView get(long id){return TicketView.from(visible(id,actor.current(),false));}
 public Set<TagView> tags(long id){var t=visible(id,actor.current(),false);var result=new LinkedHashSet<TagView>();t.getTags().stream().sorted(Comparator.comparing(Tag::getName)).forEach(tag->result.add(new TagView(tag.getId(),tag.getName())));return result;}
 public PageView<TicketView> search(String query,Status status,Priority priority,Long assignee,int page,int size,String sort,boolean ascending){
  PageView.validate(page,size);if(query.length()>160)throw new IllegalArgumentException("Search too long");
  if(!Set.of("createdAt","updatedAt","title","id").contains(sort))throw new IllegalArgumentException("Unknown sort");
  var current=actor.current();
  Specification<Ticket> specification=(root,q,cb)->{
   var clauses=new ArrayList<jakarta.persistence.criteria.Predicate>();
   if(!current.isStaff())clauses.add(cb.equal(root.get("client").get("id"),current.getId()));
   if(status!=null)clauses.add(cb.equal(root.get("status"),status));
   if(priority!=null)clauses.add(cb.equal(root.get("priority"),priority));
   if(assignee!=null)clauses.add(cb.equal(root.get("assignee").get("id"),assignee));
   if(!query.isBlank()){
    String escaped=query.toLowerCase(Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_");
    clauses.add(cb.like(cb.lower(root.get("title")),"%"+escaped+"%",'!'));
   }
   return cb.and(clauses.toArray(jakarta.persistence.criteria.Predicate[]::new));
  };
  var order=Sort.by(ascending?Sort.Direction.ASC:Sort.Direction.DESC,sort).and(Sort.by("id"));
  return PageView.of(tickets.findAll(specification,PageRequest.of(page,size,order)).map(TicketView::from));
 }
 @Transactional public TicketView revise(long id,Revision body){
  var current=actor.current();var ticket=visible(id,current,true);
  if(ticket.getStatus()!=Status.NEW)throw DomainException.conflict("Only NEW tickets can be edited");
  if(ticket.getVersion()!=body.version())throw DomainException.conflict("Stale ticket version");
  ticket.revise(body.title().strip(),body.description().strip(),body.priority(),resolveTags(body.tagIds()));tickets.flush();return TicketView.from(ticket);
 }
 @Transactional public TicketView assign(long id,long assigneeId){
  var current=actor.staff();var ticket=visible(id,current,true);
  if(ticket.getStatus()==Status.CLOSED)throw DomainException.conflict("Ticket is closed");
  if(current.getRole()!=Role.ADMIN && !current.getId().equals(assigneeId))throw DomainException.forbidden();
  var assignee=accounts.findById(assigneeId).orElseThrow(DomainException::missing);
  if(!assignee.isStaff())throw DomainException.conflict("Assignee must be SUPPORT or ADMIN");
  ticket.assign(assignee);tickets.flush();return TicketView.from(ticket);
 }
 @Transactional public TicketView transition(long id,Status target){
  var current=actor.current();var ticket=visible(id,current,true);
  if(!current.isStaff()){
   if(target!=Status.CLOSED || ticket.getStatus()!=Status.RESOLVED)throw DomainException.forbidden();
  }else if(current.getRole()!=Role.ADMIN && (ticket.getAssignee()==null || !ticket.getAssignee().getId().equals(current.getId())))throw DomainException.forbidden();
  if(target==Status.IN_PROGRESS && ticket.getAssignee()==null)throw DomainException.conflict("Assign ticket before starting work");
  Status previous=ticket.getStatus();ticket.changeStatus(target);tickets.flush();event(ticket,current,previous);return TicketView.from(ticket);
 }
 @Transactional public TicketView compensation(long id,BigDecimal amount){actor.admin();var ticket=tickets.lockById(id).orElseThrow(DomainException::missing);ticket.compensate(amount);tickets.flush();return TicketView.from(ticket);}
 @Transactional public CommentView comment(long id,AddComment body){
  var current=actor.current();var ticket=visible(id,current,true);
  if(ticket.getStatus()==Status.CLOSED)throw DomainException.conflict("Ticket is closed");
  if(body.internal()&&!current.isStaff())throw DomainException.forbidden();
  return CommentView.from(comments.saveAndFlush(new TicketComment(ticket,current,body.body().strip(),body.internal())));
 }
 public PageView<CommentView> comments(long id,int page,int size){PageView.validate(page,size);var current=actor.current();visible(id,current,false);return PageView.of(comments.visible(id,current.isStaff(),PageRequest.of(page,size,Sort.by("id"))).map(CommentView::from));}
 public List<History> history(long id){visible(id,actor.current(),false);return jdbc.query("SELECT * FROM status_history WHERE ticket_id=? ORDER BY id",(r,n)->new History(r.getLong("id"),r.getLong("actor_id"),r.getString("from_status"),r.getString("to_status"),r.getTimestamp("changed_at").toInstant()),id);}
 private Ticket visible(long id,Account current,boolean lock){
  var ticket=(lock?tickets.lockById(id):tickets.findById(id)).orElseThrow(DomainException::missing);
  if(!current.isStaff()&&!ticket.getClient().getId().equals(current.getId()))throw DomainException.missing();return ticket;
 }
 private Set<Tag> resolveTags(Set<Long> ids){var found=tags.findAllById(ids);if(found.size()!=ids.size())throw DomainException.missing();return new HashSet<>(found);}
 private String fingerprint(Draft body){
  try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json.writeValueAsBytes(List.of(body.title().strip(),body.description().strip(),body.priority(),new TreeSet<>(body.tagIds())))));}
  catch(Exception e){throw new IllegalStateException("Cannot fingerprint request",e);}
 }
 private void event(Ticket ticket,Account actor,Status from){
  Instant now=Instant.now();
  jdbc.update("INSERT INTO status_history(ticket_id,actor_id,from_status,to_status,changed_at) VALUES (?,?,?,?,?)",ticket.getId(),actor.getId(),from==null?null:from.name(),ticket.getStatus().name(),java.sql.Timestamp.from(now));
  jdbc.update("INSERT INTO outbox_event(id,ticket_id,client_id,status,occurred_at) VALUES (?,?,?,?,?)",UUID.randomUUID(),ticket.getId(),ticket.getClient().getId(),ticket.getStatus().name(),java.sql.Timestamp.from(now));
 }
}
