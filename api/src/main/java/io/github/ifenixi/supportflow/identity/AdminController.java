package io.github.ifenixi.supportflow.identity;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminController {
  public record ChangeRole(@NotNull Role role) {}

  private final AdminService service;

  public AdminController(AdminService service) {
    this.service = service;
  }

  @PatchMapping("/{id}/role")
  public AuthService.UserView role(@PathVariable long id, @Valid @RequestBody ChangeRole body) {
    return service.role(id, body.role());
  }
}
