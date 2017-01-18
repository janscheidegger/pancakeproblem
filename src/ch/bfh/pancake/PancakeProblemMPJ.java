package ch.bfh.pancake;

import mpi.MPI;
import mpi.Request;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Jan on 16.01.2017.
 */
public class PancakeProblemMPJ {

    static Request cancelRequest;
    static Request getWorkRequest;
    static int prev;
    static int next;
    static PancakeNode[] work = new PancakeNode[1];

//    static String mode = "first";
    static String mode = "count";


    private static int gapHeuristic(int[] pancakes) {
        int h = 0;
        for (int i = 0; i < pancakes.length - 1; i++) {
            if (Math.abs(pancakes[i] - pancakes[i + 1]) > 1) {
                h++;
            }
            if (pancakes[pancakes.length - 1] != pancakes.length) h++;
        }

        return h;
    }

    public static void main(String... args) {

        MPI.Init(args);


        int[] result = new int[1];
        boolean[] cancelPayload = new boolean[1];
        boolean[] getWorkPayload = new boolean[1];


        ArrayDeque<PancakeNode> pancakeStack = new ArrayDeque<>();

        int count = 0;

        int me = MPI.COMM_WORLD.Rank();
        int size = MPI.COMM_WORLD.Size();
        prev = me == 0 ? size - 1 : me - 1;
        next = me == size - 1 ? 0 : me + 1;
        cancelRequest = MPI.COMM_WORLD.Irecv(cancelPayload, 0, 1, MPI.BOOLEAN, prev, 88);
        getWorkRequest = MPI.COMM_WORLD.Irecv(getWorkPayload, 0, 1, MPI.BOOLEAN, prev, 66);

        long start = System.nanoTime();


        int[] pancakes = {2, 1, 4, 3, 6, 5, 8, 7, 10, 9, 12, 11, 14, 13, 16, 15, 17};
        int bound = gapHeuristic(pancakes);

        PancakeNode[] buffer = new PancakeNode[1];

        if (me != 0) {
            do {
                System.out.println(MPI.COMM_WORLD.Rank() + " waiting ...");
                MPI.COMM_WORLD.Recv(buffer, 0, 1, MPI.OBJECT, 0, 99);
                if (buffer[0].pancakes != null) {
                    pancakeStack.addFirst(buffer[0]);
                }
                System.out.println("put on stack  " + MPI.COMM_WORLD.Rank());
            } while (buffer[0].pancakes != null);
        }

        if (me == 0) {
            PancakeNode firstPancake = new PancakeNode(pancakes, 0, 0, bound, new int[pancakes.length * 2]);
            List<PancakeNode> pancakeList = new ArrayList<>();
            for (int i = 2; i <= firstPancake.pancakes.length; i++) {
                if (i == firstPancake.lastcut) continue;
                int[] newPancakes = flipAt(firstPancake.pancakes, i);
                int h2 = gapHeuristic(newPancakes);
                pancakeList.add(new PancakeNode(newPancakes, 1, i, h2, firstPancake.cuts));
            }
            for (int i = 0; i < pancakeList.size(); i++) {
                buffer[0] = pancakeList.get(i);
                if ((i % size) == 0) {
                    pancakeStack.addFirst(buffer[0]);
                } else {
                    MPI.COMM_WORLD.Isend(buffer, 0, 1, MPI.OBJECT, (i % size), 99);
                }
            }
            for (int i = 1; i < size; i++) {
                MPI.COMM_WORLD.Isend(new PancakeNode[]{new PancakeNode(null, 0, 0, 0)}, 0, 1, MPI.OBJECT, (i % size - 1) + 1, 99);
            }
        }

        do {
            System.out.println("bound is now " + bound);
            if ("count".equals(mode)) {
                count = countSolutions(pancakeStack.clone(), bound);
            } else {
                count = findBestSolution(pancakeStack.clone(), bound);
            }
            // Next Iteration not allowed if one has result. => Wait for each other
            //System.out.println(MPI.COMM_WORLD.Rank() + ": I'm DONE");
            MPI.COMM_WORLD.Barrier();
            bound++;
        } while (count <= 0);


        long end = System.nanoTime();
        System.out.println((end - start) / 1E9);
        System.out.println(count);
        MPI.COMM_WORLD.Allreduce(new int[]{count}, 0, result, 0, 1, MPI.INT, MPI.SUM);
        System.out.println(Arrays.toString(result));
        MPI.Finalize();
    }

