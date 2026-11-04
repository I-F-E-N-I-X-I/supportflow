package io.github.ifenixi.supportflow.ticket;
import static io.github.ifenixi.supportflow.ticket.TicketDtos.*;
import java.util.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import io.github.ifenixi.supportflow.common.PageView;
@RestController @RequestMapping("/api/v1/tickets")
public class TicketController {
 private final TicketService service;
 public TicketController(TicketService service){this.service=service;}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) public TicketView create(@Valid @RequestBody Draft body,@RequestHeader(value="Idempotency-Key",required=false) String key){return service.create(body,key);}
 @GetMapping public PageView<TicketView> list(@RequestParam(defaultValue="") String query,@RequestParam(required=false) Status status,
  @RequestParam(required=false) Priority priority,@RequestParam(required=false) Long assigneeId,@RequestParam(defaultValue="0") int page,
  @RequestParam(defaultValue="20") int size,@RequestParam(defaultValue="createdAt") String sort,@RequestParam(defaultValue="false") boolean ascending){return service.search(query,status,priority,assigneeId,page,size,sort,ascending);}
 @GetMapping("/{id}") public TicketView get(@PathVariable long id){return service.get(id);}
 @GetMapping("/{id}/tags") public Set<TagView> tags(@PathVariable long id){return service.tags(id);}
 @PutMapping("/{id}") public TicketView revise(@PathVariable long id,@Valid @RequestBody Revision body){return service.revise(id,body);}
 @PatchMapping("/{id}/assignee") public TicketView assign(@PathVariable long id,@Valid @RequestBody Assignment body){return service.assign(id,body.assigneeId());}
 @PatchMapping("/{id}/status") public TicketView transition(@PathVariable long id,@Valid @RequestBody Transition body){return service.transition(id,body.status());}
 @PatchMapping("/{id}/compensation") public TicketView compensate(@PathVariable long id,@Valid @RequestBody Compensation body){return service.compensation(id,body.amount());}
 @PostMapping("/{id}/comments") @ResponseStatus(HttpStatus.CREATED) public CommentView comment(@PathVariable long id,@Valid @RequestBody AddComment body){return service.comment(id,body);}
 @GetMapping("/{id}/comments") public PageView<CommentView> comments(@PathVariable long id,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return service.comments(id,page,size);}
 @GetMapping("/{id}/history") public List<History> history(@PathVariable long id){return service.history(id);}
}
