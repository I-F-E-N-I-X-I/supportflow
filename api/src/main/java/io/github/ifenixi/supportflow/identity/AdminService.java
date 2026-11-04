package io.github.ifenixi.supportflow.identity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import io.github.ifenixi.supportflow.common.DomainException;
@Service
public class AdminService {
 private final Actor actor;private final AccountRepository accounts;
 public AdminService(Actor actor,AccountRepository accounts){this.actor=actor;this.accounts=accounts;}
 @Transactional public AuthService.UserView role(long id,Role role){
  actor.admin();var user=accounts.lockById(id).orElseThrow(DomainException::missing);
  if(user.getRole()==Role.ADMIN || role==Role.ADMIN)throw DomainException.conflict("Admin role is managed by bootstrap only");
  user.changeRole(role);return AuthService.view(user);
 }
}
