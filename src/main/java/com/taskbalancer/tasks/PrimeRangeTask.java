package com.taskbalancer.tasks;

import com.taskbalancer.common.Task;
import com.taskbalancer.common.Result;
import java.util.ArrayList;
import java.util.List;

/**
 * Tarea que encuentra todos los números primos en un rango.
 */
public class PrimeRangeTask implements TaskHandler {
    
    @Override
    public String getTaskType() {
        return "PRIME_RANGE";
    }
    
    @Override
    public Result execute(Task task) {
        long startTime = System.currentTimeMillis();
        Result result = new Result();
        result.setTaskId(task.getTaskId());
        
        try {
            Object startObj = task.getParameters().get("start");
            Object endObj = task.getParameters().get("end");
            
            if (startObj == null || endObj == null) {
                throw new IllegalArgumentException("Parámetros 'start' y 'end' requeridos");
            }
            
            long start = ((Number) startObj).longValue();
            long end = ((Number) endObj).longValue();
            
            List<Long> primes = findPrimesInRange(start, end);
            
            result.setSuccess(true);
            result.setData(primes);
            
        } catch (Exception e) {
            result.setSuccess(false);
            result.setError(e.getMessage());
        }
        
        result.setExecutionTimeMs(System.currentTimeMillis() - startTime);
        return result;
    }
    
    private List<Long> findPrimesInRange(long start, long end) {
        List<Long> primes = new ArrayList<>();
        
        for (long n = Math.max(2, start); n <= end; n++) {
            if (isPrime(n)) {
                primes.add(n);
            }
        }
        
        return primes;
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

