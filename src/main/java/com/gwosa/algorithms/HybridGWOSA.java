package com.gwosa.algorithms;

import com.gwosa.model.ScheduleResult;
import com.gwosa.simulator.CloudEnvironment;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

// Hybrid GWO-SA: Grey Wolf Optimizer with Simulated Annealing refinement on leaders
public class HybridGWOSA extends AbstractMetaheuristic {

    private final int popSize;
    private final int maxIter;
    private final double T0;
    private final double coolingRate;
    private final double Tmin;
    private final int saIters;

    public HybridGWOSA(int popSize, int maxIter, double T0, double coolingRate,
                       double Tmin, int saIters, int seed) {
        super("Hybrid GWO-SA", seed);
        this.popSize = popSize;
        this.maxIter = maxIter;
        this.T0 = T0;
        this.coolingRate = coolingRate;
        this.Tmin = Tmin;
        this.saIters = saIters;
    }

    /** Default parameters */
    public HybridGWOSA() {
        this(30, 350, 50.0, 0.97, 0.001, 25, 42);
    }

    @Override
    public int[] schedule(CloudEnvironment env) {
        Random rng = new Random(seed);
        int n = env.getNumTasks();
        int m = env.getNumVMs();
        convergenceCurve.clear();
        long startTime = System.nanoTime();

        // ===== STEP 1: Initialize population with SMART seeding =====
        int[][] wolves = new int[popSize][n];
        double[] fitnessValues = new double[popSize];

        // Wolf 0: FCFS-based initialization (assign to least-loaded VM)
        wolves[0] = initFCFS(env, n, m);
        fitnessValues[0] = env.evaluateSchedule(wolves[0]).getFitness();

        // Wolf 1: Min-Min inspired initialization
        wolves[1] = initMinMin(env, n, m);
        fitnessValues[1] = env.evaluateSchedule(wolves[1]).getFitness();

        // Remaining wolves: random initialization
        for (int i = 2; i < popSize; i++) {
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

        // Track global best separately
        int[] globalBest = alpha.clone();
        double globalBestFitness = alphaFitness;

        convergenceCurve.add(globalBestFitness);

        double coolingFactor = Math.pow(Tmin / T0, 1.0 / maxIter);
        double T = T0;

        // ===== STEP 2: Main loop =====
        for (int t = 0; t < maxIter; t++) {
            double a = 2.0 * (1.0 - (double) t / maxIter);

            // --- GWO PHASE: Update wolf positions (discrete, preserving elitism for alpha) ---
            for (int i = 1; i < popSize; i++) {
                int idx = sorted[i];
                int[] newPos = new int[n];
                for (int j = 0; j < n; j++) {
                    double r1 = rng.nextDouble();
                    double followProb = 1.0 - a / 2.0;

                    if (r1 < 0.40) {
                        // Follow alpha
                        newPos[j] = rng.nextDouble() < followProb ? alpha[j] : rng.nextInt(m);
                    } else if (r1 < 0.70) {
                        // Follow beta
                        newPos[j] = rng.nextDouble() < followProb ? beta[j] : rng.nextInt(m);
                    } else {
                        // Follow delta
                        newPos[j] = rng.nextDouble() < followProb ? delta[j] : rng.nextInt(m);
                    }

                    // Exploration: with probability proportional to |A|, explore randomly
                    double A = 2 * a * rng.nextDouble() - a;
                    if (Math.abs(A) > 1.0 && rng.nextDouble() < 0.20) {
                        newPos[j] = rng.nextInt(m); // random exploration
                    }
                }
                wolves[idx] = newPos;
                fitnessValues[idx] = env.evaluateSchedule(wolves[idx]).getFitness();
            }

            // --- SA PHASE: Refine top 3 leaders with bottleneck guidance ---
            wolves[sorted[0]] = saRefine(alpha, alphaFitness, T, env, m, n, rng);
            fitnessValues[sorted[0]] = env.evaluateSchedule(wolves[sorted[0]]).getFitness();

            wolves[sorted[1]] = saRefine(beta, fitnessValues[sorted[1]], T, env, m, n, rng);
            fitnessValues[sorted[1]] = env.evaluateSchedule(wolves[sorted[1]]).getFitness();

            wolves[sorted[2]] = saRefine(delta, fitnessValues[sorted[2]], T, env, m, n, rng);
            fitnessValues[sorted[2]] = env.evaluateSchedule(wolves[sorted[2]]).getFitness();

            // --- RE-RANK ---
            sorted = sortIndices(fitnessValues);
            alpha = wolves[sorted[0]].clone();
            beta  = wolves[sorted[1]].clone();
            delta = wolves[sorted[2]].clone();
            alphaFitness = fitnessValues[sorted[0]];

            // Update global best & enforce elitist feedback
            if (alphaFitness < globalBestFitness) {
                globalBest = alpha.clone();
                globalBestFitness = alphaFitness;
            } else {
                wolves[sorted[popSize - 1]] = globalBest.clone();
                fitnessValues[sorted[popSize - 1]] = globalBestFitness;
            }

            // --- DIVERSITY: Re-initialize worst 20% of population every 25 iterations ---
            if (t > 0 && t % 25 == 0) {
                int numReinit = popSize / 5;
                for (int k = popSize - numReinit; k < popSize; k++) {
                    int idx = sorted[k];
                    for (int j = 0; j < n; j++) {
                        wolves[idx][j] = rng.nextInt(m);
                    }
                    fitnessValues[idx] = env.evaluateSchedule(wolves[idx]).getFitness();
                }
            }

            // Cool temperature
            T *= coolingFactor;
            convergenceCurve.add(globalBestFitness);
        }

        executionTimeMs = (System.nanoTime() - startTime) / 1_000_000.0;
        return globalBest;
    }

    // SA local search: 70% bottleneck VM offloading, 30% random swaps
    private int[] saRefine(int[] leader, double leaderFitness,
                           double T, CloudEnvironment env,
                           int numVMs, int numTasks, Random rng) {
        int[] current = leader.clone();
        double currentFitness = leaderFitness;
        int[] best = leader.clone();
        double bestFitness = leaderFitness;

        ScheduleResult curRes = env.evaluateSchedule(current);
        double[] ct = curRes.getCompletionTimePerVM();

        for (int iter = 0; iter < saIters; iter++) {
            int[] neighbor = current.clone();

            if (rng.nextDouble() < 0.70 && ct.length > 0) {
                // Bottleneck-guided move: identify highest loaded VM
                int maxVM = 0;
                double maxCT = ct[0];
                for (int v = 1; v < numVMs; v++) {
                    if (ct[v] > maxCT) {
                        maxCT = ct[v];
                        maxVM = v;
                    }
                }

                int[] tasksOnMaxVM = new int[numTasks];
                int count = 0;
                for (int j = 0; j < numTasks; j++) {
                    if (neighbor[j] == maxVM) {
                        tasksOnMaxVM[count++] = j;
                    }
                }

                if (count > 0) {
                    int taskToMove = tasksOnMaxVM[rng.nextInt(count)];
                    neighbor[taskToMove] = rng.nextInt(numVMs);
                } else {
                    int randTask = rng.nextInt(numTasks);
                    neighbor[randTask] = rng.nextInt(numVMs);
                }
            } else {
                // Exploratory random multi-task swap (1-3 tasks)
                int numSwaps = 1 + rng.nextInt(3);
                for (int s = 0; s < numSwaps; s++) {
                    int randTask = rng.nextInt(numTasks);
                    neighbor[randTask] = rng.nextInt(numVMs);
                }
            }

            ScheduleResult nRes = env.evaluateSchedule(neighbor);
            double neighborFitness = nRes.getFitness();
            double delta = neighborFitness - currentFitness;

            if (delta < 0) {
                current = neighbor;
                currentFitness = neighborFitness;
                ct = nRes.getCompletionTimePerVM();
            } else if (T > 0) {
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
        }
        return best;
    }

    /** FCFS-based initialization: assign each task to least-loaded VM */
    private int[] initFCFS(CloudEnvironment env, int n, int m) {
        int[] mapping = new int[n];
        double[] vmLoad = new double[m];
        var tasks = env.getTasks();
        var vms = env.getVMs();

        for (int i = 0; i < n; i++) {
            int bestVM = 0;
            double minCT = Double.MAX_VALUE;
            for (int j = 0; j < m; j++) {
                double ct = vmLoad[j] + (double) tasks.get(i).getWorkload() / vms.get(j).getMips();
                if (ct < minCT) {
                    minCT = ct;
                    bestVM = j;
                }
            }
            mapping[i] = bestVM;
            vmLoad[bestVM] += (double) tasks.get(i).getWorkload() / vms.get(bestVM).getMips();
        }
        return mapping;
    }

    /** Min-Min inspired initialization */
    private int[] initMinMin(CloudEnvironment env, int n, int m) {
        var tasks = env.getTasks();
        var vms = env.getVMs();
        int[] mapping = new int[n];
        Arrays.fill(mapping, -1);
        double[] vmLoad = new double[m];
        boolean[] assigned = new boolean[n];

        for (int count = 0; count < n; count++) {
            int bestTask = -1;
            int bestVM = -1;
            double bestCT = Double.MAX_VALUE;

            for (int i = 0; i < n; i++) {
                if (assigned[i]) continue;
                for (int j = 0; j < m; j++) {
                    double ct = vmLoad[j] + (double) tasks.get(i).getWorkload() / vms.get(j).getMips();
                    if (ct < bestCT) {
                        bestCT = ct;
                        bestTask = i;
                        bestVM = j;
                    }
                }
            }

            mapping[bestTask] = bestVM;
            assigned[bestTask] = true;
            vmLoad[bestVM] += (double) tasks.get(bestTask).getWorkload() / vms.get(bestVM).getMips();
        }
        return mapping;
    }

    private int[] sortIndices(double[] values) {
        Integer[] indices = new Integer[values.length];
        for (int i = 0; i < values.length; i++) indices[i] = i;
        Arrays.sort(indices, Comparator.comparingDouble(i -> values[i]));
        int[] result = new int[values.length];
        for (int i = 0; i < values.length; i++) result[i] = indices[i];
        return result;
    }

    // Getters
    public int getPopSize() { return popSize; }
    public int getMaxIter() { return maxIter; }
    public double getT0() { return T0; }
    public double getCoolingRate() { return coolingRate; }
    public double getTmin() { return Tmin; }
    public int getSaIters() { return saIters; }
}
