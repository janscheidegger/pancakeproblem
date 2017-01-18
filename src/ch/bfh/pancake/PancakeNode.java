package ch.bfh.pancake;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Created by Jan on 16.01.2017.
 */
public class PancakeNode implements Serializable {
    int[] pancakes;
    int depth;
    int lastcut;
    int heuristic;
    int[] cuts;


    public PancakeNode(int[] pancakes, int depth, int lastcut, int heuristic) {
        this.pancakes = pancakes;
        this.depth = depth;
        this.lastcut = lastcut;
        this.heuristic = heuristic;
    }

    public PancakeNode(int[] pancakes, int depth, int lastcut, int heuristic, int[] lastcuts) {
        this.pancakes = pancakes;
        this.depth = depth;
        this.lastcut = lastcut;
        this.heuristic = heuristic;
        this.cuts = Arrays.copyOf(lastcuts, lastcuts.length);
        this.cuts[depth] = lastcut;
    }

    public boolean isSorted() {
        int previous = pancakes[0];
        for (int i = 1; i < pancakes.length; i++) {
            if (previous > pancakes[i]) {
                return false;
            }
            previous = pancakes[i];
        }
        return true;
    }

    @Override
    public String toString() {
        return "PancakeNode{" +
                "pancakes=" + Arrays.toString(pancakes) +
                ", depth=" + depth +
                ", lastcut=" + lastcut +
                ", heuristic=" + heuristic +
                '}';
    }
}
