package io.github.ifenixi.supportflow.learning;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CoreBasicsTest {
  @Test
  void primitiveValuesAndWidening() {
    var example = CoreBasics.example();
    assertEquals(312, example.count());
    assertEquals(2147483648L, example.large());
    assertTrue(example.active());
    assertNotEquals((double) example.approximate(), example.precise());
  }

  @Test
  void narrowingRejectsOverflow() {
    assertEquals(42, CoreBasics.checkedNarrowing(42L));
    assertThrows(ArithmeticException.class, () -> CoreBasics.checkedNarrowing(Long.MAX_VALUE));
  }

  @Test
  void defensiveArrayCopy() {
    int[] original = {1, 2, 3};
    int[] copy = CoreBasics.doubledCopy(original);
    assertNotSame(original, copy);
    assertArrayEquals(new int[] {2, 4, 6}, copy);
    assertArrayEquals(new int[] {1, 2, 3}, original);
  }
}
