package com.gwosa.results;

import com.gwosa.algorithms.SchedulingAlgorithm;
import com.gwosa.model.ScheduleResult;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// Collects, formats, and exports simulation results to console and CSV files
public class ResultCollector {

    // scenario -> algorithm -> ScheduleResult
    private final Map<String, Map<String, ScheduleResult>> results = new LinkedHashMap<>();
    private final Map<String, Map<String, Double>> executionTimes = new LinkedHashMap<>();
    private final Map<String, Map<String, List<Double>>> convergenceCurves = new LinkedHashMap<>();

    public ResultCollector() {
    }

    // Save metrics for an algorithm run
    public void addResult(String scenario, SchedulingAlgorithm algo, ScheduleResult result) {
        results.computeIfAbsent(scenario, k -> new LinkedHashMap<>()).put(algo.getName(), result);
        executionTimes.computeIfAbsent(scenario, k -> new LinkedHashMap<>()).put(algo.getName(), algo.getExecutionTimeMs());
        List<Double> curve = algo.getConvergenceCurve();
        convergenceCurves.computeIfAbsent(scenario, k -> new LinkedHashMap<>())
                .put(algo.getName(), (curve != null && !curve.isEmpty()) ? new ArrayList<>(curve) : Collections.emptyList());
    }

    // Print summary tables grouped by scenario
    public void printResults() {
        for (var entry : results.entrySet()) {
            String scenario = entry.getKey();
            System.out.println("\n" + "=".repeat(100));
            System.out.printf(Locale.US, "SCENARIO: %s%n", scenario);
            System.out.println("=".repeat(100));
            System.out.printf(Locale.US, "%-32s | %12s | %12s | %8s | %12s%n",
                    "Algorithm", "Makespan", "Cost", "DoI", "Time (ms)");
            System.out.println("-".repeat(88));

            Map<String, Double> scenarioTimes = executionTimes.get(scenario);
            for (var algoEntry : entry.getValue().entrySet()) {
                String algoName = algoEntry.getKey();
                ScheduleResult r = algoEntry.getValue();
                double time = (scenarioTimes != null && scenarioTimes.containsKey(algoName))
                        ? scenarioTimes.get(algoName) : 0.0;
                System.out.printf(Locale.US, "%-32s | %12.2f | %12.2f | %8.4f | %12.2f%n",
                        algoName, r.getMakespan(), r.getCost(), r.getDegreeOfImbalance(), time);
            }
        }
    }

