package ch.bfh.pancake;

import mpi.MPI;
import mpi.Request;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

/**
 * Created by Jan on 02.01.2017.
 */
public class MPJVersion {

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
        MPI.Init(args);
        pancakesUnorderd = new int[]{2, 1, 4, 3, 6, 5, 8, 7, 10, 9, 12, 11, 14, 13, 15};
        idaStarFirst(pancakesUnorderd);
//        idaStarCount(pancakesUnorderd);
        MPI.Finalize();
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


        int me = MPI.COMM_WORLD.Rank();
        int size = MPI.COMM_WORLD.Size();
        int depth;
        int bound = 13;//heuristic(unsortedPancakes);

        PancakeNode[] inputBuffer = new PancakeNode[1];

        if (me == 0) {
            long start = System.nanoTime();

            Deque<PancakeNode> pancakeStack = new ArrayDeque<>();
            pancakeStack.addFirst(new PancakeNode(unsortedPancakes, null, 0));


            System.out.println("SEARCH BOUND IS " + bound);
            System.out.println(Arrays.toString(unsortedPancakes));
            Request[] requests = new Request[size];

            for (int i = 1; i < size; i++) {
                requests[i] = MPI.COMM_WORLD.Irecv(inputBuffer, 0, 1, MPI.OBJECT, i, 1);
            }
            inputBuffer[0] = pancakeStack.removeFirst();
            MPI.COMM_WORLD.Isend(inputBuffer, 0, 1, MPI.OBJECT, 1, 1);

            boolean run = true;

            while (run) {
                for (int i = 1; i < size; i++) {
                    if (requests[i].Test() != null) {
                        if (inputBuffer[0] == null) {
                            System.out.println("FERTIG");
                            run = false;
                            break;
                        }
                        pancakeStack.addFirst(inputBuffer[0]);
//                        System.out.println("got an answer from Worker");
                        requests[i] = MPI.COMM_WORLD.Irecv(inputBuffer, 0, 1, MPI.OBJECT, i, 1);
                        inputBuffer[0] = pancakeStack.removeFirst();
//                        System.out.println("Sent new Task");
//                        System.out.println(pancakeStack.size());
//                        System.out.println(Arrays.toString(inputBuffer[0].pancake));
                        MPI.COMM_WORLD.Isend(inputBuffer, 0, 1, MPI.OBJECT, i , 1);

                    }
                }
            }
            System.out.println((System.nanoTime() - start) / 1E9);

        } else {
            boolean run = true;
            while (run) {
//                System.out.println("Listening for Tasks...");
                MPI.COMM_WORLD.Recv(inputBuffer, 0, 1, MPI.OBJECT, 0, 1);
//                System.out.println("Got Task");

                if(inputBuffer[0] == null) {
                    run = false;
                    continue;
                }
                System.out.println("rank: "+ me + " -> "+Arrays.toString(inputBuffer[0].pancake));

                depth = inputBuffer[0].depth;
                int heuristic = heuristic(inputBuffer[0].pancake);
                int f = depth++ + heuristic;
                System.out.println("heuristics is: "+heuristic);
                if (f <= bound) {
                    if (isSorted(inputBuffer[0].pancake)) {
                        System.out.println(Arrays.toString(inputBuffer[0].pancake) + " is a solution!");
                        printPath(inputBuffer[0]);
                        MPI.COMM_WORLD.Isend(null, 0, 1, MPI.OBJECT, 0, 1);

                    }
                    for (int i = 2; i <= inputBuffer[0].pancake.length; i++) {
//                        System.out.println("Sent node to chef!");
                        inputBuffer[0] = new PancakeNode(flipAt(inputBuffer[0].pancake, i), inputBuffer[0], depth);
//                        System.out.println("At Depth: "+inputBuffer[0].depth);
                        MPI.COMM_WORLD.Isend(inputBuffer, 0, 1, MPI.OBJECT, 0, 1);
                    }
                }
            }
        }
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


    private static void printPath(PancakeNode currentPancakes) {
        Deque<int[]> path = new ArrayDeque<>();
        path.addFirst(currentPancakes.pancake);
        while ((currentPancakes = currentPancakes.cameFrom) != null) {
            path.addFirst(currentPancakes.pancake);
        }
        int i = 0;
        while (!path.isEmpty()) {
            System.out.println("State " + i++ + ":" + Arrays.toString(path.removeFirst()));
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

    private static class PancakeNode implements Serializable{
        int[] pancake;
        PancakeNode cameFrom;
        int depth;

        PancakeNode(int[] pancake, PancakeNode cameFrom, int depth) {
            this.pancake = pancake;
            this.cameFrom = cameFrom;
            this.depth = depth;
        }
    }
}
