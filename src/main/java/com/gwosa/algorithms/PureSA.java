package com.gwosa.algorithms;

import com.gwosa.simulator.CloudEnvironment;

import java.util.Random;

/**
 * Pure Simulated Annealing (SA) metaheuristic for cloud task scheduling.
 * Uses discrete representation (integer VM indices).
 * Generates neighbors by reassigning random tasks to different VMs.
 */
public class PureSA extends AbstractMetaheuristic {

    private final int maxIter;
    private final double T0;
    private final double coolingRate;
    private final double Tmin;

    public PureSA(int maxIter, double T0, double coolingRate, double Tmin, int seed) {
        super("Pure SA", seed);
        this.maxIter = maxIter;
        this.T0 = T0;
        this.coolingRate = coolingRate;
        this.Tmin = Tmin;
    }

    public PureSA() {
        this(5000, 100.0, 0.97, 0.001, 42);
    }

    @Override
    public int[] schedule(CloudEnvironment env) {
        Random rng = new Random(seed);
        int n = env.getNumTasks();
        int m = env.getNumVMs();
        convergenceCurve.clear();
        long startTime = System.nanoTime();

        // Start with random discrete solution
        int[] current = new int[n];
        for (int j = 0; j < n; j++) {
            current[j] = rng.nextInt(m);
        }
        var curRes = env.evaluateSchedule(current);
        double currentFitness = curRes.getFitness();
        double[] ct = curRes.getCompletionTimePerVM();

        int[] best = current.clone();
        double bestFitness = currentFitness;
        convergenceCurve.add(bestFitness);

        // Adaptive cooling factor so temperature smoothly spans all maxIter iterations
        double effectiveCooling = Math.pow(Tmin / T0, 1.0 / maxIter);
        double T = T0;

        for (int iter = 0; iter < maxIter; iter++) {
            int[] neighbor = current.clone();

            if (rng.nextDouble() < 0.60 && ct.length > 0) {
                // Bottleneck-guided move: identify highest loaded VM
                int maxVM = 0;
                double maxCT = ct[0];
                for (int v = 1; v < m; v++) {
                    if (ct[v] > maxCT) {
                        maxCT = ct[v];
                        maxVM = v;
                    }
                }

                int[] tasksOnMax = new int[n];
                int count = 0;
                for (int j = 0; j < n; j++) {
                    if (neighbor[j] == maxVM) {
                        tasksOnMax[count++] = j;
                    }
                }

                if (count > 0) {
                    int tIdx = tasksOnMax[rng.nextInt(count)];
                    neighbor[tIdx] = rng.nextInt(m);
                } else {
                    neighbor[rng.nextInt(n)] = rng.nextInt(m);
                }
            } else {
                // Standard random multi-task perturbation (1-3 tasks)
                int numPerturb = 1 + rng.nextInt(3);
                for (int p = 0; p < numPerturb; p++) {
                    int randTask = rng.nextInt(n);
                    neighbor[randTask] = rng.nextInt(m);
                }
            }

            var nRes = env.evaluateSchedule(neighbor);
            double neighborFitness = nRes.getFitness();
            double delta = neighborFitness - currentFitness;

            if (delta < 0.0) {
                current = neighbor;
                currentFitness = neighborFitness;
                ct = nRes.getCompletionTimePerVM();
            } else if (T > 0.0) {
                double prob = Math.exp(-delta / T);
                if (rng.nextDouble() < prob) {
                    current = neighbor;
                    currentFitness = neighborFitness;
                    ct = nRes.getCompletionTimePerVM();
                }
            }

            if (currentFitness < bestFitness) {
                best = current.clone();
                bestFitness = currentFitness;
            }

            T *= effectiveCooling;
            convergenceCurve.add(bestFitness);
        }

        executionTimeMs = (System.nanoTime() - startTime) / 1_000_000.0;
        return best;
    }

    public int getMaxIter() { return maxIter; }
    public double getT0() { return T0; }
    public double getCoolingRate() { return coolingRate; }
    public double getTmin() { return Tmin; }
}
