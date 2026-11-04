package io.github.ifenixi.supportflow;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.ifenixi.supportflow.common.DomainException;
import io.github.ifenixi.supportflow.identity.*;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;

class AuthServiceTest {
  @Test
  void unknownUserStillPerformsPasswordComparison() {
    var users = mock(AccountRepository.class);
    var passwords = mock(PasswordEncoder.class);
    when(passwords.encode(anyString())).thenReturn("dummy");
    when(users.findByUsername("missing")).thenReturn(Optional.empty());
    var service = new AuthService(users, passwords, mock(JwtEncoder.class));
    assertEquals(
        401,
        assertThrows(DomainException.class, () -> service.login("missing", "password"))
            .status()
            .value());
    verify(passwords).matches("password", "dummy");
  }
}
