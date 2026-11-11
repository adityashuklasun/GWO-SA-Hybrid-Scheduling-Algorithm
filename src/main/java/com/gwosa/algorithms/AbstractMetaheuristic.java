package com.gwosa.algorithms;

import java.util.ArrayList;
import java.util.List;

// Base class for metaheuristics to track common metrics (seed, runtime, convergence)
public abstract class AbstractMetaheuristic implements SchedulingAlgorithm {

    protected final String name;
    protected final int seed;
    protected List<Double> convergenceCurve = new ArrayList<>();
    protected double executionTimeMs = 0;

    protected AbstractMetaheuristic(String name, int seed) {
        this.name = name;
        this.seed = seed;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public List<Double> getConvergenceCurve() {
        return convergenceCurve;
    }

    @Override
    public double getExecutionTimeMs() {
        return executionTimeMs;
    }

    // Convert continuous [0, 1) position to discrete VM index
    protected int[] discretize(double[] position, int numVMs) {
        int[] mapping = new int[position.length];
        for (int i = 0; i < position.length; i++) {
            mapping[i] = ((int) Math.floor(position[i] * numVMs)) % numVMs;
            if (mapping[i] < 0) {
                mapping[i] += numVMs;
            }
        }
        return mapping;
    }

    // Keep coordinates within bounds [0.0, 0.999]
    protected void clamp(double[] position) {
        for (int i = 0; i < position.length; i++) {
            if (position[i] < 0) {
                position[i] = 0.0;
            } else if (position[i] >= 1.0) {
                position[i] = 0.999;
            }
        }
    }
}
