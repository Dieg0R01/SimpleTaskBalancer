package com.taskbalancer.client;

import com.taskbalancer.common.*;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Cliente que envía tareas al Master y espera resultados.
 */
public class Client {
    
    private final String masterHost;
    private final int masterPort;
    
    public Client(String masterHost, int masterPort) {
        this.masterHost = masterHost;
        this.masterPort = masterPort;
    }
    
    public Result submitTask(Task task) {
        try (Socket socket = new Socket(masterHost, masterPort)) {
            
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            
            Message taskMessage = new Message("TASK", task);
            out.writeObject(taskMessage);
            out.flush();
            
            System.out.println("[Cliente] Tarea enviada: " + task.getTaskId());
            
            Object ackObj = in.readObject();
            if (ackObj instanceof Message) {
                Message ackMsg = (Message) ackObj;
                System.out.println("[Cliente] ACK recibido: " + ackMsg.getPayload());
            }
            
            Object resultObj = in.readObject();
            if (resultObj instanceof Message) {
                Message resultMsg = (Message) resultObj;
                
                if ("RESULT".equals(resultMsg.getType())) {
                    Result result = (Result) resultMsg.getPayload();
                    System.out.println("[Cliente] Resultado recibido: " + result.getTaskId());
                    return result;
                }
            }
            
            Result errorResult = new Result();
            errorResult.setTaskId(task.getTaskId());
            errorResult.setSuccess(false);
            errorResult.setError("No se recibió resultado válido");
            return errorResult;
            
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("[Cliente] Error: " + e.getMessage());
            
            Result errorResult = new Result();
            errorResult.setTaskId(task.getTaskId());
            errorResult.setSuccess(false);
            errorResult.setError("Error de conexión: " + e.getMessage());
            return errorResult;
        }
    }
    
    public static Task createTask(String taskType, Map<String, Object> parameters) {
        String taskId = UUID.randomUUID().toString();
        return new Task(taskId, taskType, parameters);
    }
    
    public static Task createPrimeTestTask(long number) {
        Map<String, Object> params = new HashMap<>();
        params.put("number", number);
        return createTask("PRIME_TEST", params);
    }
    
    public static Task createPrimeRangeTask(long start, long end) {
        Map<String, Object> params = new HashMap<>();
        params.put("start", start);
        params.put("end", end);
        return createTask("PRIME_RANGE", params);
    }
    
    public static Task createFactorizeTask(long number) {
        Map<String, Object> params = new HashMap<>();
        params.put("number", number);
        return createTask("FACTORIZE", params);
    }
    
    public static Task createHashStressTask(String input, int iterations) {
        Map<String, Object> params = new HashMap<>();
        params.put("input", input);
        params.put("iterations", iterations);
        return createTask("HASH_STRESS", params);
    }
    
    public static Task createSortRandomTask(int count) {
        Map<String, Object> params = new HashMap<>();
        params.put("count", count);
        return createTask("SORT_RANDOM", params);
    }
    
    public static Task createPiEstimationTask(int iterations) {
        Map<String, Object> params = new HashMap<>();
        params.put("iterations", iterations);
        return createTask("PI_ESTIMATION", params);
    }
    
    public static Task createMatrixMultTask(int size) {
        Map<String, Object> params = new HashMap<>();
        params.put("size", size);
        return createTask("MATRIX_MULT", params);
    }
    
    public static void main(String[] args) {
        String masterHost = args.length > 0 ? args[0] : "localhost";
        int masterPort = args.length > 1 ? Integer.parseInt(args[1]) : 8080;
        
        Client client = new Client(masterHost, masterPort);
        
        System.out.println("\n=== SimpleTaskBalancer - Cliente de Demostración ===\n");
        
        System.out.println("--- Test de Primalidad ---");
        Task primeTest = createPrimeTestTask(982451653);
        Result result1 = client.submitTask(primeTest);
        System.out.println("Resultado: " + result1);
        System.out.println();
        
        System.out.println("--- Rango de Primos ---");
        Task primeRange = createPrimeRangeTask(1000, 1100);
        Result result2 = client.submitTask(primeRange);
        System.out.println("Resultado: " + result2);
        System.out.println();
        
        System.out.println("--- Factorización ---");
        Task factorize = createFactorizeTask(123456789);
        Result result3 = client.submitTask(factorize);
        System.out.println("Resultado: " + result3);
        System.out.println();
        
        System.out.println("--- Hash Stress Test ---");
        Task hashStress = createHashStressTask("SimpleTaskBalancer", 10000);
        Result result4 = client.submitTask(hashStress);
        System.out.println("Resultado: " + result4);
        System.out.println();
        
        System.out.println("--- Ordenamiento de Números Aleatorios ---");
        Task sortRandom = createSortRandomTask(1000000);
        Result result5 = client.submitTask(sortRandom);
        System.out.println("Resultado: " + result5);
        System.out.println();
        
        System.out.println("--- Estimación de Pi (Monte Carlo) ---");
        Task piEstimation = createPiEstimationTask(10000000);
        Result result6 = client.submitTask(piEstimation);
        System.out.println("Resultado: " + result6);
        System.out.println();
        
        System.out.println("--- Multiplicación de Matrices ---");
        Task matrixMult = createMatrixMultTask(200);
        Result result7 = client.submitTask(matrixMult);
        System.out.println("Resultado: " + result7);
        System.out.println();
        
        System.out.println("=== Todas las tareas completadas ===");
    }
}

