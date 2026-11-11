package com.gwosa.algorithms;

import com.gwosa.simulator.CloudEnvironment;

import java.util.Collections;
import java.util.List;

// Round Robin: assigns tasks to VMs in cyclic modulo order
public class RoundRobin implements SchedulingAlgorithm {

    private final String name;
    private double executionTimeMs = 0;

    public RoundRobin() {
        this("Round Robin");
    }

    public RoundRobin(String name) {
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

        int[] mapping = new int[n];
        for (int i = 0; i < n; i++) {
            mapping[i] = i % m;
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
