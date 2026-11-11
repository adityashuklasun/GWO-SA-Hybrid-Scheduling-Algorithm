package com.gwosa.algorithms;

import com.gwosa.simulator.CloudEnvironment;

import java.util.Random;

// Hybrid WOA-GS (Acharya et al., 2025): Whale Optimization with Golden Section search
public class HybridWOAGS extends AbstractMetaheuristic {

    private final int popSize;
    private final int maxIter;
    private static final double GOLDEN_RATIO = (Math.sqrt(5.0) - 1.0) / 2.0; // ~0.6180339887

    public HybridWOAGS(int popSize, int maxIter, int seed) {
        super("Hybrid WOA-GS [Acharya '25]", seed);
        this.popSize = popSize;
        this.maxIter = maxIter;
    }

    public HybridWOAGS() {
        this(30, 200, 42);
    }

    @Override
    public int[] schedule(CloudEnvironment env) {
        Random rng = new Random(seed);
        int n = env.getNumTasks();
        int m = env.getNumVMs();
        convergenceCurve.clear();
        long startTime = System.nanoTime();

        // 1. Initialize whale population
        int[][] whales = new int[popSize][n];
        double[] fitness = new double[popSize];

        int[] bestWhale = new int[n];
        double bestFitness = Double.POSITIVE_INFINITY;

        for (int i = 0; i < popSize; i++) {
            for (int j = 0; j < n; j++) {
                whales[i][j] = rng.nextInt(m);
            }
            fitness[i] = env.evaluateSchedule(whales[i]).getFitness();
            if (fitness[i] < bestFitness) {
                bestFitness = fitness[i];
                bestWhale = whales[i].clone();
            }
        }
        convergenceCurve.add(bestFitness);

        // 2. Main WOA loop
        for (int t = 0; t < maxIter; t++) {
            double a = 2.0 - t * (2.0 / maxIter); // a decreases linearly from 2 to 0
            double a2 = -1.0 + t * (-1.0 / maxIter); // a2 linearly decreases from -1 to -2

            for (int i = 0; i < popSize; i++) {
                double r = rng.nextDouble();
                double A = 2.0 * a * r - a;
                double C = 2.0 * r;
                double p = rng.nextDouble();

                int[] newPos = new int[n];
                for (int j = 0; j < n; j++) {
                    if (p < 0.5) {
                        if (Math.abs(A) < 1.0) {
                            // Encircling prey (moving toward best whale)
                            double followProb = 1.0 - Math.abs(A);
                            newPos[j] = rng.nextDouble() < followProb ? bestWhale[j] : rng.nextInt(m);
                        } else {
                            // Search for prey (exploration: random whale)
                            int randWhaleIdx = rng.nextInt(popSize);
                            newPos[j] = whales[randWhaleIdx][j];
                        }
                    } else {
                        // Spiral bubble-net attacking
                        double l = (a2 - 1.0) * rng.nextDouble() + 1.0;
                        double spiralProb = Math.exp(l) * Math.cos(2.0 * Math.PI * l);
                        if (rng.nextDouble() < Math.abs(spiralProb)) {
                            newPos[j] = bestWhale[j];
                        } else {
                            newPos[j] = rng.nextInt(m);
                        }
                    }
                }
                whales[i] = newPos;
                fitness[i] = env.evaluateSchedule(whales[i]).getFitness();

                if (fitness[i] < bestFitness) {
                    bestFitness = fitness[i];
                    bestWhale = whales[i].clone();
                }
            }

            // 3. Golden Section Search Local Exploitation on the best solution
            bestWhale = goldenSearchExploit(bestWhale, env, m, n, rng);
            bestFitness = env.evaluateSchedule(bestWhale).getFitness();

            convergenceCurve.add(bestFitness);
        }

        executionTimeMs = (System.nanoTime() - startTime) / 1_000_000.0;
        return bestWhale;
    }

    /**
     * Discrete Golden Section Search: evaluates discrete interval subdivisions around best whale.
     */
    private int[] goldenSearchExploit(int[] best, CloudEnvironment env, int numVMs, int numTasks, Random rng) {
        int[] candidate = best.clone();
        double currentFit = env.evaluateSchedule(candidate).getFitness();

        int searchSteps = 5;
        for (int s = 0; s < searchSteps; s++) {
            int taskIdx = rng.nextInt(numTasks);
            int origVM = candidate[taskIdx];

            // Golden step calculation
            int step = (int) Math.round((numVMs - 1) * (1.0 - GOLDEN_RATIO));
            int trialVM1 = (origVM + step) % numVMs;
            int trialVM2 = (origVM + (numVMs - step)) % numVMs;

            candidate[taskIdx] = trialVM1;
            double fit1 = env.evaluateSchedule(candidate).getFitness();

            candidate[taskIdx] = trialVM2;
            double fit2 = env.evaluateSchedule(candidate).getFitness();

            if (fit1 < currentFit && fit1 <= fit2) {
                candidate[taskIdx] = trialVM1;
                currentFit = fit1;
            } else if (fit2 < currentFit) {
                candidate[taskIdx] = trialVM2;
                currentFit = fit2;
            } else {
                candidate[taskIdx] = origVM; // Revert
            }
        }
        return candidate;
    }
}
