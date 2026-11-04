package io.github.ifenixi.supportflow.ticket;

import io.github.ifenixi.supportflow.common.DomainException;

public enum Status {
  NEW,
  IN_PROGRESS,
  RESOLVED,
  CLOSED;

  public void transitionTo(Status next) {
    boolean valid =
        switch (this) {
          case NEW -> next == IN_PROGRESS;
          case IN_PROGRESS -> next == RESOLVED;
          case RESOLVED -> next == CLOSED || next == IN_PROGRESS;
          case CLOSED -> false;
        };
    if (!valid)
      throw DomainException.conflict("Transition from " + this + " to " + next + " is not allowed");
  }
}
