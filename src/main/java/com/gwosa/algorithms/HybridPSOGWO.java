package com.gwosa.algorithms;

import com.gwosa.simulator.CloudEnvironment;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

// Hybrid PSO-GWO (Kumari et al., 2025): combines PSO velocity with GWO wolf leaders
public class HybridPSOGWO extends AbstractMetaheuristic {

    private final int popSize;
    private final int maxIter;
    private final double w;  // inertia weight
    private final double c1; // cognitive acceleration
    private final double c2; // social acceleration

    public HybridPSOGWO(int popSize, int maxIter, double w, double c1, double c2, int seed) {
        super("Hybrid PSO-GWO [Kumari '25]", seed);
        this.popSize = popSize;
        this.maxIter = maxIter;
        this.w = w;
        this.c1 = c1;
        this.c2 = c2;
    }

    public HybridPSOGWO() {
        this(30, 200, 0.7, 1.5, 1.5, 42);
    }

    @Override
    public int[] schedule(CloudEnvironment env) {
        Random rng = new Random(seed);
        int n = env.getNumTasks();
        int m = env.getNumVMs();
        convergenceCurve.clear();
        long startTime = System.nanoTime();

        // 1. Initialize hybrid population & velocities
        int[][] positions = new int[popSize][n];
        double[][] velocities = new double[popSize][n];
        int[][] pbest = new int[popSize][n];
        double[] pbestFitness = new double[popSize];
        double[] fitnessValues = new double[popSize];

        for (int i = 0; i < popSize; i++) {
            for (int j = 0; j < n; j++) {
                positions[i][j] = rng.nextInt(m);
                velocities[i][j] = (rng.nextDouble() * 2.0 - 1.0) * 0.5;
            }
            pbest[i] = positions[i].clone();
            fitnessValues[i] = env.evaluateSchedule(positions[i]).getFitness();
            pbestFitness[i] = fitnessValues[i];
        }

        // Rank to identify GWO leaders
        int[] sorted = sortIndices(fitnessValues);
        int[] alpha = positions[sorted[0]].clone();
        int[] beta  = positions[sorted[1]].clone();
        int[] delta = positions[sorted[2]].clone();
        double gbestFitness = fitnessValues[sorted[0]];

        convergenceCurve.add(gbestFitness);

        // 2. Main hybrid loop
        for (int t = 0; t < maxIter; t++) {
            double a = 2.0 - t * (2.0 / maxIter); // GWO parameter decay

            for (int i = 0; i < popSize; i++) {
                int[] newPos = new int[n];
                for (int j = 0; j < n; j++) {
                    double r1 = rng.nextDouble();
                    double r2 = rng.nextDouble();

                    // PSO Velocity update directed toward alpha (gbest)
                    double cog = (pbest[i][j] == positions[i][j]) ? 0.0 : c1 * r1;
                    double soc = (alpha[j] == positions[i][j]) ? 0.0 : c2 * r2;
                    velocities[i][j] = w * velocities[i][j] + cog + soc;

                    double sigmoid = 1.0 / (1.0 + Math.exp(-velocities[i][j]));

                    // GWO Encircling Influence
                    double followProb = 1.0 - a / 2.0;

                    if (rng.nextDouble() < sigmoid) {
                        // Guided by wolf leadership
                        double leaderPick = rng.nextDouble();
                        if (leaderPick < 0.5) {
                            newPos[j] = alpha[j];
                        } else if (leaderPick < 0.8) {
                            newPos[j] = beta[j];
                        } else {
                            newPos[j] = delta[j];
                        }
                    } else if (rng.nextDouble() < followProb) {
                        newPos[j] = pbest[i][j];
                    } else {
                        newPos[j] = rng.nextInt(m); // Random perturbation
                    }
                }

                positions[i] = newPos;
                fitnessValues[i] = env.evaluateSchedule(positions[i]).getFitness();

                // Update pbest
                if (fitnessValues[i] < pbestFitness[i]) {
                    pbestFitness[i] = fitnessValues[i];
                    pbest[i] = positions[i].clone();
                }
            }

            // Update GWO leaders (alpha, beta, delta)
            sorted = sortIndices(fitnessValues);
            alpha = positions[sorted[0]].clone();
            beta  = positions[sorted[1]].clone();
            delta = positions[sorted[2]].clone();

            if (fitnessValues[sorted[0]] < gbestFitness) {
                gbestFitness = fitnessValues[sorted[0]];
            }

            convergenceCurve.add(gbestFitness);
        }

        executionTimeMs = (System.nanoTime() - startTime) / 1_000_000.0;
        return alpha;
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
