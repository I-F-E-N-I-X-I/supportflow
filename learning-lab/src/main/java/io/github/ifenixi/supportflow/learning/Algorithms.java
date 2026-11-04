package io.github.ifenixi.supportflow.learning;
import java.util.*;
/** Independent training exercises; these are not production ticket-storage algorithms. */
public final class Algorithms {
 private Algorithms(){}
 public static int linearSearch(int[] values,int target){for(int i=0;i<values.length;i++)if(values[i]==target)return i;return -1;}
 public static int binarySearch(int[] sorted,int target){int left=0,right=sorted.length-1;while(left<=right){int middle=left+(right-left)/2;if(sorted[middle]==target)return middle;if(sorted[middle]<target)left=middle+1;else right=middle-1;}return -1;}
 public static int[] mergeSort(int[] values){
  if(values.length<2)return values.clone();int middle=values.length/2;
  int[] left=mergeSort(Arrays.copyOfRange(values,0,middle)),right=mergeSort(Arrays.copyOfRange(values,middle,values.length)),result=new int[values.length];
  int a=0,b=0,i=0;while(a<left.length&&b<right.length)result[i++]=left[a]<=right[b]?left[a++]:right[b++];
  while(a<left.length)result[i++]=left[a++];while(b<right.length)result[i++]=right[b++];return result;
 }
 public static boolean twoSumSorted(int[] sorted,long target){int left=0,right=sorted.length-1;while(left<right){long sum=(long)sorted[left]+sorted[right];if(sum==target)return true;if(sum<target)left++;else right--;}return false;}
 public static long maxWindowSum(int[] values,int size){
  if(size<1||size>values.length)throw new IllegalArgumentException("Window must fit input");long sum=0;for(int i=0;i<size;i++)sum+=values[i];long best=sum;
  for(int i=size;i<values.length;i++){sum+=(long)values[i]-values[i-size];best=Math.max(best,sum);}return best;
 }
 public static List<Integer> bfs(Map<Integer,List<Integer>> graph,int root){
  Queue<Integer> queue=new ArrayDeque<>();Set<Integer> seen=new HashSet<>();List<Integer> order=new ArrayList<>();queue.add(root);seen.add(root);
  while(!queue.isEmpty()){int node=queue.remove();order.add(node);for(int child:graph.getOrDefault(node,List.of()))if(seen.add(child))queue.add(child);}return order;
 }
 public static List<Integer> dfs(Map<Integer,List<Integer>> graph,int root){var seen=new HashSet<Integer>();var order=new ArrayList<Integer>();visit(graph,root,seen,order);return order;}
 private static void visit(Map<Integer,List<Integer>> graph,int node,Set<Integer> seen,List<Integer> order){if(!seen.add(node))return;order.add(node);for(int child:graph.getOrDefault(node,List.of()))visit(graph,child,seen,order);}
 public static List<Integer> topK(int[] values,int k){if(k<0)throw new IllegalArgumentException("k >= 0");var heap=new PriorityQueue<Integer>();for(int value:values){heap.add(value);if(heap.size()>k)heap.remove();}return heap.stream().sorted(Comparator.reverseOrder()).toList();}
 public static Map<Integer,Integer> frequencies(int[] values){Map<Integer,Integer> result=new HashMap<>();for(int value:values)result.merge(value,1,Integer::sum);return result;}
 public static String digits(long value){if(value<0)throw new IllegalArgumentException("Nonnegative value required");StringBuilder result=new StringBuilder();do{result.append((char)('0'+value%10));value/=10;}while(value>0);return result.reverse().toString();}
}
