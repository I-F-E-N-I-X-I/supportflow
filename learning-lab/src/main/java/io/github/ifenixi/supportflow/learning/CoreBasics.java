package io.github.ifenixi.supportflow.learning;

/** Small executable examples for primitive/reference semantics, outside the production API. */
public final class CoreBasics {
  private CoreBasics() {}

  public record Primitives(
      byte small,
      short medium,
      int count,
      long large,
      float approximate,
      double precise,
      char letter,
      boolean active) {}

  public static Primitives example() {
    byte small = 12;
    short medium = 300;
    int count = small + medium;
    long large = (long) Integer.MAX_VALUE + 1;
    float approximate = 0.1f;
    double precise = 0.1d;
    char letter = 'J';
    boolean active = count > 0 && letter == 'J';
    return new Primitives(small, medium, count, large, approximate, precise, letter, active);
  }

  public static int checkedNarrowing(long value) {
    return Math.toIntExact(value);
  }

  public static int[] doubledCopy(int[] values) {
    int[] copy = values.clone();
    for (int i = 0; i < copy.length; i++) copy[i] = Math.multiplyExact(copy[i], 2);
    return copy;
  }
}
