package com.taskbalancer.tasks;

import com.taskbalancer.common.Task;
import com.taskbalancer.common.Result;
import java.util.Random;

/**
 * Tarea que multiplica dos matrices cuadradas de tamaño NxN.
 */
public class MatrixMultTask implements TaskHandler {
    
    @Override
    public String getTaskType() {
        return "MATRIX_MULT";
    }
    
    @Override
    public Result execute(Task task) {
        long startTime = System.currentTimeMillis();
        Result result = new Result();
        result.setTaskId(task.getTaskId());
        
        try {
            Object sizeObj = task.getParameters().get("size");
            
            if (sizeObj == null) {
                throw new IllegalArgumentException("Parámetro 'size' requerido");
            }
            
            int size = ((Number) sizeObj).intValue();
            
            if (size > 500) {
                throw new IllegalArgumentException("Tamaño máximo: 500");
            }
            
            double[][] matrixA = generateRandomMatrix(size);
            double[][] matrixB = generateRandomMatrix(size);
            double[][] resultMatrix = multiplyMatrices(matrixA, matrixB);
            
            String summary = String.format("Matriz %dx%d multiplicada. Elemento [0][0] = %.2f", 
                                        size, size, resultMatrix[0][0]);
            
            result.setSuccess(true);
            result.setData(summary);
            
        } catch (Exception e) {
            result.setSuccess(false);
            result.setError(e.getMessage());
        }
        
        result.setExecutionTimeMs(System.currentTimeMillis() - startTime);
        return result;
    }
    
    private double[][] generateRandomMatrix(int size) {
        Random random = new Random();
        double[][] matrix = new double[size][size];
        
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                matrix[i][j] = random.nextDouble() * 100;
            }
        }
        
        return matrix;
    }
    
    private double[][] multiplyMatrices(double[][] a, double[][] b) {
        int size = a.length;
        double[][] result = new double[size][size];
        
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                result[i][j] = 0;
                for (int k = 0; k < size; k++) {
                    result[i][j] += a[i][k] * b[k][j];
                }
            }
        }
        
        return result;
    }
}

