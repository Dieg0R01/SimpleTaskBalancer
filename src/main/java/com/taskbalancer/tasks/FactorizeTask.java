package com.taskbalancer.tasks;

import com.taskbalancer.common.Task;
import com.taskbalancer.common.Result;
import java.util.ArrayList;
import java.util.List;

/**
 * Tarea que calcula los factores primos de un número.
 */
public class FactorizeTask implements TaskHandler {
    
    @Override
    public String getTaskType() {
        return "FACTORIZE";
    }
    
    @Override
    public Result execute(Task task) {
        long startTime = System.currentTimeMillis();
        Result result = new Result();
        result.setTaskId(task.getTaskId());
        
        try {
            Object numberObj = task.getParameters().get("number");
            
            if (numberObj == null) {
                throw new IllegalArgumentException("Parámetro 'number' requerido");
            }
            
            long number = ((Number) numberObj).longValue();
            List<Long> factors = factorize(number);
            
            result.setSuccess(true);
            result.setData(factors);
            
        } catch (Exception e) {
            result.setSuccess(false);
            result.setError(e.getMessage());
        }
        
        result.setExecutionTimeMs(System.currentTimeMillis() - startTime);
        return result;
    }
    
    private List<Long> factorize(long n) {
        List<Long> factors = new ArrayList<>();
        
        while (n % 2 == 0) {
            factors.add(2L);
            n /= 2;
        }
        
        for (long i = 3; i * i <= n; i += 2) {
            while (n % i == 0) {
                factors.add(i);
                n /= i;
            }
        }
        
        if (n > 2) {
            factors.add(n);
        }
        
        return factors;
    }
}

