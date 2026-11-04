package io.github.ifenixi.supportflow.identity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/auth")
public class AuthController {
 public record Credentials(@NotNull @Pattern(regexp="[a-zA-Z0-9_-]{3,64}") String username,
 @NotNull @Size(min=12,max=64) @Pattern(regexp="[!-~]+") String password){}
 private final AuthService service;private final Actor actor;
 public AuthController(AuthService service,Actor actor){this.service=service;this.actor=actor;}
 @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED) public AuthService.UserView register(@Valid @RequestBody Credentials body){return service.register(body.username(),body.password());}
 @PostMapping("/login") public AuthService.Token login(@Valid @RequestBody Credentials body){return service.login(body.username(),body.password());}
 @GetMapping("/me") public AuthService.UserView me(){return AuthService.view(actor.current());}
}
