package com.gwosa.model;

import java.util.Objects;

// Virtual machine hardware specifications
public class VMConfig {

    private final int id;
    private final long mips;              // processing speed in MIPS
    private final double costPerSecond;   // dollar cost per second of runtime
    private final int ram;                // memory in MB
    private final long bandwidth;          // bandwidth in Mbps
    private final int pes;                // number of CPU cores

    public VMConfig(int id, long mips, double costPerSecond, int ram, long bandwidth, int pes) {
        this.id = id;
        this.mips = mips;
        this.costPerSecond = costPerSecond;
        this.ram = ram;
        this.bandwidth = bandwidth;
        this.pes = pes;
    }

    public int getId() { return id; }
    public long getMips() { return mips; }
    public double getCostPerSecond() { return costPerSecond; }
    public int getRam() { return ram; }
    public long getBandwidth() { return bandwidth; }
    public int getPes() { return pes; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VMConfig vmConfig = (VMConfig) o;
        return id == vmConfig.id &&
                mips == vmConfig.mips &&
                Double.compare(vmConfig.costPerSecond, costPerSecond) == 0 &&
                ram == vmConfig.ram &&
                bandwidth == vmConfig.bandwidth &&
                pes == vmConfig.pes;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, mips, costPerSecond, ram, bandwidth, pes);
    }

    @Override
    public String toString() {
        return "VMConfig{" +
                "id=" + id +
                ", mips=" + mips +
                ", costPerSecond=" + costPerSecond +
                ", ram=" + ram +
                ", bandwidth=" + bandwidth +
                ", pes=" + pes +
                '}';
    }
}