    private static int findBestSolution(ArrayDeque<PancakeNode> pancakeStack, int bound) {
        int count = 0;
        getWorkRequest.Cancel();
        //System.out.println(MPI.COMM_WORLD.Rank() + ": I'm back at work");
        PancakeNode currentPancakes;
        while (true) {

            if (pancakeStack.isEmpty()) {
                getWork(pancakeStack);
                if (work[0] == null) {
                    //System.out.println(MPI.COMM_WORLD.Rank() + ": Got Signal to leave");
                    MPI.COMM_WORLD.Isend(new PancakeNode[]{null}, 0, 1, MPI.OBJECT, prev, 77);
                    break;
                } else {
                    //System.out.println(MPI.COMM_WORLD.Rank() + ": I'm back at work");
                    continue;
                }
            }

            if (getWorkRequest.Test() != null) {
                sendWork(pancakeStack);
            }

            if (cancelRequest.Test() != null) {
                System.out.println("This is thend");
                MPI.COMM_WORLD.Send(new boolean[]{true}, 0, 1, MPI.BOOLEAN, next, 88);
                return 1;
            }

            currentPancakes = pancakeStack.removeFirst();
            int f = currentPancakes.depth + currentPancakes.heuristic;
            if (f > bound) continue;
            if (currentPancakes.isSorted()) {
                count++;
                System.out.println(Arrays.toString(currentPancakes.cuts));
                MPI.COMM_WORLD.Send(new boolean[]{true}, 0, 1, MPI.BOOLEAN, next, 88);
                return 1;
//                continue;
            }
            int nextDepth = currentPancakes.depth + 1;
            for (int i = 2; i <= currentPancakes.pancakes.length; i++) {
                if (i == currentPancakes.lastcut) continue;
                int[] newPancakes = flipAt(currentPancakes.pancakes, i);
                int h2 = gapHeuristic(newPancakes);
                pancakeStack.addFirst(new PancakeNode(newPancakes, nextDepth, i, h2, currentPancakes.cuts));
            }

        }
        return count;


    }

    private static int countSolutions(ArrayDeque<PancakeNode> pancakeStack, int bound) {
        int count = 0;
        getWorkRequest.Cancel();
        //System.out.println(MPI.COMM_WORLD.Rank() + ": I'm back at work");
        PancakeNode currentPancakes;
        while (true) {

            if (pancakeStack.isEmpty()) {
                getWork(pancakeStack);
                if (work[0] == null) {
                    //System.out.println(MPI.COMM_WORLD.Rank() + ": Got Signal to leave");
                    MPI.COMM_WORLD.Isend(new PancakeNode[]{null}, 0, 1, MPI.OBJECT, prev, 77);
                    break;
                } else {
                    //System.out.println(MPI.COMM_WORLD.Rank() + ": I'm back at work");
                    continue;
                }
            }

            if (getWorkRequest.Test() != null) {
                sendWork(pancakeStack);
            }

//            if(cancelRequest.Test() != null) {
//                System.out.println("This is thend");
//                MPI.COMM_WORLD.Send(new boolean[]{true}, 0, 1, MPI.BOOLEAN, next, 88);
//                return 1;
//            }

            currentPancakes = pancakeStack.removeFirst();
            int f = currentPancakes.depth + currentPancakes.heuristic;
            if (f > bound) continue;
            if (currentPancakes.isSorted()) {
                count++;
//                MPI.COMM_WORLD.Send(new boolean[]{true}, 0, 1, MPI.BOOLEAN, next, 88);
//                return 1;
                continue;
            }
            int nextDepth = currentPancakes.depth + 1;
            for (int i = 2; i <= currentPancakes.pancakes.length; i++) {
                if (i == currentPancakes.lastcut) continue;
                int[] newPancakes = flipAt(currentPancakes.pancakes, i);
                int h2 = gapHeuristic(newPancakes);
                pancakeStack.addFirst(new PancakeNode(newPancakes, nextDepth, i, h2));
            }

        }
        return count;
    }

    private static void getWork(ArrayDeque<PancakeNode> pancakeNodes) {
        //System.out.println(MPI.COMM_WORLD.Rank() + ": Hey! " + next + " I need Work!");
        MPI.COMM_WORLD.Isend(new boolean[]{true}, 0, 1, MPI.BOOLEAN, next, 66);
        MPI.COMM_WORLD.Recv(work, 0, 1, MPI.OBJECT, next, 77);
        if (work[0] != null) {
            pancakeNodes.addFirst(work[0]);
        }
    }

    private static void sendWork(ArrayDeque<PancakeNode> pancakeNodes) {
        if (pancakeNodes.size() > 1) {
            //System.out.println(MPI.COMM_WORLD.Rank() + ": I give you Work! TO: " + prev);
            MPI.COMM_WORLD.Send(new PancakeNode[]{pancakeNodes.removeLast()}, 0, 1, MPI.OBJECT, prev, 77);

        } else {
            //System.out.println(MPI.COMM_WORLD.Rank() + ": Sorry, no work here! TO: " + prev);
            MPI.COMM_WORLD.Isend(new PancakeNode[]{null}, 0, 1, MPI.OBJECT, prev, 77);
        }
    }


    private static int[] flipAt(int[] pancakes, int position) {
        int[] temp = Arrays.copyOf(pancakes, pancakes.length);
        for (int i = 0; i < position; i++) {
            temp[i] = pancakes[position - i - 1];
        }
        return temp;
    }

    private static void printSolution(int[] pancakes, int[] flips) {
        int i = 0;
        do {
            System.out.print("State " + i + ": ");
            for (int j = 0; j < flips[i]; j++) {
                System.out.print(pancakes[j] + ", ");
            }
            System.out.print("| ");
            for (int j = flips[i]; j < pancakes.length; j++) {
                System.out.print(pancakes[j] + ", ");
            }
            System.out.println();
            pancakes = flipAt(pancakes, flips[++i]);
        } while (flips[i] != 0);
    }

}
