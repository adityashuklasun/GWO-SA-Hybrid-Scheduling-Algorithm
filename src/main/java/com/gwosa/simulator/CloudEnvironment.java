package com.gwosa.simulator;

import com.gwosa.model.ScheduleResult;
import com.gwosa.model.TaskItem;
import com.gwosa.model.VMConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;

// Cloud simulation environment containing tasks, VMs, and evaluation weights
public class CloudEnvironment {

    private final List<TaskItem> tasks;
    private final List<VMConfig> vms;
    private final String scenarioName;
    private final double w1; // weight makespan
    private final double w2; // weight cost
    private final double w3; // weight DOI

    // Default constructor: 50% makespan, 30% cost, 20% DOI
    public CloudEnvironment(String scenarioName, int numTasks, int numVMs, long seed) {
        this(scenarioName, numTasks, numVMs, seed, 0.5, 0.3, 0.2);
    }

    public CloudEnvironment(String scenarioName, int numTasks, int numVMs, long seed,
                            double w1, double w2, double w3) {
        this.scenarioName = Objects.requireNonNull(scenarioName, "scenarioName must not be null");
        this.w1 = w1;
        this.w2 = w2;
        this.w3 = w3;
        Random rng = new Random(seed);
        this.tasks = Collections.unmodifiableList(createTasks(numTasks, rng));
        this.vms = Collections.unmodifiableList(createVMs(numVMs, rng));
    }

    // Generate tasks with workload between 1,000 and 50,000 MI
    private List<TaskItem> createTasks(int n, Random rng) {
        List<TaskItem> list = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            long workload = 1000L + rng.nextLong(49001L);
            list.add(new TaskItem(i, workload));
        }
        return list;
    }

    // Generate heterogeneous VMs with tiered pricing
    private List<VMConfig> createVMs(int m, Random rng) {
        long[] mipsOptions = {250L, 500L, 750L, 1000L};
        double[] costOptions = {0.5, 1.5, 3.5, 6.0}; // faster VMs cost proportionally more
        int[] ramOptions = {512, 1024, 2048};
        List<VMConfig> list = new ArrayList<>(m);
        for (int j = 0; j < m; j++) {
            long mips = mipsOptions[j % mipsOptions.length];
            double costPerSec = costOptions[j % costOptions.length];
            int ram = ramOptions[j % ramOptions.length];
            list.add(new VMConfig(j, mips, costPerSec, ram, 1000L, 1));
        }
        return list;
    }

    // Evaluates a schedule mapping against this environment
    public ScheduleResult evaluateSchedule(int[] mapping) {
        return ScheduleResult.evaluate(mapping, tasks, vms, w1, w2, w3);
    }

    public List<TaskItem> getTasks() { return tasks; }
    public List<VMConfig> getVms() { return vms; }
    public List<VMConfig> getVMs() { return vms; }
    public String getScenarioName() { return scenarioName; }
    public int getNumTasks() { return tasks.size(); }
    public int getNumVMs() { return vms.size(); }
    public double getW1() { return w1; }
    public double getW2() { return w2; }
    public double getW3() { return w3; }

    // Standard benchmark test setups
    public static CloudEnvironment small() {
        return new CloudEnvironment("Small", 100, 10, 42L);
    }

    public static CloudEnvironment medium() {
        return new CloudEnvironment("Medium", 200, 20, 42L);
    }

    public static CloudEnvironment large() {
        return new CloudEnvironment("Large", 500, 30, 42L);
    }

    public static CloudEnvironment xlarge() {
        return new CloudEnvironment("XLarge", 1000, 50, 42L);
    }

    public static List<CloudEnvironment> allScenarios() {
        return List.of(small(), medium(), large(), xlarge());
    }

    @Override
    public String toString() {
        return "CloudEnvironment{" +
                "scenarioName='" + scenarioName + '\'' +
                ", numTasks=" + getNumTasks() +
                ", numVMs=" + getNumVMs() +
                ", w1=" + w1 +
                ", w2=" + w2 +
                ", w3=" + w3 +
                '}';
    }
}
