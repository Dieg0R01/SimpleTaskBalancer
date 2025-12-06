package com.taskbalancer.tasks;

import com.taskbalancer.common.Task;
import com.taskbalancer.common.Result;
import java.util.Random;

/**
 * Tarea que estima el valor de PI usando el método Monte Carlo.
 */
public class PiEstimationTask implements TaskHandler {
    
    @Override
    public String getTaskType() {
        return "PI_ESTIMATION";
    }
    
    @Override
    public Result execute(Task task) {
        long startTime = System.currentTimeMillis();
        Result result = new Result();
        result.setTaskId(task.getTaskId());
        
        try {
            Object iterObj = task.getParameters().get("iterations");
            
            if (iterObj == null) {
                throw new IllegalArgumentException("Parámetro 'iterations' requerido");
            }
            
            int iterations = ((Number) iterObj).intValue();
            double piEstimate = estimatePi(iterations);
            
            result.setSuccess(true);
            result.setData(piEstimate);
            
        } catch (Exception e) {
            result.setSuccess(false);
            result.setError(e.getMessage());
        }
        
        result.setExecutionTimeMs(System.currentTimeMillis() - startTime);
        return result;
    }
    
    private double estimatePi(int iterations) {
        Random random = new Random();
        int insideCircle = 0;
        
        for (int i = 0; i < iterations; i++) {
            double x = random.nextDouble();
            double y = random.nextDouble();
            
            if (x * x + y * y <= 1.0) {
                insideCircle++;
            }
        }
        
        return 4.0 * insideCircle / iterations;
    }
}

