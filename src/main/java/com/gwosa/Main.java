package com.gwosa;

import com.gwosa.algorithms.FCFS;
import com.gwosa.algorithms.HybridDECSO;
import com.gwosa.algorithms.HybridGWOSA;
import com.gwosa.algorithms.HybridPSOGWO;
import com.gwosa.algorithms.HybridWOAGS;
import com.gwosa.algorithms.MinMin;
import com.gwosa.algorithms.PureGWO;
import com.gwosa.algorithms.PurePSO;
import com.gwosa.algorithms.PureSA;
import com.gwosa.algorithms.RoundRobin;
import com.gwosa.algorithms.SchedulingAlgorithm;
import com.gwosa.model.ScheduleResult;
import com.gwosa.results.ResultCollector;
import com.gwosa.simulator.CloudEnvironment;

import java.io.File;
import java.util.List;
import java.util.Locale;

// Main entry point: runs all 10 algorithms across 4 scenarios
public class Main {
    public static void main(String[] args) throws Exception {
        System.out.println("================================================================");
        System.out.println("  Hybrid GWO-SA for Multi-Objective Cloud Task Scheduling");
        System.out.println("================================================================");

        // Create all scenarios
        List<CloudEnvironment> scenarios = CloudEnvironment.allScenarios();

        // Create all algorithms (Heuristics + Pure Metaheuristics + 2025 Hybrids + Proposed GWO-SA)
        List<SchedulingAlgorithm> algorithms = List.of(
                new RoundRobin(),
                new FCFS(),
                new MinMin(),
                new PureGWO(),
                new PureSA(),
                new PurePSO(),
                new HybridWOAGS(),
                new HybridPSOGWO(),
                new HybridDECSO(),
                new HybridGWOSA()
        );

        ResultCollector collector = new ResultCollector();

        // Run each algorithm on each scenario
        for (CloudEnvironment env : scenarios) {
            System.out.printf(Locale.US, "%nRunning scenario: %s (%d tasks, %d VMs)%n",
                    env.getScenarioName(), env.getNumTasks(), env.getNumVMs());
            System.out.println("-".repeat(50));

            for (SchedulingAlgorithm algo : algorithms) {
                System.out.printf(Locale.US, "  Running %-20s ... ", algo.getName());

                int[] mapping = algo.schedule(env);
                ScheduleResult result = env.evaluateSchedule(mapping);

                collector.addResult(env.getScenarioName(), algo, result);

                System.out.printf(Locale.US, "Makespan=%.2f, Cost=%.2f, DoI=%.4f (%.0fms)%n",
                        result.getMakespan(), result.getCost(), result.getDegreeOfImbalance(),
                        algo.getExecutionTimeMs());
            }
        }

        // Print comparison tables
        collector.printResults();

        // Print improvement percentages
        collector.printImprovementTable();

        // Export to CSV
        String resultsDir = "results";
        new File(resultsDir).mkdirs();
        collector.exportToCSV(resultsDir + "/comparison_results.csv");
        collector.exportConvergenceCSV(resultsDir + "/convergence.csv");

        System.out.println("\nDone! All results saved to results/ directory.");
    }
}
