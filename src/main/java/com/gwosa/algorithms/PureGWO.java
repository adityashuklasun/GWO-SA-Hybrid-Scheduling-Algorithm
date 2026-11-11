package com.gwosa.algorithms;

import com.gwosa.simulator.CloudEnvironment;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

/**
 * Pure Grey Wolf Optimizer (GWO) metaheuristic for cloud task scheduling.
 * Uses discrete representation (integer VM indices) with probabilistic
 * leader-following for the position update.
 */
public class PureGWO extends AbstractMetaheuristic {

    private final int popSize;
    private final int maxIter;

    public PureGWO(int popSize, int maxIter, int seed) {
        super("Pure GWO", seed);
        this.popSize = popSize;
        this.maxIter = maxIter;
    }

    public PureGWO() {
        this(30, 400, 42);
    }

    @Override
    public int[] schedule(CloudEnvironment env) {
        Random rng = new Random(seed);
        int n = env.getNumTasks();
        int m = env.getNumVMs();
        convergenceCurve.clear();
        long startTime = System.nanoTime();

        // Initialize population: random VM assignments
        int[][] wolves = new int[popSize][n];
        double[] fitnessValues = new double[popSize];

        for (int i = 0; i < popSize; i++) {
            for (int j = 0; j < n; j++) {
                wolves[i][j] = rng.nextInt(m);
            }
            fitnessValues[i] = env.evaluateSchedule(wolves[i]).getFitness();
        }

        // Identify alpha, beta, delta
        int[] sorted = sortIndices(fitnessValues);
        int[] alpha = wolves[sorted[0]].clone();
        int[] beta  = wolves[sorted[1]].clone();
        int[] delta = wolves[sorted[2]].clone();
        double alphaFitness = fitnessValues[sorted[0]];

        int[] globalBest = alpha.clone();
        double globalBestFitness = alphaFitness;

        convergenceCurve.add(globalBestFitness);

        for (int t = 0; t < maxIter; t++) {
            double a = 2.0 - t * (2.0 / maxIter);

            for (int i = 0; i < popSize; i++) {
                int[] newPos = new int[n];
                for (int j = 0; j < n; j++) {
                    double r = rng.nextDouble();
                    double followProb = 1.0 - a / 2.0;

                    if (r < 0.33) {
                        newPos[j] = rng.nextDouble() < followProb ? alpha[j] : rng.nextInt(m);
                    } else if (r < 0.66) {
                        newPos[j] = rng.nextDouble() < followProb ? beta[j] : rng.nextInt(m);
                    } else {
                        newPos[j] = rng.nextDouble() < followProb ? delta[j] : rng.nextInt(m);
                    }

                    // Extra exploration when |A| > 1
                    double A = 2 * a * rng.nextDouble() - a;
                    if (Math.abs(A) > 1 && rng.nextDouble() < 0.2) {
                        newPos[j] = rng.nextInt(m);
                    }
                }
                wolves[i] = newPos;
                fitnessValues[i] = env.evaluateSchedule(wolves[i]).getFitness();
            }

            sorted = sortIndices(fitnessValues);
            alpha = wolves[sorted[0]].clone();
            beta  = wolves[sorted[1]].clone();
            delta = wolves[sorted[2]].clone();
            alphaFitness = fitnessValues[sorted[0]];

            if (alphaFitness < globalBestFitness) {
                globalBest = alpha.clone();
                globalBestFitness = alphaFitness;
            }

            convergenceCurve.add(globalBestFitness);
        }

        executionTimeMs = (System.nanoTime() - startTime) / 1_000_000.0;
        return globalBest;
    }

    protected int[] sortIndices(double[] values) {
        Integer[] indices = new Integer[values.length];
        for (int i = 0; i < values.length; i++) indices[i] = i;
        Arrays.sort(indices, Comparator.comparingDouble(i -> values[i]));
        int[] result = new int[values.length];
        for (int i = 0; i < values.length; i++) result[i] = indices[i];
        return result;
    }

    public int getPopSize() { return popSize; }
    public int getMaxIter() { return maxIter; }
}
