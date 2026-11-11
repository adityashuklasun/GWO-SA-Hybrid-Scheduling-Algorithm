package com.gwosa;

import com.gwosa.algorithms.*;
import com.gwosa.model.ScheduleResult;
import com.gwosa.simulator.CloudEnvironment;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.*;

// Statistical validation over 30 independent runs (Mean, StdDev, Wilcoxon p-value)
public class StatisticalBenchmark {

    private static final int NUM_RUNS = 30;

    public static void main(String[] args) throws Exception {
        System.out.println("==========================================================================");
        System.out.println("  Statistical Evaluation (30 Independent Runs) - Hybrid GWO-SA Project");
        System.out.println("==========================================================================");

        List<CloudEnvironment> scenarios = List.of(
                CloudEnvironment.small(),
                CloudEnvironment.medium(),
                CloudEnvironment.large()
        );

        String[] algoNames = {
                "Round Robin",
                "FCFS",
                "Min-Min",
                "Pure GWO",
                "Pure SA",
                "Pure PSO",
                "Hybrid WOA-GS [Acharya '25]",
                "Hybrid PSO-GWO [Kumari '25]",
                "Hybrid DECSO [Gunduz '25]",
                "Hybrid GWO-SA"
        };

        File resultsDir = new File("results");
        resultsDir.mkdirs();
        File statFile = new File("results/statistical_results.csv");

        try (PrintWriter pw = new PrintWriter(new FileWriter(statFile))) {
            pw.println("Scenario,Algorithm,MeanMakespan,StdMakespan,BestMakespan,WorstMakespan,MeanCost,StdCost,MeanDoI,StdDoI,MeanFitness,Wilcoxon_P_Value");

            for (CloudEnvironment env : scenarios) {
                System.out.printf(Locale.US, "%n==========================================================================%n");
                System.out.printf(Locale.US, "SCENARIO: %s (%d Tasks, %d VMs) - %d Monte Carlo Runs%n",
                        env.getScenarioName(), env.getNumTasks(), env.getNumVMs(), NUM_RUNS);
                System.out.println("==========================================================================");
                System.out.printf(Locale.US, "%-30s | %18s | %18s | %14s | %10s%n",
                        "Algorithm", "Makespan (Mean±Std)", "Cost (Mean±Std)", "DoI (Mean±Std)", "p-value");
                System.out.println("-".repeat(102));

                // Map of algoName -> array of makespans across 30 runs
                Map<String, double[]> makespanRuns = new LinkedHashMap<>();
                Map<String, double[]> costRuns = new LinkedHashMap<>();
                Map<String, double[]> doiRuns = new LinkedHashMap<>();
                Map<String, double[]> fitnessRuns = new LinkedHashMap<>();

                for (String name : algoNames) {
                    makespanRuns.put(name, new double[NUM_RUNS]);
                    costRuns.put(name, new double[NUM_RUNS]);
                    doiRuns.put(name, new double[NUM_RUNS]);
                    fitnessRuns.put(name, new double[NUM_RUNS]);
                }

                // Execute 30 runs
                for (int run = 0; run < NUM_RUNS; run++) {
                    int seed = 100 + run * 37;

                    List<SchedulingAlgorithm> algos = List.of(
                            new RoundRobin(),
                            new FCFS(),
                            new MinMin(),
                            new PureGWO(30, 250, seed),
                            new PureSA(3000, 100.0, 0.97, 0.001, seed),
                            new PurePSO(30, 150, 0.7, 1.5, 1.5, seed),
                            new HybridWOAGS(30, 150, seed),
                            new HybridPSOGWO(30, 150, 0.7, 1.5, 1.5, seed),
                            new HybridDECSO(30, 150, 0.5, 0.7, seed),
                            new HybridGWOSA(30, 250, 50.0, 0.97, 0.001, 20, seed)
                    );

                    for (SchedulingAlgorithm algo : algos) {
                        int[] mapping = algo.schedule(env);
                        ScheduleResult res = env.evaluateSchedule(mapping);
                        String name = algo.getName();

                        makespanRuns.get(name)[run] = res.getMakespan();
                        costRuns.get(name)[run] = res.getCost();
                        doiRuns.get(name)[run] = res.getDegreeOfImbalance();
                        fitnessRuns.get(name)[run] = res.getFitness();
                    }
                }

                double[] gwoSaMakespans = makespanRuns.get("Hybrid GWO-SA");

                // Compute and print statistics
                for (String name : algoNames) {
                    double[] mks = makespanRuns.get(name);
                    double[] csts = costRuns.get(name);
                    double[] dois = doiRuns.get(name);
                    double[] fits = fitnessRuns.get(name);

                    double meanMk = mean(mks);
                    double stdMk = stdDev(mks, meanMk);
                    double bestMk = min(mks);
                    double worstMk = max(mks);

                    double meanCost = mean(csts);
                    double stdCost = stdDev(csts, meanCost);

                    double meanDoI = mean(dois);
                    double stdDoI = stdDev(dois, meanDoI);

                    double meanFit = mean(fits);

                    double pValue;
                    if (name.equals("Hybrid GWO-SA")) {
                        pValue = 1.0;
                    } else {
                        pValue = computeWilcoxonPValue(gwoSaMakespans, mks);
                    }

                    String pValStr = name.equals("Hybrid GWO-SA") ? "N/A (Ref)" :
                            (pValue < 0.001 ? "< 0.001 ***" : String.format(Locale.US, "%.4f %s", pValue, pValue < 0.05 ? "**" : ""));

                    System.out.printf(Locale.US, "%-30s | %8.2f ± %-7.2f | %8.2f ± %-7.2f | %5.3f ± %-5.3f | %10s%n",
                            name, meanMk, stdMk, meanCost, stdCost, meanDoI, stdDoI, pValStr);

                    pw.printf(Locale.US, "%s,%s,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.6f%n",
                            env.getScenarioName(), name, meanMk, stdMk, bestMk, worstMk,
                            meanCost, stdCost, meanDoI, stdDoI, meanFit, pValue);
                }
            }
        }

        System.out.println("\n[DONE] Statistical validation complete. Data saved to results/statistical_results.csv");
    }

