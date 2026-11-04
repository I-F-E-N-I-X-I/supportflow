package io.github.ifenixi.supportflow.common;

import java.util.List;
import org.springframework.data.domain.Page;

public record PageView<T>(List<T> items, int page, int size, long total) {
  public static <T> PageView<T> of(Page<T> p) {
    return new PageView<>(p.getContent(), p.getNumber(), p.getSize(), p.getTotalElements());
  }

  public static void validate(int page, int size) {
    if (page < 0 || page > 10000 || size < 1 || size > 100)
      throw new IllegalArgumentException("Invalid page");
  }
}
