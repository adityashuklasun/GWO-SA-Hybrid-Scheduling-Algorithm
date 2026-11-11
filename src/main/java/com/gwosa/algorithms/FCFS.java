package com.gwosa.algorithms;

import com.gwosa.model.TaskItem;
import com.gwosa.model.VMConfig;
import com.gwosa.simulator.CloudEnvironment;

import java.util.Collections;
import java.util.List;

// First Come First Served (FCFS): assigns tasks to earliest available VM
public class FCFS implements SchedulingAlgorithm {

    private final String name;
    private double executionTimeMs = 0;

    public FCFS() {
        this("FCFS");
    }

    public FCFS(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int[] schedule(CloudEnvironment env) {
        long startTime = System.nanoTime();
        int n = env.getNumTasks();
        int m = env.getNumVMs();

        List<TaskItem> tasks = env.getTasks();
        List<VMConfig> vms = env.getVms();

        int[] mapping = new int[n];
        double[] vmCompletionTime = new double[m];

        for (int i = 0; i < n; i++) {
            // Find VM that becomes free earliest
            int bestVm = 0;
            double minCT = vmCompletionTime[0];
            for (int j = 1; j < m; j++) {
                if (vmCompletionTime[j] < minCT) {
                    minCT = vmCompletionTime[j];
                    bestVm = j;
                }
            }

            mapping[i] = bestVm;
            double execTime = (double) tasks.get(i).getWorkload() / vms.get(bestVm).getMips();
            vmCompletionTime[bestVm] += execTime;
        }

        executionTimeMs = (System.nanoTime() - startTime) / 1_000_000.0;
        return mapping;
    }

    @Override
    public List<Double> getConvergenceCurve() {
        return Collections.emptyList();
    }

    @Override
    public double getExecutionTimeMs() {
        return executionTimeMs;
    }
}
