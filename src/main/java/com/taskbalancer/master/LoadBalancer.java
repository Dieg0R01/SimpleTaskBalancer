package com.taskbalancer.master;

import java.util.List;

/**
 * Algoritmo de balanceo de carga para seleccionar el worker más apropiado.
 * 
 * Se podría haber tomado una solución más elegante con un patrón de diseño como el Strategy Pattern.
 * Pero teniendo en cuenta solo dos estrategias, se opta por una solución más simple.
 * 
 *  public interface LoadBalancingStrategy {
 *      WorkerInfo selectWorker(List<WorkerInfo> availableWorkers);
 *  }
 * 
 */
public class LoadBalancer {
    
    public enum Strategy {
        LEAST_LOADED,
        ROUND_ROBIN
    }
    
    private final Strategy strategy;
    private int roundRobinIndex = 0;
    
    public LoadBalancer(Strategy strategy) {
        this.strategy = strategy;
    }
    
    public synchronized WorkerInfo selectWorker(List<WorkerInfo> availableWorkers) {
        if (availableWorkers == null || availableWorkers.isEmpty()) {
            return null;
        }
        
        switch (strategy) {
            case LEAST_LOADED:
                return selectLeastLoaded(availableWorkers);
            
            case ROUND_ROBIN:
                return selectRoundRobin(availableWorkers);
            
            default:
                return selectLeastLoaded(availableWorkers);
        }
    }
    
    private WorkerInfo selectLeastLoaded(List<WorkerInfo> workers) {
        WorkerInfo bestWorker = null;
        double minLoad = Double.MAX_VALUE;
        
        for (WorkerInfo worker : workers) {
            double load = worker.getLoadRatio();
            if (load < minLoad) {
                minLoad = load;
                bestWorker = worker;
            }
        }
        
        return bestWorker;
    }
    
    private WorkerInfo selectRoundRobin(List<WorkerInfo> workers) {
        if (roundRobinIndex >= workers.size()) {
            roundRobinIndex = 0;
        }
        
        WorkerInfo worker = workers.get(roundRobinIndex);
        roundRobinIndex = (roundRobinIndex + 1) % workers.size();
        
        return worker;
    }
}

