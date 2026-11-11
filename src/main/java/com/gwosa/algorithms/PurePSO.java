package com.gwosa.algorithms;

import com.gwosa.simulator.CloudEnvironment;

import java.util.Random;

/**
 * Pure Particle Swarm Optimization (PSO) metaheuristic for cloud task scheduling.
 * Uses discrete representation with velocity-based probability of changing VM assignment.
 */
public class PurePSO extends AbstractMetaheuristic {

    private final int popSize;
    private final int maxIter;
    private final double w;
    private final double c1;
    private final double c2;

    public PurePSO(int popSize, int maxIter, double w, double c1, double c2, int seed) {
        super("Pure PSO", seed);
        this.popSize = popSize;
        this.maxIter = maxIter;
        this.w = w;
        this.c1 = c1;
        this.c2 = c2;
    }

    public PurePSO() {
        this(30, 200, 0.7, 1.5, 1.5, 42);
    }

    @Override
    public int[] schedule(CloudEnvironment env) {
        Random rng = new Random(seed);
        int n = env.getNumTasks();
        int m = env.getNumVMs();
        convergenceCurve.clear();
        long startTime = System.nanoTime();

        // Discrete PSO: positions are integer VM indices, velocities are real-valued
        int[][] x = new int[popSize][n];
        double[][] v = new double[popSize][n];
        int[][] pbest = new int[popSize][n];
        double[] pbestFitness = new double[popSize];

        int[] gbest = new int[n];
        double gbestFitness = Double.POSITIVE_INFINITY;

        // Initialize
        for (int i = 0; i < popSize; i++) {
            for (int j = 0; j < n; j++) {
                x[i][j] = rng.nextInt(m);
                v[i][j] = (rng.nextDouble() * 2.0 - 1.0) * 0.5;
            }
            pbest[i] = x[i].clone();
            double fitness = env.evaluateSchedule(x[i]).getFitness();
            pbestFitness[i] = fitness;
            if (fitness < gbestFitness) {
                gbestFitness = fitness;
                gbest = x[i].clone();
            }
        }
        convergenceCurve.add(gbestFitness);

        for (int t = 0; t < maxIter; t++) {
            for (int i = 0; i < popSize; i++) {
                for (int j = 0; j < n; j++) {
                    double r1 = rng.nextDouble();
                    double r2 = rng.nextDouble();

                    // Velocity update (cognitive + social components)
                    double cogComp = (pbest[i][j] == x[i][j]) ? 0 : c1 * r1;
                    double socComp = (gbest[j] == x[i][j]) ? 0 : c2 * r2;
                    v[i][j] = w * v[i][j] + cogComp + socComp;

                    // Sigmoid probability of changing position
                    double sigmoid = 1.0 / (1.0 + Math.exp(-v[i][j]));

                    if (rng.nextDouble() < sigmoid) {
                        // Move towards pbest or gbest
                        if (rng.nextDouble() < 0.5) {
                            x[i][j] = pbest[i][j];
                        } else {
                            x[i][j] = gbest[j];
                        }
                    }
                    // else: stay at current position

                    // Small mutation probability for exploration
                    if (rng.nextDouble() < 0.05) {
                        x[i][j] = rng.nextInt(m);
                    }
                }

                double fitness = env.evaluateSchedule(x[i]).getFitness();
                if (fitness < pbestFitness[i]) {
                    pbestFitness[i] = fitness;
                    pbest[i] = x[i].clone();
                    if (fitness < gbestFitness) {
                        gbestFitness = fitness;
                        gbest = x[i].clone();
                    }
                }
            }
            convergenceCurve.add(gbestFitness);
        }

        executionTimeMs = (System.nanoTime() - startTime) / 1_000_000.0;
        return gbest;
    }

    public int getPopSize() { return popSize; }
    public int getMaxIter() { return maxIter; }
    public double getW() { return w; }
    public double getC1() { return c1; }
    public double getC2() { return c2; }
}
