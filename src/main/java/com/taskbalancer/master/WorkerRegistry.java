package com.taskbalancer.master;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registro de workers conectados al Master.
 */
public class WorkerRegistry {
    
    private final Map<String, WorkerInfo> workers;
    
    public WorkerRegistry() {
        this.workers = new ConcurrentHashMap<>();
    }
    
    public void registerWorker(WorkerInfo worker) {
        workers.put(worker.getWorkerId(), worker);
        System.out.println("[Master] Worker registrado: " + worker);
    }
    
    public void unregisterWorker(String workerId) {
        WorkerInfo worker = workers.remove(workerId);
        if (worker != null) {
            System.out.println("[Master] Worker desregistrado: " + workerId);
        }
    }
    
    public WorkerInfo getWorker(String workerId) {
        return workers.get(workerId);
    }
    
    public List<WorkerInfo> getAllWorkers() {
        return new ArrayList<>(workers.values());
    }
    
    public List<WorkerInfo> getAvailableWorkers() {
        List<WorkerInfo> available = new ArrayList<>();
        for (WorkerInfo worker : workers.values()) {
            if (worker.isAvailable()) {
                available.add(worker);
            }
        }
        return available;
    }
    
    public int getWorkerCount() {
        return workers.size();
    }
    
    public int getActiveWorkerCount() {
        return (int) workers.values().stream()
                .filter(WorkerInfo::isActive)
                .count();
    }
    
    public void updateWorkerHeartbeat(String workerId, int currentTasks) {
        WorkerInfo worker = workers.get(workerId);
        if (worker != null) {
            worker.updateHeartbeat();
            worker.setCurrentTasks(currentTasks);
        }
    }
    
    public void checkTimeouts(long timeoutMs) {
        long now = System.currentTimeMillis();
        for (WorkerInfo worker : workers.values()) {
            if (worker.isActive() && (now - worker.getLastHeartbeat()) > timeoutMs) {
                worker.setActive(false);
                System.out.println("[Master] Worker timeout: " + worker.getWorkerId());
            }
        }
    }
    
    public void printStatus() {
        System.out.println("\n=== Estado de Workers ===");
        System.out.println("Total: " + getWorkerCount() + " | Activos: " + getActiveWorkerCount());
        
        for (WorkerInfo worker : getAllWorkers()) {
            System.out.println("  " + worker);
        }
        System.out.println("========================\n");
    }
}

