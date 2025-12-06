package com.taskbalancer.tasks;

import com.taskbalancer.common.Task;
import com.taskbalancer.common.Result;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Tarea que aplica funciones hash repetidamente a una cadena.
 */
public class HashStressTask implements TaskHandler {
    
    @Override
    public String getTaskType() {
        return "HASH_STRESS";
    }
    
    @Override
    public Result execute(Task task) {
        long startTime = System.currentTimeMillis();
        Result result = new Result();
        result.setTaskId(task.getTaskId());
        
        try {
            String input = (String) task.getParameters().get("input");
            Object iterObj = task.getParameters().get("iterations");
            
            if (input == null || iterObj == null) {
                throw new IllegalArgumentException("Parámetros 'input' e 'iterations' requeridos");
            }
            
            int iterations = ((Number) iterObj).intValue();
            String hashed = applyHashMultipleTimes(input, iterations);
            
            result.setSuccess(true);
            result.setData(hashed);
            
        } catch (Exception e) {
            result.setSuccess(false);
            result.setError(e.getMessage());
        }
        
        result.setExecutionTimeMs(System.currentTimeMillis() - startTime);
        return result;
    }
    
    private String applyHashMultipleTimes(String input, int iterations) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        String current = input;
        
        for (int i = 0; i < iterations; i++) {
            byte[] hash = md.digest(current.getBytes());
            current = bytesToHex(hash);
        }
        
        return current;
    }
    
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}

