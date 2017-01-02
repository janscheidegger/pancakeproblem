package ch.bfh.pancake;

import java.util.Arrays;
import java.util.Stack;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by Jan on 29.11.2016.
 */
public class Sequential {


    // Gap heuristic
    // Numbers of pancakes which are not of adjacent size to the pancake below
    private static int heuristic(int[] unsortedPancakes) {
        int h = 0;
        for(int i=0; i < unsortedPancakes.length -1; i++) {
            if(Math.abs(unsortedPancakes[i] - unsortedPancakes[i+1]) > 1) {
                h++;
            }
        }
        return h;
    }

//    private static int[] pancakesUnorderd = ThreadLocalRandom.current().ints(1,30).distinct().limit(29).toArray();
private static int[] pancakesUnorderd = {2, 1, 4, 3, 6, 5, 8, 7, 10, 9, 12, 11, 14, 13, 15 };


    public static void main(String... args) {
        idaStar(pancakesUnorderd);
    }

    private static void idaStar(int[] unsortedPancakes) {
        long start = System.nanoTime();
        int bound = heuristic(unsortedPancakes);
        System.out.println("SEARCH BOUND IS "+bound);
        System.out.println(Arrays.toString(unsortedPancakes));
        while(!search(unsortedPancakes, bound)) {
            bound++;
            System.out.println("SEARCH BOUND IS NOW NEW "+bound);
        }
        System.out.println((System.nanoTime() - start)/1E9);
    }


    private static boolean search(int[] pancakes, int bound) {

        Stack<int[]> pancakeStack = new Stack<>();
        Stack<Integer> depthStack = new Stack<>();
        pancakeStack.push(pancakes);
        depthStack.push(0);

        int[] currentPancakes;
        Integer depth;
        while(!depthStack.empty() && !pancakeStack.empty())  {
            currentPancakes = pancakeStack.pop();
            depth = depthStack.pop();
            int f = depth++ + heuristic(currentPancakes);
            if (f > bound) continue;
            if(isSorted(currentPancakes)) {
                System.out.println(Arrays.toString(currentPancakes) + " is a solution!");
                return true;
            }

            for (int i = 2; i <= currentPancakes.length; i++) {
                pancakeStack.push(flipAt(currentPancakes, i));
                depthStack.push(depth);
            }
        }
        return false;
    }

    private static int[] flipAt(int[] pancakes, int position) {
        int[] temp = Arrays.copyOf(pancakes, pancakes.length);
        for(int i = 0; i<position; i++) {
            temp[i] = pancakes[position - i - 1];
        }
        return temp;
    }

    private static boolean isSorted(int[] pancakes) {
        int previous = pancakes[0];
        for(int i = 1; i < pancakes.length; i++) {
            if(previous > pancakes[i]) {
                return false;
            }
            previous = pancakes[i];
        }
        System.out.println("!!!! FOUND!!!!" +Arrays.toString(pancakes));
        return true;
    }

}
