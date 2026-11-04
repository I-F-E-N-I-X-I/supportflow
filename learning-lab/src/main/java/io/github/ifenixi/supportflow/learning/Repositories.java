package io.github.ifenixi.supportflow.learning;

import java.util.*;
import java.util.function.Function;

public final class Repositories {
  public interface Repository<K, V> {
    void save(V value);

    Optional<V> find(K key);

    boolean delete(K key);
  }

  public abstract static class MemoryRepository<K, V> implements Repository<K, V> {
    private final Map<K, V> values = new HashMap<>();
    private final Function<V, K> key;

    protected MemoryRepository(Function<V, K> key) {
      this.key = Objects.requireNonNull(key);
    }

    protected abstract void validate(V value);

    @Override
    public final void save(V value) {
      validate(value);
      values.put(key.apply(value), value);
    }

    public final void save(Collection<V> values) {
      values.forEach(this::save);
    }

    @Override
    public Optional<V> find(K key) {
      return Optional.ofNullable(values.get(key));
    }

    @Override
    public boolean delete(K key) {
      return values.remove(key) != null;
    }
  }

  public static final class TicketKey {
    private final long value;

    public TicketKey(long value) {
      if (value < 1) throw new IllegalArgumentException("Positive id required");
      this.value = value;
    }

    public long value() {
      return value;
    }

    @Override
    public boolean equals(Object other) {
      return this == other || other instanceof TicketKey key && value == key.value;
    }

    @Override
    public int hashCode() {
      return Long.hashCode(value);
    }
  }

  public record Item(TicketKey id, String title) {}

  public static final class Tickets extends MemoryRepository<TicketKey, Item> {
    public Tickets() {
      super(Item::id);
    }

    @Override
    protected void validate(Item item) {
      if (item == null || item.id() == null || item.title() == null || item.title().isBlank())
        throw new IllegalArgumentException("Invalid item");
    }
  }
}
