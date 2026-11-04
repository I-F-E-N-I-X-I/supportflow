package io.github.ifenixi.supportflow.identity;

import io.github.ifenixi.supportflow.common.DomainException;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
  public record UserView(long id, String username, Role role) {}

  public record Token(String accessToken, String tokenType, long expiresIn) {}

  private final AccountRepository accounts;
  private final PasswordEncoder passwords;
  private final JwtEncoder encoder;
  private final String dummyHash;

  public AuthService(AccountRepository accounts, PasswordEncoder passwords, JwtEncoder encoder) {
    this.accounts = accounts;
    this.passwords = passwords;
    this.encoder = encoder;
    dummyHash = passwords.encode("timing-placeholder-password");
  }

  @Transactional
  public UserView register(String username, String password) {
    var account =
        accounts.saveAndFlush(new Account(username, passwords.encode(password), Role.CLIENT));
    return view(account);
  }

  public Token login(String username, String password) {
    var account = accounts.findByUsername(username);
    boolean matches =
        passwords.matches(password, account.map(Account::getPasswordHash).orElse(dummyHash));
    if (account.isEmpty() || !matches)
      throw new DomainException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
    var now = Instant.now();
    var claims =
        JwtClaimsSet.builder()
            .issuer("supportflow")
            .subject(account.get().getId().toString())
            .issuedAt(now)
            .expiresAt(now.plusSeconds(900))
            .build();
    return new Token(
        encoder
            .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
            .getTokenValue(),
        "Bearer",
        900);
  }

  public static UserView view(Account a) {
    return new UserView(a.getId(), a.getUsername(), a.getRole());
  }
}
