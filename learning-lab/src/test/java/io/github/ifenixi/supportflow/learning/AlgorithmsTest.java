package io.github.ifenixi.supportflow.learning;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;
class AlgorithmsTest {
 @ParameterizedTest @ValueSource(ints={0,1,2,10,100}) void mergeSortMatchesJdk(int length){int[] input=new Random(length).ints(length,-100,100).toArray();int[] expected=input.clone();Arrays.sort(expected);assertArrayEquals(expected,Algorithms.mergeSort(input));}
 @ParameterizedTest @ValueSource(ints={-10,0,5,9,20}) void searchesMatchReference(int value){int[] values={-10,0,5,9,20};assertEquals(Algorithms.linearSearch(values,value),Algorithms.binarySearch(values,value));}
 @Test void searchMissing(){assertEquals(-1,Algorithms.binarySearch(new int[]{1,2,3},4));assertEquals(-1,Algorithms.linearSearch(new int[0],1));}
 @Test void twoPointersHandleOverflow(){assertTrue(Algorithms.twoSumSorted(new int[]{Integer.MAX_VALUE,Integer.MAX_VALUE},4294967294L));assertFalse(Algorithms.twoSumSorted(new int[]{1},2));}
 @Test void window(){assertEquals(9,Algorithms.maxWindowSum(new int[]{1,2,3,4,5},2));assertEquals(-3,Algorithms.maxWindowSum(new int[]{-3,-2,-1},2));}
 @Test void invalidWindow(){assertThrows(IllegalArgumentException.class,()->Algorithms.maxWindowSum(new int[]{1},2));}
 @Test void traversalHandlesCycles(){var graph=Map.of(1,List.of(2,3),2,List.of(1,4),3,List.of(4),4,List.of(1));assertEquals(List.of(1,2,3,4),Algorithms.bfs(graph,1));assertEquals(List.of(1,2,4,3),Algorithms.dfs(graph,1));}
 @Test void topK(){assertEquals(List.of(9,8,8),Algorithms.topK(new int[]{1,8,4,9,8},3));assertTrue(Algorithms.topK(new int[]{1},0).isEmpty());}
 @Test void frequencies(){assertEquals(Map.of(1,2,2,1),Algorithms.frequencies(new int[]{1,2,1}));}
 @ParameterizedTest @ValueSource(longs={0,1,100,Long.MAX_VALUE}) void digits(long value){assertEquals(Long.toString(value),Algorithms.digits(value));}
}
