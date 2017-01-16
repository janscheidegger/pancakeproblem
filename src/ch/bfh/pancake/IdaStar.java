package ch.bfh.pancake;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

/**
 * Created by Jan on 04.01.2017.
 */
public class IdaStar {

    // Gap heuristic
    // Numbers of pancakes which are not of adjacent size to the pancake below
    private static int heuristic(int[] unsortedPancakes) {
        int h = 0;
        for (int i = 0; i < unsortedPancakes.length - 1; i++) {
            if (Math.abs(unsortedPancakes[i] - unsortedPancakes[i + 1]) > 1) {
                h++;
            }
        }
        return h;
    }

    private static int[] pancakesUnorderd = {2, 1, 4, 3, 6, 5, 8, 7, 10, 9, 12, 11, 14, 13, 15};

    public static void main(String... args) {
        idaStar(new PancakeNode(pancakesUnorderd, null));

    }

    public static void idaStar(PancakeNode initialState) {
        int initialCost = heuristic(initialState.pancake);
        int cost;
        boolean solutionFound = false;
        Deque<PancakeNode> stack = new ArrayDeque<>();
        stack.addFirst(initialState);
        while (!solutionFound) {
            cost = initialCost;
            initialCost++;
            int depth = 0;
            while (depth >= 0) {
                PancakeNode nextChild = stack.removeFirst();
                if(depth + heuristic(nextChild.pancake) <= cost) {
                    depth++;
                    for (int i = 2; i <= nextChild.pancake.length; i++) {
                        int[] newPancakes = flipAt(nextChild.pancake, i);
                        if (isSorted(nextChild.pancake)) solutionFound = true;
                        else stack.addFirst(new PancakeNode(newPancakes, nextChild));
                    }
                    continue;
                }
                depth--;
            }
        }
        System.out.println("found solution");
    }

    private static class PancakeNode {
        int[] pancake;
        PancakeNode cameFrom;

        PancakeNode(int[] pancake, PancakeNode cameFrom) {
            this.pancake = pancake;
            this.cameFrom = cameFrom;
        }
    }

    private static int[] flipAt(int[] pancakes, int position) {
        int[] temp = Arrays.copyOf(pancakes, pancakes.length);
        for (int i = 0; i < position; i++) {
            temp[i] = pancakes[position - i - 1];
        }
        return temp;
    }

    private static boolean isSorted(int[] pancakes) {
        int previous = pancakes[0];
        for (int i = 1; i < pancakes.length; i++) {
            if (previous > pancakes[i]) {
                return false;
            }
            previous = pancakes[i];
        }
        return true;
    }
}
