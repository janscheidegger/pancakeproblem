package ch.bfh.pancake;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by Jan on 29.11.2016.
 */
public class Sequential {


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

    //    private static int[] pancakesUnorderd = ThreadLocalRandom.current().ints(1,30).distinct().limit(29).toArray();
//    private static int[] pancakesUnorderd = {2, 1, 4, 3, 6, 5, 8, 7, 10, 9, 12, 11, 14, 13, 15};
    private static int[] pancakesUnorderd;


    public static void main(String... args) {
        if (args.length != 2) {
            System.out.println("Please start the program with valid paramters");
            System.out.println("java Sequential [first/all] [random/changed]");
        } else {
            if ("random".equalsIgnoreCase(args[1])) {
                pancakesUnorderd = ThreadLocalRandom.current().ints(1, 30).distinct().limit(29).toArray();
            } else if ("changed".equalsIgnoreCase(args[1])) {
                pancakesUnorderd = new int[]{2, 1, 4, 3, 6, 5, 8, 7, 10, 9, 12, 11, 14, 13, 15 };
            } else {
                System.out.println("Please start the program with valid paramters");
                System.out.println("java Sequential [first/all] [random/changed]");
            }

            if ("first".equalsIgnoreCase(args[0])) {
                idaStarFirst(pancakesUnorderd);
            } else if ("all".equalsIgnoreCase(args[0])) {
                idaStarCount(pancakesUnorderd);
            } else {
                System.out.println("Please start the program with valid paramters");
                System.out.println("java Sequential [first/all] [random/changed]");
            }
        }
    }

    private static void idaStarCount(int[] unsortedPancakes) {
        long start = System.nanoTime();
        int bound = heuristic(unsortedPancakes);
        System.out.println("SEARCH BOUND IS " + bound);
        System.out.println(Arrays.toString(unsortedPancakes));
        int count;
        while ((count = count(unsortedPancakes, bound)) < 1) {
            bound++;
            System.out.println("SEARCH BOUND IS NOW NEW " + bound);
        }
        System.out.println(count);
        System.out.println((System.nanoTime() - start) / 1E9);
    }

    private static void idaStarFirst(int[] unsortedPancakes) {
        long start = System.nanoTime();
        int bound = heuristic(unsortedPancakes);
        System.out.println("SEARCH BOUND IS " + bound);
        System.out.println(Arrays.toString(unsortedPancakes));
        while (!search(unsortedPancakes, bound)) {
            bound++;
            System.out.println("SEARCH BOUND IS NOW NEW " + bound);
        }
        System.out.println((System.nanoTime() - start) / 1E9);
    }

    private static int count(int[] pancakes, int bound) {
        Deque<int[]> pancakeStack = new ArrayDeque<>();
        Deque<Integer> depthStack = new ArrayDeque<>();
        pancakeStack.addFirst(pancakes);
        depthStack.addFirst(0);

        int count = 0;
        int[] currentPancakes;
        Integer depth;
        while (!depthStack.isEmpty() && !pancakeStack.isEmpty()) {
            currentPancakes = pancakeStack.removeFirst();
            depth = depthStack.removeFirst();
            int f = depth++ + heuristic(currentPancakes);
            if (f > bound) continue;
            if (isSorted(currentPancakes)) {
                count++;
            }

            for (int i = 2; i <= currentPancakes.length; i++) {
                pancakeStack.addFirst(flipAt(currentPancakes, i));
                depthStack.addFirst(depth);
            }
        }
        return count;
    }


    private static boolean search(int[] pancakes, int bound) {
        Deque<PancakeNode> pancakeStack = new ArrayDeque<>();
        Deque<Integer> depthStack = new ArrayDeque<>();
        pancakeStack.addFirst(new PancakeNode(pancakes, null));
        depthStack.addFirst(0);

        PancakeNode currentPancakes;
        Integer depth;
        while (!depthStack.isEmpty() && !pancakeStack.isEmpty()) {
            currentPancakes = pancakeStack.removeFirst();
            depth = depthStack.removeFirst();
            int f = depth++ + heuristic(currentPancakes.pancake);
            if (f > bound) {
                continue;
            }
            if (isSorted(currentPancakes.pancake)) {
                System.out.println(Arrays.toString(currentPancakes.pancake) + " is a solution!");
                printPath(currentPancakes);
                return true;
            }

            for (int i = 2; i <= currentPancakes.pancake.length; i++) {
                pancakeStack.addFirst(new PancakeNode(flipAt(currentPancakes.pancake, i), currentPancakes));
                depthStack.addFirst(depth);
            }
        }
        return false;
    }

    private static void printPath(PancakeNode currentPancakes) {
        Deque<int[]> path = new ArrayDeque<>();
        path.addFirst(currentPancakes.pancake);
        while((currentPancakes = currentPancakes.cameFrom) != null) {
            path.addFirst(currentPancakes.pancake);
        }
        int i = 0;
        while (!path.isEmpty()) {
            System.out.println("State " + i++ + ":" + Arrays.toString(path.removeFirst()));
        }
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
