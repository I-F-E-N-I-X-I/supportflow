package io.github.ifenixi.supportflow.common;

import org.springframework.http.HttpStatus;

public class DomainException extends RuntimeException {
  private final HttpStatus status;

  public DomainException(HttpStatus status, String message) {
    super(message);
    this.status = status;
  }

  public HttpStatus status() {
    return status;
  }

  public static DomainException missing() {
    return new DomainException(HttpStatus.NOT_FOUND, "Resource not found");
  }

  public static DomainException conflict(String message) {
    return new DomainException(HttpStatus.CONFLICT, message);
  }

  public static DomainException forbidden() {
    return new DomainException(HttpStatus.FORBIDDEN, "Insufficient permissions");
  }
}
