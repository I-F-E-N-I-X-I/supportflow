package io.github.ifenixi.supportflow.worker;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class InternalSecurity extends OncePerRequestFilter {
  private final byte[] token;

  public InternalSecurity(@Value("${app.internal-token}") String token) {
    if (token.length() < 32) throw new IllegalStateException("Internal token too short");
    this.token = token.getBytes(StandardCharsets.UTF_8);
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws ServletException, IOException {
    String path = req.getRequestURI();
    if (path.equals("/actuator/health") || path.startsWith("/actuator/health/")) {
      chain.doFilter(req, res);
      return;
    }
    String header = req.getHeader("X-Internal-Token");
    if (header == null || !MessageDigest.isEqual(token, header.getBytes(StandardCharsets.UTF_8))) {
      res.setStatus(401);
      return;
    }
    chain.doFilter(req, res);
  }
}
