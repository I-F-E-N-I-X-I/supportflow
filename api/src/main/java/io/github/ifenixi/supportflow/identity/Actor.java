package io.github.ifenixi.supportflow.identity;

import io.github.ifenixi.supportflow.common.DomainException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class Actor {
  private final AccountRepository accounts;

  public Actor(AccountRepository accounts) {
    this.accounts = accounts;
  }

  public Account current() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null) throw DomainException.forbidden();
    try {
      return accounts
          .findById(Long.parseLong(authentication.getName()))
          .orElseThrow(DomainException::forbidden);
    } catch (NumberFormatException e) {
      throw DomainException.forbidden();
    }
  }

  public Account staff() {
    var a = current();
    if (!a.isStaff()) throw DomainException.forbidden();
    return a;
  }

  public Account admin() {
    var a = current();
    if (a.getRole() != Role.ADMIN) throw DomainException.forbidden();
    return a;
  }
}
