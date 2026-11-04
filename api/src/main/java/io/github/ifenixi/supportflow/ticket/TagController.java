package io.github.ifenixi.supportflow.ticket;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tags")
public class TagController {
  public record Input(@NotBlank @Size(max = 40) @Pattern(regexp = "[a-zA-Z0-9_-]+") String name) {}

  private final TagService service;

  public TagController(TagService service) {
    this.service = service;
  }

  @GetMapping
  public List<TicketDtos.TagView> list() {
    return service.list();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public TicketDtos.TagView create(@Valid @RequestBody Input body) {
    return service.create(body.name());
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable long id) {
    service.delete(id);
  }
}
