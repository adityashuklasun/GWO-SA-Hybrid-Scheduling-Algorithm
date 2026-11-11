package com.gwosa.algorithms;

import com.gwosa.model.TaskItem;
import com.gwosa.model.VMConfig;
import com.gwosa.simulator.CloudEnvironment;

import java.util.Collections;
import java.util.List;

// Min-Min heuristic: maps small tasks to fast VMs first
public class MinMin implements SchedulingAlgorithm {

    private final String name;
    private double executionTimeMs = 0;

    public MinMin() {
        this("Min-Min");
    }

    public MinMin(String name) {
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

        // Precompute execution times: et[i][j] = workload / mips
        double[][] et = new double[n][m];
        for (int i = 0; i < n; i++) {
            long workload = tasks.get(i).getWorkload();
            for (int j = 0; j < m; j++) {
                et[i][j] = (double) workload / vms.get(j).getMips();
            }
        }

        boolean[] assigned = new boolean[n];
        double[] currentCT = new double[m];
        int[] mapping = new int[n];

        // Greedily pick task-VM pair with minimum completion time
        for (int scheduledCount = 0; scheduledCount < n; scheduledCount++) {
            int bestTask = -1;
            int bestVm = -1;
            double minEarliestCT = Double.MAX_VALUE;

            for (int i = 0; i < n; i++) {
                if (assigned[i]) continue;

                for (int j = 0; j < m; j++) {
                    double completionTime = currentCT[j] + et[i][j];
                    if (completionTime < minEarliestCT) {
                        minEarliestCT = completionTime;
                        bestTask = i;
                        bestVm = j;
                    }
                }
            }

            if (bestTask != -1 && bestVm != -1) {
                mapping[bestTask] = bestVm;
                currentCT[bestVm] += et[bestTask][bestVm];
                assigned[bestTask] = true;
            }
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
