package com.taskbalancer.tasks;

import com.taskbalancer.common.Task;
import com.taskbalancer.common.Result;

/**
 * Tarea que verifica si un número es primo.
 */
public class PrimeTestTask implements TaskHandler {
    
    @Override
    public String getTaskType() {
        return "PRIME_TEST";
    }
    
    @Override
    public Result execute(Task task) {
        long startTime = System.currentTimeMillis();
        Result result = new Result();
        result.setTaskId(task.getTaskId());
        
        try {
            Object numberObj = task.getParameters().get("number");
            long number;
            
            if (numberObj instanceof Number) {
                number = ((Number) numberObj).longValue();
            } else {
                throw new IllegalArgumentException("Parámetro 'number' requerido");
            }
            
            boolean isPrime = isPrime(number);
            
            result.setSuccess(true);
            result.setData(isPrime);
            
        } catch (Exception e) {
            result.setSuccess(false);
            result.setError(e.getMessage());
        }
        
        result.setExecutionTimeMs(System.currentTimeMillis() - startTime);
        return result;
    }
    
    private boolean isPrime(long n) {
        if (n <= 1) return false;
        if (n <= 3) return true;
        if (n % 2 == 0 || n % 3 == 0) return false;
        
        for (long i = 5; i * i <= n; i += 6) {
            if (n % i == 0 || n % (i + 2) == 0) {
                return false;
            }
        }
        
        return true;
    }
}

