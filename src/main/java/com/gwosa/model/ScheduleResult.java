package com.gwosa.model;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

// Encapsulates scheduling evaluation metrics (makespan, cost, DOI, composite fitness)
public class ScheduleResult {

    private final double makespan;
    private final double cost;
    private final double degreeOfImbalance;
    private final double[] completionTimePerVM;
    private final double fitness;

    public ScheduleResult(double makespan, double cost, double degreeOfImbalance,
                          double[] completionTimePerVM, double fitness) {
        this.makespan = makespan;
        this.cost = cost;
        this.degreeOfImbalance = degreeOfImbalance;
        this.completionTimePerVM = (completionTimePerVM != null) ? completionTimePerVM.clone() : new double[0];
        this.fitness = fitness;
    }

    // Evaluates a task-to-VM assignment mapping
    public static ScheduleResult evaluate(int[] mapping, List<TaskItem> tasks, List<VMConfig> vms,
                                          double w1, double w2, double w3) {
        Objects.requireNonNull(mapping, "mapping must not be null");
        Objects.requireNonNull(tasks, "tasks must not be null");
        Objects.requireNonNull(vms, "vms must not be null");
        if (vms.isEmpty()) {
            throw new IllegalArgumentException("vms list must not be empty");
        }

        int numVMs = vms.size();
        double[] ct = new double[numVMs];

        // Total execution time for each VM
        for (int i = 0; i < mapping.length; i++) {
            int vmIdx = mapping[i];
            double execTime = (double) tasks.get(i).getWorkload() / vms.get(vmIdx).getMips();
            ct[vmIdx] += execTime;
        }

        // Makespan = max completion time
        double makespan = 0.0;
        double minCT = Double.MAX_VALUE;
        double sumCT = 0.0;
        for (double c : ct) {
            makespan = Math.max(makespan, c);
            minCT = Math.min(minCT, c);
            sumCT += c;
        }

        // Total cost across active VMs
        double cost = 0.0;
        for (int j = 0; j < numVMs; j++) {
            cost += vms.get(j).getCostPerSecond() * ct[j];
        }

        // Degree of Imbalance: (maxCT - minCT) / avgCT
        double avgCT = sumCT / numVMs;
        double doi = (avgCT > 0.0) ? (makespan - minCT) / avgCT : 0.0;

        // Normalized fitness calculation to balance different scales
        long totalWorkload = 0;
        long maxMips = 0;
        for (TaskItem t : tasks) totalWorkload += t.getWorkload();
        for (VMConfig v : vms) maxMips = Math.max(maxMips, v.getMips());
        double refMakespan = (double) totalWorkload / (maxMips * numVMs);
        if (refMakespan == 0) refMakespan = 1;

        double avgCostRate = 0;
        for (VMConfig v : vms) avgCostRate += v.getCostPerSecond();
        avgCostRate /= numVMs;
        double refCost = avgCostRate * refMakespan * numVMs;
        if (refCost == 0) refCost = 1;

        double refDoI = 2.0;

        double normMakespan = makespan / refMakespan;
        double normCost = cost / refCost;
        double normDoI = doi / refDoI;

        double fitness = w1 * normMakespan + w2 * normCost + w3 * normDoI;

        return new ScheduleResult(makespan, cost, doi, ct, fitness);
    }

    public double getMakespan() { return makespan; }
    public double getCost() { return cost; }
    public double getDegreeOfImbalance() { return degreeOfImbalance; }
    public double[] getCompletionTimePerVM() { return completionTimePerVM.clone(); }
    public double getFitness() { return fitness; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScheduleResult that = (ScheduleResult) o;
        return Double.compare(that.makespan, makespan) == 0 &&
                Double.compare(that.cost, cost) == 0 &&
                Double.compare(that.degreeOfImbalance, degreeOfImbalance) == 0 &&
                Double.compare(that.fitness, fitness) == 0 &&
                Arrays.equals(completionTimePerVM, that.completionTimePerVM);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(makespan, cost, degreeOfImbalance, fitness);
        result = 31 * result + Arrays.hashCode(completionTimePerVM);
        return result;
    }

    @Override
    public String toString() {
        return String.format(Locale.US,
                "ScheduleResult[makespan=%.4f s, cost=$%.4f, doi=%.4f, fitness=%.4f]",
                makespan, cost, degreeOfImbalance, fitness);
    }
}
