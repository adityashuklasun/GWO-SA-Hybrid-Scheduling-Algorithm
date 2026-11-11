package com.gwosa.model;

import java.util.Objects;

// Represents a cloud task / cloudlet
public class TaskItem {

    private final int id;
    private final long workload;   // in Million Instructions (MI)
    private final long fileSize;   // in MB
    private final long outputSize; // in MB

    public TaskItem(int id, long workload) {
        this(id, workload, 300L, 300L);
    }

    public TaskItem(int id, long workload, long fileSize, long outputSize) {
        this.id = id;
        this.workload = workload;
        this.fileSize = fileSize;
        this.outputSize = outputSize;
    }

    public int getId() { return id; }
    public long getWorkload() { return workload; }
    public long getFileSize() { return fileSize; }
    public long getOutputSize() { return outputSize; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TaskItem taskItem = (TaskItem) o;
        return id == taskItem.id &&
                workload == taskItem.workload &&
                fileSize == taskItem.fileSize &&
                outputSize == taskItem.outputSize;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, workload, fileSize, outputSize);
    }

    @Override
    public String toString() {
        return "TaskItem{" +
                "id=" + id +
                ", workload=" + workload +
                ", fileSize=" + fileSize +
                ", outputSize=" + outputSize +
                '}';
    }
}
