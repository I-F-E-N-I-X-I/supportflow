package io.github.ifenixi.supportflow.worker;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
public class NotificationController {
  private final NotificationStore store;

  public NotificationController(NotificationStore store) {
    this.store = store;
  }

  @GetMapping("/internal/notifications")
  public NotificationStore.Inbox inbox(
      @RequestParam long clientId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return store.inbox(clientId, page, size);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  ProblemDetail invalid() {
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Invalid parameters");
  }
}
