package com.taskbalancer.master;

import java.io.ObjectOutputStream;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Información sobre un worker registrado.
 */
public class WorkerInfo {
    private final String workerId;
    private final int maxTasks;
    private final AtomicInteger currentTasks;
    private final ObjectOutputStream out;
    private long lastHeartbeat;
    private boolean active;
    
    public WorkerInfo(String workerId, int maxTasks, ObjectOutputStream out) {
        this.workerId = workerId;
        this.maxTasks = maxTasks;
        this.out = out;
        this.currentTasks = new AtomicInteger(0);
        this.lastHeartbeat = System.currentTimeMillis();
        this.active = true;
    }
    
    public String getWorkerId() {
        return workerId;
    }
    
    public int getMaxTasks() {
        return maxTasks;
    }
    
    public int getCurrentTasks() {
        return currentTasks.get();
    }
    
    public void setCurrentTasks(int tasks) {
        this.currentTasks.set(tasks);
    }
    
    public void incrementTasks() {
        this.currentTasks.incrementAndGet();
    }
    
    public void decrementTasks() {
        this.currentTasks.decrementAndGet();
    }
    
    public ObjectOutputStream getOutputStream() {
        return out;
    }
    
    public long getLastHeartbeat() {
        return lastHeartbeat;
    }
    
    public void updateHeartbeat() {
        this.lastHeartbeat = System.currentTimeMillis();
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void setActive(boolean active) {
        this.active = active;
    }
    
    public boolean isAvailable() {
        return active && currentTasks.get() < maxTasks;
    }
    
    public double getLoadRatio() {
        return maxTasks > 0 ? (double) currentTasks.get() / maxTasks : 1.0;
    }
    
    @Override
    public String toString() {
        return "WorkerInfo{" +
                "workerId='" + workerId + '\'' +
                ", currentTasks=" + currentTasks.get() +
                ", maxTasks=" + maxTasks +
                ", active=" + active +
                ", loadRatio=" + String.format("%.2f", getLoadRatio()) +
                '}';
    }
}

