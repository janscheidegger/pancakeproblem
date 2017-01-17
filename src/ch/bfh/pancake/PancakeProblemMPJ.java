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
    static int before;
    static int next;


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


        ArrayDeque<PancakeNode> pancakeStack = new ArrayDeque<>();

        int count = 0;

        int me = MPI.COMM_WORLD.Rank();
        int size = MPI.COMM_WORLD.Size();
        before = me == 0 ? size-1 : me -1;
        next = me == size-1 ? 0 : me + 1;
        cancelRequest = MPI.COMM_WORLD.Irecv(cancelPayload, 0,1, MPI.BOOLEAN, before , 88);

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
            PancakeNode firstPancake = new PancakeNode(pancakes, 0, 0, bound);
            List<PancakeNode> pancakeList = new ArrayList<>();
            for (int i = 2; i <= firstPancake.pancakes.length; i++) {
                if (i == firstPancake.lastcut) continue;
                int[] newPancakes = flipAt(firstPancake.pancakes, i);
                int h2 = gapHeuristic(newPancakes);
                pancakeList.add(new PancakeNode(newPancakes, 1, i, h2));
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
            count = countSolutions(pancakeStack.clone(), bound);
            // Next Iteration not allowed if one has result. => Wait for each other
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

    private static int countSolutions(ArrayDeque<PancakeNode> pancakeStack, int bound) {
        int count = 0;

        PancakeNode currentPancakes;
        while (!pancakeStack.isEmpty()) {

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


    private static int[] flipAt(int[] pancakes, int position) {
        int[] temp = Arrays.copyOf(pancakes, pancakes.length);
        for (int i = 0; i < position; i++) {
            temp[i] = pancakes[position - i - 1];
        }
        return temp;
    }

}
