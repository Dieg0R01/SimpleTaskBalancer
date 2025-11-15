package com.taskbalancer.worker;

import com.taskbalancer.common.Task;
import com.taskbalancer.common.Result;
import com.taskbalancer.tasks.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Ejecutor de tareas que delega a los handlers específicos.
 */
public class TaskExecutor {
    
    private final Map<String, TaskHandler> handlers;
    
    public TaskExecutor() {
        handlers = new HashMap<>();
        registerHandlers();
    }
    
    private void registerHandlers() {
        registerHandler(new PrimeTestTask());
        registerHandler(new PrimeRangeTask());
        registerHandler(new FactorizeTask());
        registerHandler(new HashStressTask());
        registerHandler(new SortRandomTask());
        registerHandler(new PiEstimationTask());
        registerHandler(new MatrixMultTask());
    }
    
    private void registerHandler(TaskHandler handler) {
        handlers.put(handler.getTaskType(), handler);
    }
    
    public Result executeTask(Task task) {
        TaskHandler handler = handlers.get(task.getTaskType());
        
        if (handler == null) {
            Result result = new Result();
            result.setTaskId(task.getTaskId());
            result.setSuccess(false);
            result.setError("Tipo de tarea desconocido: " + task.getTaskType());
            return result;
        }
        
        return handler.execute(task);
    }
    
    public boolean supportsTaskType(String taskType) {
        return handlers.containsKey(taskType);
    }
}

