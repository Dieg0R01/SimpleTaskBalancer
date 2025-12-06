package com.taskbalancer.tasks;

import com.taskbalancer.common.Task;
import com.taskbalancer.common.Result;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Tarea que genera números aleatorios y los ordena.
 */
public class SortRandomTask implements TaskHandler {
    
    @Override
    public String getTaskType() {
        return "SORT_RANDOM";
    }
    
    @Override
    public Result execute(Task task) {
        long startTime = System.currentTimeMillis();
        Result result = new Result();
        result.setTaskId(task.getTaskId());
        
        try {
            Object countObj = task.getParameters().get("count");
            
            if (countObj == null) {
                throw new IllegalArgumentException("Parámetro 'count' requerido");
            }
            
            int count = ((Number) countObj).intValue();
            List<Integer> sorted = generateAndSort(count);
            
            result.setSuccess(true);
            result.setData("Ordenados " + count + " números. Primeros 10: " + 
                        sorted.subList(0, Math.min(10, sorted.size())));
            
        } catch (Exception e) {
            result.setSuccess(false);
            result.setError(e.getMessage());
        }
        
        result.setExecutionTimeMs(System.currentTimeMillis() - startTime);
        return result;
    }
    
    private List<Integer> generateAndSort(int count) {
        Random random = new Random();
        List<Integer> numbers = new ArrayList<>(count);
        
        for (int i = 0; i < count; i++) {
            numbers.add(random.nextInt(1000000));
        }
        
        Collections.sort(numbers);
        
        return numbers;
    }
}