    // Export all metrics to CSV
    public void exportToCSV(String filePath) throws IOException {
        File file = new File(filePath);
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            pw.println("Scenario,Algorithm,Makespan,Cost,DoI,Fitness,ExecutionTimeMs");
            for (var entry : results.entrySet()) {
                String scenario = entry.getKey();
                Map<String, Double> scenarioTimes = executionTimes.get(scenario);
                for (var algoEntry : entry.getValue().entrySet()) {
                    String algoName = algoEntry.getKey();
                    ScheduleResult r = algoEntry.getValue();
                    double time = (scenarioTimes != null && scenarioTimes.containsKey(algoName))
                            ? scenarioTimes.get(algoName) : 0.0;
                    pw.printf(Locale.US, "%s,%s,%.4f,%.4f,%.4f,%.4f,%.2f%n",
                            scenario, algoName, r.getMakespan(), r.getCost(), r.getDegreeOfImbalance(),
                            r.getFitness(), time);
                }
            }
        }
        System.out.println("\nResults exported to: " + filePath);
    }

    // Export iteration convergence curve history to CSV
    public void exportConvergenceCSV(String filePath) throws IOException {
        if (convergenceCurves.isEmpty()) {
            System.out.println("No convergence data available to export.");
            return;
        }

        File file = new File(filePath);
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        String firstScenario = convergenceCurves.keySet().iterator().next();
        Map<String, List<Double>> algoCurves = convergenceCurves.get(firstScenario);

        if (algoCurves == null || algoCurves.isEmpty()) {
            System.out.println("No convergence curves found in scenario: " + firstScenario);
            return;
        }

        List<String> algosWithCurves = new ArrayList<>();
        int maxIterations = 0;
        for (Map.Entry<String, List<Double>> entry : algoCurves.entrySet()) {
            List<Double> curve = entry.getValue();
            if (curve != null && !curve.isEmpty()) {
                algosWithCurves.add(entry.getKey());
                if (curve.size() > maxIterations) {
                    maxIterations = curve.size();
                }
            }
        }

        if (algosWithCurves.isEmpty() || maxIterations == 0) {
            System.out.println("No convergence iterations recorded to export.");
            return;
        }

        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            StringBuilder header = new StringBuilder("Iteration");
            for (String algoName : algosWithCurves) {
                header.append(",").append(algoName);
            }
            pw.println(header);

            for (int i = 0; i < maxIterations; i++) {
                StringBuilder row = new StringBuilder();
                row.append(i + 1);
                for (String algoName : algosWithCurves) {
                    List<Double> curve = algoCurves.get(algoName);
                    if (curve != null && i < curve.size()) {
                        row.append(String.format(Locale.US, ",%.4f", curve.get(i)));
                    } else if (curve != null && !curve.isEmpty()) {
                        row.append(String.format(Locale.US, ",%.4f", curve.get(curve.size() - 1)));
                    } else {
                        row.append(",");
                    }
                }
                pw.println(row);
            }
        }
        System.out.println("Convergence curves exported to: " + filePath + " (Scenario: " + firstScenario + ")");
    }

    // Print percentage improvement of Hybrid GWO-SA against each baseline
    public void printImprovementTable() {
        System.out.println("\n" + "=".repeat(100));
        System.out.println("IMPROVEMENT OF HYBRID GWO-SA OVER BASELINES");
        System.out.println("=".repeat(100));

        for (var entry : results.entrySet()) {
            String scenario = entry.getKey();
            var algoResults = entry.getValue();

            String hybridKey = null;
            if (algoResults.containsKey("Hybrid GWO-SA")) {
                hybridKey = "Hybrid GWO-SA";
            } else {
                for (String k : algoResults.keySet()) {
                    if (k.equalsIgnoreCase("Hybrid GWO-SA") || k.equalsIgnoreCase("HybridGWOSA") || k.toLowerCase().contains("hybrid")) {
                        hybridKey = k;
                        break;
                    }
                }
            }

            if (hybridKey == null) continue;

            ScheduleResult hybridResult = algoResults.get(hybridKey);
            if (hybridResult == null) continue;

            System.out.printf(Locale.US, "%n--- %s ---%n", scenario);
            System.out.printf(Locale.US, "%-32s | %15s | %15s | %15s%n", "vs Algorithm", "Makespan Imp %", "Cost Imp %", "DoI Imp %");
            System.out.println("-".repeat(84));

            for (var algoEntry : algoResults.entrySet()) {
                if (algoEntry.getKey().equals(hybridKey)) continue;

                ScheduleResult baseline = algoEntry.getValue();
                double baseMakespan = baseline.getMakespan() == 0.0 ? 1.0 : baseline.getMakespan();
                double baseCost = baseline.getCost() == 0.0 ? 1.0 : baseline.getCost();
                double baseDoi = baseline.getDegreeOfImbalance() == 0.0 ? 1.0 : baseline.getDegreeOfImbalance();

                double mkImp = (baseline.getMakespan() - hybridResult.getMakespan()) / baseMakespan * 100.0;
                double costImp = (baseline.getCost() - hybridResult.getCost()) / baseCost * 100.0;
                double doiImp = (baseline.getDegreeOfImbalance() - hybridResult.getDegreeOfImbalance()) / baseDoi * 100.0;

                System.out.printf(Locale.US, "%-32s | %+14.2f%% | %+14.2f%% | %+14.2f%%%n",
                        algoEntry.getKey(), mkImp, costImp, doiImp);
            }
        }
    }

    public Map<String, Map<String, ScheduleResult>> getResults() { return results; }
    public Map<String, Map<String, Double>> getExecutionTimes() { return executionTimes; }
    public Map<String, Map<String, List<Double>>> getConvergenceCurves() { return convergenceCurves; }
}
