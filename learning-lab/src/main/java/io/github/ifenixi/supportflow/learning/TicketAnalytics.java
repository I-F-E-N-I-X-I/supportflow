package io.github.ifenixi.supportflow.learning;
import java.util.*;
import java.util.stream.Collectors;
public final class TicketAnalytics {
 public record Sample(long id,String category,int minutes,List<String> tags) implements Comparable<Sample> {
  public Sample{Objects.requireNonNull(category);if(minutes<0)throw new IllegalArgumentException("Negative duration");tags=List.copyOf(tags);}
  @Override public int compareTo(Sample other){return Long.compare(id,other.id);}
 }
 public record Summary(long count,int totalMinutes,Map<String,Long> byCategory,List<String> tags,List<Long> slowestIds,boolean anySlow,boolean allValid,boolean noneEmpty,Optional<Sample> first){}
 public static Summary summarize(List<Sample> samples){
  return new Summary(samples.stream().count(),samples.stream().map(Sample::minutes).reduce(0,Math::addExact),
   samples.stream().collect(Collectors.groupingBy(Sample::category,TreeMap::new,Collectors.counting())),
   samples.stream().flatMap(s->s.tags().stream()).distinct().sorted().toList(),
   samples.stream().filter(s->s.minutes()>0).sorted(Comparator.comparingInt(Sample::minutes).reversed().thenComparingLong(Sample::id)).limit(3).map(Sample::id).toList(),
   samples.stream().anyMatch(s->s.minutes()>60),samples.stream().allMatch(s->s.id()>0),samples.stream().noneMatch(s->s.category().isBlank()),samples.stream().sorted().findFirst());
 }
 public static List<String> deduplicatedHistory(List<String> changes){
  LinkedList<String> history=new LinkedList<>(changes);Set<String> seen=new TreeSet<>();Iterator<String> iterator=history.iterator();while(iterator.hasNext())if(!seen.add(iterator.next()))iterator.remove();return List.copyOf(history);
 }
 public static List<String> undoOrder(List<String> changes){Deque<String> stack=new ArrayDeque<>();changes.forEach(stack::push);List<String> result=new ArrayList<>();while(!stack.isEmpty())result.add(stack.pop());return result;}
}
