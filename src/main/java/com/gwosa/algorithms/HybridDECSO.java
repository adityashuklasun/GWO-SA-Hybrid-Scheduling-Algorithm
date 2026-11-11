package com.gwosa.algorithms;

import com.gwosa.simulator.CloudEnvironment;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

// Hybrid DECSO (Gunduz & Gurkas-Aydin, 2025): Differential Evolution + Chicken Swarm
public class HybridDECSO extends AbstractMetaheuristic {

    private final int popSize;
    private final int maxIter;
    private final double F;   // DE mutation scaling factor
    private final double CR;  // DE crossover rate

    public HybridDECSO(int popSize, int maxIter, double F, double CR, int seed) {
        super("Hybrid DECSO [Gunduz '25]", seed);
        this.popSize = popSize;
        this.maxIter = maxIter;
        this.F = F;
        this.CR = CR;
    }

    public HybridDECSO() {
        this(30, 200, 0.5, 0.7, 42);
    }

    @Override
    public int[] schedule(CloudEnvironment env) {
        Random rng = new Random(seed);
        int n = env.getNumTasks();
        int m = env.getNumVMs();
        convergenceCurve.clear();
        long startTime = System.nanoTime();

        // 1. Initialize chicken swarm
        int[][] swarm = new int[popSize][n];
        double[] fitness = new double[popSize];

        for (int i = 0; i < popSize; i++) {
            for (int j = 0; j < n; j++) {
                swarm[i][j] = rng.nextInt(m);
            }
            fitness[i] = env.evaluateSchedule(swarm[i]).getFitness();
        }

        int[] sorted = sortIndices(fitness);
        int[] globalBest = swarm[sorted[0]].clone();
        double bestFitness = fitness[sorted[0]];
        convergenceCurve.add(bestFitness);

        // Group partition sizes: Roosters (top 20%), Hens (next 60%), Chicks (bottom 20%)
        int rNum = Math.max(2, popSize / 5);
        int hNum = popSize * 3 / 5;

        // 2. Main DECSO evolutionary loop
        for (int t = 0; t < maxIter; t++) {
            // Re-order hierarchy every 10 iterations
            if (t % 10 == 0) {
                sorted = sortIndices(fitness);
                globalBest = swarm[sorted[0]].clone();
                bestFitness = fitness[sorted[0]];
            }

            for (int i = 0; i < popSize; i++) {
                int[] trial = new int[n];

                // Differential Evolution Mutation & Crossover
                int r1 = rng.nextInt(popSize);
                int r2 = rng.nextInt(popSize);
                int r3 = rng.nextInt(popSize);
                while (r1 == i) r1 = rng.nextInt(popSize);
                while (r2 == i || r2 == r1) r2 = rng.nextInt(popSize);
                while (r3 == i || r3 == r1 || r3 == r2) r3 = rng.nextInt(popSize);

                int jRand = rng.nextInt(n);

                for (int j = 0; j < n; j++) {
                    if (rng.nextDouble() < CR || j == jRand) {
                        // DE mutation step combined with CSO role guidance
                        if (i < rNum) {
                            // Rooster: leadership exploitation
                            trial[j] = rng.nextDouble() < 0.7 ? globalBest[j] : swarm[r1][j];
                        } else if (i < rNum + hNum) {
                            // Hen: forage with rooster partner
                            int roosterIdx = sorted[rng.nextInt(rNum)];
                            trial[j] = rng.nextDouble() < 0.5 ? swarm[roosterIdx][j] : swarm[r1][j];
                        } else {
                            // Chick: follow mother hen + DE mutation
                            int diff = swarm[r2][j] - swarm[r3][j];
                            int mutated = (int) Math.round(swarm[r1][j] + F * diff);
                            mutated = ((mutated % m) + m) % m;
                            trial[j] = mutated;
                        }
                    } else {
                        trial[j] = swarm[i][j];
                    }
                }

                // Selection (greedy replacement)
                double trialFitness = env.evaluateSchedule(trial).getFitness();
                if (trialFitness < fitness[i]) {
                    swarm[i] = trial;
                    fitness[i] = trialFitness;

                    if (trialFitness < bestFitness) {
                        bestFitness = trialFitness;
                        globalBest = trial.clone();
                    }
                }
            }

            convergenceCurve.add(bestFitness);
        }

        executionTimeMs = (System.nanoTime() - startTime) / 1_000_000.0;
        return globalBest;
    }

    private int[] sortIndices(double[] values) {
        Integer[] indices = new Integer[values.length];
        for (int i = 0; i < values.length; i++) indices[i] = i;
        Arrays.sort(indices, Comparator.comparingDouble(i -> values[i]));
        int[] result = new int[values.length];
        for (int i = 0; i < values.length; i++) result[i] = indices[i];
        return result;
    }
}
