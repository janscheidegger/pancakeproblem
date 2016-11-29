package ch.bfh.pancake;

import java.util.Arrays;

/**
 * Created by Jan on 29.11.2016.
 */
public class Main {


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

    private static int[] pancakesUnorderd = new int[]{4, 8, 7, 5,3,2,1};


    public static void main(String... args) {
        idaStar(pancakesUnorderd);
    }

    private static int[] idaStar(int[] unsortedPancakes) {
        int bound = heuristic(unsortedPancakes);
        System.out.println("SEARCH BOUND IS "+bound);
        search(unsortedPancakes, 0, ++bound);
        return new int[]{};
    }


    private static void search(int[] pancakes, int g, int bound) {
        //System.out.println(g);
        int f = g + heuristic(pancakes);
        if (f > bound) return;
        if(isSorted(pancakes)) {
            return;
        }
        for (int i = 2; i <= pancakes.length; i++) {
            //System.out.println("Flip at " + i);
            search(flipAt(pancakes, i), g + 1, bound);
        }
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
