package io.github.ifenixi.supportflow.learning;

import java.time.Duration;

/** Strategy + factory: choose SLA calculation without adding branches to callers. */
public final class Policies {
  public interface SlaPolicy {
    Duration target();
  }

  public enum Tier {
    STANDARD,
    PREMIUM
  }

  public static SlaPolicy forTier(Tier tier) {
    return switch (tier) {
      case STANDARD -> () -> Duration.ofHours(24);
      case PREMIUM -> () -> Duration.ofHours(4);
    };
  }

  public record Query(String category, int limit, boolean openOnly) {
    public static Builder builder() {
      return new Builder();
    }

    public static final class Builder {
      private String category = "all";
      private int limit = 20;
      private boolean openOnly;

      public Builder category(String value) {
        category = value;
        return this;
      }

      public Builder limit(int value) {
        limit = value;
        return this;
      }

      public Builder openOnly(boolean value) {
        openOnly = value;
        return this;
      }

      public Query build() {
        if (category == null || category.isBlank() || limit < 1 || limit > 100)
          throw new IllegalArgumentException("Invalid query");
        return new Query(category, limit, openOnly);
      }
    }
  }
}
