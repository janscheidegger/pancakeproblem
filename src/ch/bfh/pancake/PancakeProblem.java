package ch.bfh.pancake;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

/**
 * Created by Jan on 16.01.2017.
 */
public class PancakeProblem {

    private static int gapHeuristic(int[] pancakes) {
        int h = 0;
        for(int i = 0; i < pancakes.length - 1; i++) {
            if(Math.abs(pancakes[i] - pancakes[i+1]) > 1) {
                h++;
            }
            if (pancakes[pancakes.length-1] != pancakes.length) h++;
        }

        return h;
    }

    public static void main(String... args) {

        Options options = createOptions();

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("Pancake Sorter", options);

            System.exit(1);
            return;
        }

        String mode = cmd.getOptionValue("mode");
        String outputFilePath = cmd.getOptionValue("stackType");


        long start = System.nanoTime();
        int[] pancakes = {2, 1, 4, 3, 6, 5, 8, 7, 10, 9, 12, 11, 14, 13, 16, 15, 17};
        int bound = gapHeuristic(pancakes);
        int count;
        do {
            if("count".equalsIgnoreCase(mode)) {
                count = countSolutions(pancakes, bound);
            } else {
                pancakes = new int[]{2, 1, 4, 3, 6, 5, 8, 7, 10, 9, 12, 11, 14, 13, 15};
                count = findBestSolution(pancakes, bound);
            }
            bound++;
            System.out.println("bound is now "+bound);
        } while(count <= 0);
        long end = System.nanoTime();
        System.out.println((end - start)/1E9);
        System.out.println(count);
    }

    private static int findBestSolution(int[] pancakes, int bound) {
        Deque<PancakeNode> pancakeStack = new ArrayDeque<>();
        pancakeStack.addFirst(new PancakeNode(pancakes, 0, 0, bound, new int[pancakes.length]));
        int count = 0;

        PancakeNode currentPancakes;
        while (!pancakeStack.isEmpty()) {
            currentPancakes = pancakeStack.removeFirst();
            int f = currentPancakes.depth + currentPancakes.heuristic;
            if (f > bound) continue;
            if(currentPancakes.isSorted()) {
                count++;
                System.out.println(Arrays.toString(currentPancakes.cuts));
                printSolution(pancakes, currentPancakes.cuts);
                return count;
            }
            int nextDepth = currentPancakes.depth + 1;
            for(int i = 2;i <= currentPancakes.pancakes.length; i++) {
                if(i == currentPancakes.lastcut) continue;
                int [] newPancakes = flipAt(currentPancakes.pancakes, i);
                int h2 = gapHeuristic(newPancakes);
                pancakeStack.addFirst(new PancakeNode(newPancakes, nextDepth, i, h2, currentPancakes.cuts));
            }
        }
        return count;
    }


    private static Options createOptions() {
        Options options = new Options();

        Option mode = new Option("m", "mode", true, "first or all");
        mode.setRequired(true);
        options.addOption(mode);

        Option stack = new Option("s", "stackType", true, "random or pairwise");
        stack.setRequired(true);
        options.addOption(stack);
        return options;
    }

    private static int countSolutions(int [] pancakes, int bound) {
        Deque<PancakeNode> pancakeStack = new ArrayDeque<>();
        pancakeStack.addFirst(new PancakeNode(pancakes, 0, 0, bound));
        int count = 0;

        PancakeNode currentPancakes;
        while (!pancakeStack.isEmpty()) {
            currentPancakes = pancakeStack.removeFirst();
            int f = currentPancakes.depth + currentPancakes.heuristic;
            if (f > bound) continue;
            if(currentPancakes.isSorted()) {
                count++;
                continue;
                //return count;
            }
            int nextDepth = currentPancakes.depth + 1;
            for(int i = 2;i <= currentPancakes.pancakes.length; i++) {
                if(i == currentPancakes.lastcut) continue;
                int [] newPancakes = flipAt(currentPancakes.pancakes, i);
                int h2 = gapHeuristic(newPancakes);
                pancakeStack.addFirst(new PancakeNode(newPancakes, nextDepth, i, h2));
            }
        }
        return count;
    }


    private static int[] flipAt(int[] pancakes, int position) {
        int[] temp = Arrays.copyOf(pancakes, pancakes.length);
        for(int i = 0; i < position; i++) {
            temp[i] = pancakes[position-i-1];
        }
        return temp;
    }


    private static void printSolution(int[] pancakes, int[] flips) {
        int i = 0;
        do {
            System.out.print("State "+i+": ");
            for (int j = 0; j < flips[i];j++) {
                System.out.print(pancakes[j]+", ");
            }
            System.out.print("| ");
            for(int j = flips[i];j<pancakes.length; j++ ) {
                System.out.print(pancakes[j]+", ");
            }
            System.out.println();
            pancakes = flipAt(pancakes, flips[++i]);
        } while (flips[i]!= 0);
    }
}
