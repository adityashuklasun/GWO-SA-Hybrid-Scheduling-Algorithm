package com.gwosa.algorithms;

import com.gwosa.simulator.CloudEnvironment;
import java.util.List;

// Common interface for cloud task scheduling algorithms
public interface SchedulingAlgorithm {

    String getName();

    // Generates task-to-VM assignment mapping: mapping[task_i] = vm_j
    int[] schedule(CloudEnvironment env);

    // Iterative fitness history (empty for deterministic heuristics)
    List<Double> getConvergenceCurve();

    double getExecutionTimeMs();
}
