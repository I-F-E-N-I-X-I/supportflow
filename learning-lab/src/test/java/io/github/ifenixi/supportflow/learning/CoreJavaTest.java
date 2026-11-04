package io.github.ifenixi.supportflow.learning;

import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class CoreJavaTest {
  @Test
  void equalKeysResolveSameHashEntry() {
    var a = new Repositories.TicketKey(1);
    var b = new Repositories.TicketKey(1);
    assertNotSame(a, b);
    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
    var repo = new Repositories.Tickets();
    repo.save(new Repositories.Item(a, "Ticket"));
    assertTrue(repo.find(b).isPresent());
    assertTrue(repo.delete(b));
    assertTrue(repo.find(a).isEmpty());
  }

  @Test
  void overloadedSaveAndValidation() {
    var repo = new Repositories.Tickets();
    repo.save(
        List.of(
            new Repositories.Item(new Repositories.TicketKey(1), "One"),
            new Repositories.Item(new Repositories.TicketKey(2), "Two")));
    assertTrue(repo.find(new Repositories.TicketKey(2)).isPresent());
    assertThrows(IllegalArgumentException.class, () -> repo.save(new Repositories.Item(null, "")));
  }

  @Test
  void stringsUseEquals() {
    String a = new String("ticket"), b = new String("ticket");
    assertFalse(a == b);
    assertTrue(a.equals(b));
  }

  @Test
  void analyticsUsesStreams() {
    var samples =
        List.of(
            new TicketAnalytics.Sample(2, "billing", 90, List.of("b", "a")),
            new TicketAnalytics.Sample(1, "technical", 10, List.of("a")));
    var summary = TicketAnalytics.summarize(samples);
    assertEquals(2, summary.count());
    assertEquals(100, summary.totalMinutes());
    assertEquals(List.of("a", "b"), summary.tags());
    assertEquals(List.of(2L, 1L), summary.slowestIds());
    assertTrue(summary.anySlow());
    assertTrue(summary.allValid());
    assertTrue(summary.noneEmpty());
    assertEquals(1, summary.first().orElseThrow().id());
    assertEquals(1, summary.byCategory().get("billing"));
  }

  @Test
  void emptyAnalytics() {
    var summary = TicketAnalytics.summarize(List.of());
    assertEquals(0, summary.count());
    assertTrue(summary.first().isEmpty());
    assertFalse(summary.anySlow());
  }

  @Test
  void listIteratorAndStack() {
    assertEquals(
        List.of("NEW", "OPEN"), TicketAnalytics.deduplicatedHistory(List.of("NEW", "OPEN", "NEW")));
    assertEquals(List.of("C", "B", "A"), TicketAnalytics.undoOrder(List.of("A", "B", "C")));
  }

  @Test
  void strategyAndFactory() {
    assertEquals(Duration.ofHours(4), Policies.forTier(Policies.Tier.PREMIUM).target());
    assertEquals(Duration.ofHours(24), Policies.forTier(Policies.Tier.STANDARD).target());
  }

  @Test
  void builder() {
    assertEquals(
        5, Policies.Query.builder().category("billing").limit(5).openOnly(true).build().limit());
    assertThrows(IllegalArgumentException.class, () -> Policies.Query.builder().limit(0).build());
  }

  @Test
  void synchronizedCounter() throws Exception {
    assertEquals(800, ConcurrencyLab.countWithThreads(8));
  }

  @Test
  void executorAndCompletableFuture() throws Exception {
    assertEquals(10, ConcurrencyLab.sumAsync(List.of(1, 2, 3, 4)));
    assertEquals(0, ConcurrencyLab.sumAsync(List.of()));
  }

  @Test
  void volatileStopSignal() throws Exception {
    var signal = new ConcurrencyLab.StopSignal();
    var thread = new Thread(signal::stop);
    thread.start();
    thread.join();
    assertTrue(signal.isStopped());
  }

  @Test
  void ttlAndInvalidation() {
    var clock = new MutableClock();
    var cache = new ConcurrencyLab.TtlCache<String, String>(clock);
    cache.put("k", "v", Duration.ofSeconds(2));
    assertEquals("v", cache.get("k").orElseThrow());
    clock.now = clock.now.plusSeconds(2);
    assertTrue(cache.get("k").isEmpty());
    cache.put("k", "v2", Duration.ofSeconds(2));
    cache.invalidate("k");
    assertTrue(cache.get("k").isEmpty());
  }

  static class MutableClock extends Clock {
    Instant now = Instant.EPOCH;

    public java.time.ZoneId getZone() {
      return java.time.ZoneOffset.UTC;
    }

    public Clock withZone(java.time.ZoneId zone) {
      return this;
    }

    public Instant instant() {
      return now;
    }
  }
}