    private static double mean(double[] arr) {
        double sum = 0;
        for (double v : arr) sum += v;
        return sum / arr.length;
    }

    private static double stdDev(double[] arr, double mean) {
        double sumSq = 0;
        for (double v : arr) sumSq += (v - mean) * (v - mean);
        return Math.sqrt(sumSq / arr.length);
    }

    private static double min(double[] arr) {
        double min = arr[0];
        for (double v : arr) if (v < min) min = v;
        return min;
    }

    private static double max(double[] arr) {
        double max = arr[0];
        for (double v : arr) if (v > max) max = v;
        return max;
    }

    /**
     * Computes asymptotic p-value for Wilcoxon signed-rank test between two paired arrays.
     */
    private static double computeWilcoxonPValue(double[] proposed, double[] baseline) {
        int n = proposed.length;
        List<Double> diffs = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            double d = baseline[i] - proposed[i];
            if (Math.abs(d) > 1e-9) {
                diffs.add(d);
            }
        }

        int N = diffs.size();
        if (N < 5) {
            return 1.0;
        }

        // Rank absolute differences
        Integer[] order = new Integer[N];
        for (int i = 0; i < N; i++) order[i] = i;
        Arrays.sort(order, Comparator.comparingDouble(i -> Math.abs(diffs.get(i))));

        double[] ranks = new double[N];
        int i = 0;
        while (i < N) {
            int j = i;
            while (j < N - 1 && Math.abs(Math.abs(diffs.get(order[j])) - Math.abs(diffs.get(order[j + 1]))) < 1e-9) {
                j++;
            }
            double avgRank = (i + j + 2) / 2.0; // 1-based ranks
            for (int k = i; k <= j; k++) {
                ranks[order[k]] = avgRank;
            }
            i = j + 1;
        }

        double wPlus = 0;
        for (int k = 0; k < N; k++) {
            if (diffs.get(k) > 0) {
                wPlus += ranks[k];
            }
        }

        // Normal approximation for Wilcoxon signed-rank test
        double meanW = N * (N + 1.0) / 4.0;
        double varW = N * (N + 1.0) * (2.0 * N + 1.0) / 24.0;
        double z = (wPlus - meanW) / Math.sqrt(varW);

        // Approximate two-tailed p-value using standard normal CDF
        return 2.0 * (1.0 - normCdf(Math.abs(z)));
    }

    private static double normCdf(double z) {
        // Abramowitz and Stegun approximation
        double t = 1.0 / (1.0 + 0.2316419 * Math.abs(z));
        double poly = t * (0.319381530 + t * (-0.356563782 + t * (1.781477937 + t * (-1.821255978 + t * 1.330274429))));
        double pdf = (1.0 / Math.sqrt(2.0 * Math.PI)) * Math.exp(-0.5 * z * z);
        double cdf = 1.0 - pdf * poly;
        return z >= 0 ? cdf : 1.0 - cdf;
    }
}
