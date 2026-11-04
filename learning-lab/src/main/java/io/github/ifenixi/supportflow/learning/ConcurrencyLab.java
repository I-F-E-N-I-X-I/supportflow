package io.github.ifenixi.supportflow.learning;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;

public final class ConcurrencyLab {
  public static final class Counter {
    private long value;

    public synchronized void increment() {
      value++;
    }

    public synchronized long value() {
      return value;
    }
  }

  public static final class StopSignal {
    private volatile boolean stopped;

    public void stop() {
      stopped = true;
    }

    public boolean isStopped() {
      return stopped;
    }
  }

  public static final class TtlCache<K, V> {
    private record Entry<V>(V value, Instant expiresAt) {}

    private final ConcurrentHashMap<K, Entry<V>> values = new ConcurrentHashMap<>();
    private final Clock clock;

    public TtlCache(Clock clock) {
      this.clock = clock;
    }

    public void put(K key, V value, Duration ttl) {
      if (ttl.isNegative() || ttl.isZero())
        throw new IllegalArgumentException("Positive TTL required");
      values.put(key, new Entry<>(Objects.requireNonNull(value), clock.instant().plus(ttl)));
    }

    public Optional<V> get(K key) {
      var entry = values.get(key);
      if (entry == null) return Optional.empty();
      if (!clock.instant().isBefore(entry.expiresAt())) {
        values.remove(key, entry);
        return Optional.empty();
      }
      return Optional.of(entry.value());
    }

    public void invalidate(K key) {
      values.remove(key);
    }
  }

  public static long countWithThreads(int count) throws InterruptedException {
    if (count < 0 || count > 100) throw new IllegalArgumentException("0..100 threads");
    Counter counter = new Counter();
    List<Thread> threads = new ArrayList<>();
    Runnable action =
        () -> {
          for (int i = 0; i < 100; i++) counter.increment();
        };
    for (int i = 0; i < count; i++) {
      var thread = new Thread(action);
      threads.add(thread);
      thread.start();
    }
    for (Thread thread : threads) thread.join();
    return counter.value();
  }

  public static int sumAsync(List<Integer> values) throws Exception {
    ExecutorService executor = Executors.newFixedThreadPool(2);
    try {
      var first =
          CompletableFuture.supplyAsync(
              () -> values.subList(0, values.size() / 2).stream().reduce(0, Math::addExact),
              executor);
      var second =
          CompletableFuture.supplyAsync(
              () ->
                  values.subList(values.size() / 2, values.size()).stream()
                      .reduce(0, Math::addExact),
              executor);
      return first.thenCombine(second, Math::addExact).get(5, TimeUnit.SECONDS);
    } catch (ExecutionException e) {
      throw new IllegalStateException("Parallel calculation failed", e.getCause());
    } finally {
      executor.shutdownNow();
    }
  }
}
