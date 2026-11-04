package io.github.ifenixi.supportflow.notification;
import org.springframework.web.bind.annotation.*;
import io.github.ifenixi.supportflow.identity.Actor;
import io.github.ifenixi.supportflow.common.PageView;
@RestController @RequestMapping("/api/v1/notifications")
public class NotificationController {
 private final NotificationClient client;private final Actor actor;
 public NotificationController(NotificationClient client,Actor actor){this.client=client;this.actor=actor;}
 @GetMapping(produces="application/json") public String inbox(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){PageView.validate(page,size);return client.inbox(actor.current().getId(),page,size);}
}
