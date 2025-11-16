package com.taskbalancer.worker;

import com.taskbalancer.common.*;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Worker que se conecta al Master y ejecuta tareas en paralelo.
 */
public class Worker {
    
    private final String workerId;
    private final String masterHost;
    private final int masterPort;
    private final int maxConcurrentTasks;
    
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    
    private final ExecutorService taskExecutor;
    private final ScheduledExecutorService heartbeatExecutor;
    private final TaskExecutor executor;
    
    private final AtomicInteger currentTasks;
    private final AtomicBoolean running;
    
    public Worker(String workerId, String masterHost, int masterPort, int maxConcurrentTasks) {
        this.workerId = workerId;
        this.masterHost = masterHost;
        this.masterPort = masterPort;
        this.maxConcurrentTasks = maxConcurrentTasks;
        
        this.taskExecutor = Executors.newFixedThreadPool(maxConcurrentTasks);
        this.heartbeatExecutor = Executors.newScheduledThreadPool(1);
        this.executor = new TaskExecutor();
        
        this.currentTasks = new AtomicInteger(0);
        this.running = new AtomicBoolean(false);
    }
    
    public void start() {
        try {
            connectToMaster();
            registerWithMaster();
            running.set(true);
            
            startHeartbeat();
            
            System.out.println("[Worker " + workerId + "] Conectado al Master en " + 
                             masterHost + ":" + masterPort);
            
            listenForTasks();
            
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("[Worker " + workerId + "] Error: " + e.getMessage());
            shutdown();
        }
    }
    
    private void connectToMaster() throws IOException {
        socket = new Socket(masterHost, masterPort);
        out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        in = new ObjectInputStream(socket.getInputStream());
    }
    
    private void registerWithMaster() throws IOException {
        Map<String, Object> registrationData = new HashMap<>();
        registrationData.put("workerId", workerId);
        registrationData.put("maxTasks", maxConcurrentTasks);
        registrationData.put("currentTasks", 0);
        
        Message registerMsg = new Message("WORKER_REGISTER", registrationData);
        sendMessage(registerMsg);
        
        System.out.println("[Worker " + workerId + "] Registro enviado al Master");
    }
    
    private void startHeartbeat() {
        heartbeatExecutor.scheduleAtFixedRate(() -> {
            try {
                Map<String, Object> heartbeatData = new HashMap<>();
                heartbeatData.put("workerId", workerId);
                heartbeatData.put("currentTasks", currentTasks.get());
                heartbeatData.put("maxTasks", maxConcurrentTasks);
                
                Message heartbeat = new Message("HEARTBEAT", heartbeatData);
                sendMessage(heartbeat);
                
            } catch (Exception e) {
                System.err.println("[Worker " + workerId + "] Error enviando heartbeat: " + 
                                 e.getMessage());
            }
        }, 5, 5, TimeUnit.SECONDS);
    }
    
    private void listenForTasks() throws IOException, ClassNotFoundException {
        try {
            while (running.get()) {
                Object obj = in.readObject();
                if (obj instanceof Message) {
                    processMessage((Message) obj);
                }
            }
        } catch (EOFException e) {
            if (running.get()) {
                System.err.println("[Worker " + workerId + "] Conexión cerrada por el Master");
            }
        } finally {
            shutdown();
        }
    }
    
    private void processMessage(Message message) {
        try {
            String type = message.getType();
            
            if ("TASK".equals(type)) {
                handleTaskMessage(message);
            } else if ("SHUTDOWN".equals(type)) {
                System.out.println("[Worker " + workerId + "] Recibido comando de apagado");
                shutdown();
            }
            
        } catch (Exception e) {
            System.err.println("[Worker " + workerId + "] Error procesando mensaje: " + 
                             e.getMessage());
        }
    }
    
    private void handleTaskMessage(Message message) {
        try {
            Task task = (Task) message.getPayload();
            
            System.out.println("[Worker " + workerId + "] Tarea recibida: " + task.getTaskId() + 
                             " (" + task.getTaskType() + ")");
            
            currentTasks.incrementAndGet();
            
            taskExecutor.submit(() -> {
                try {
                    Result result = executor.executeTask(task);
                    result.setWorkerId(workerId);
                    
                    System.out.println("[Worker " + workerId + "] Tarea completada: " + 
                                     task.getTaskId() + " en " + result.getExecutionTimeMs() + "ms");
                    
                    sendResult(result);
                    
                } catch (Exception e) {
                    System.err.println("[Worker " + workerId + "] Error ejecutando tarea: " + 
                                     e.getMessage());
                    
                    Result errorResult = new Result();
                    errorResult.setTaskId(task.getTaskId());
                    errorResult.setSuccess(false);
                    errorResult.setError(e.getMessage());
                    errorResult.setWorkerId(workerId);
                    
                    sendResult(errorResult);
                    
                } finally {
                    currentTasks.decrementAndGet();
                }
            });
            
        } catch (Exception e) {
            System.err.println("[Worker " + workerId + "] Error manejando tarea: " + e.getMessage());
        }
    }
    
    private void sendResult(Result result) {
        Message resultMessage = new Message("RESULT", result);
        sendMessage(resultMessage);
    }
    
    private synchronized void sendMessage(Message message) {
        if (out != null) {
            try {
                out.writeObject(message);
                out.flush();
                // Solo reset si es necesario, puede causar problemas
                if (!"HEARTBEAT".equals(message.getType())) {
                    out.reset();
                }
            } catch (IOException e) {
                System.err.println("[Worker " + workerId + "] Error enviando mensaje: " + 
                                 e.getMessage());
                // Si falla el envío, intentar reconectar
                running.set(false);
            }
        }
    }
    
    public void shutdown() {
        if (!running.getAndSet(false)) {
            return;
        }
        
        System.out.println("[Worker " + workerId + "] Apagando...");
        
        heartbeatExecutor.shutdown();
        taskExecutor.shutdown();
        
        try {
            if (!taskExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                taskExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            taskExecutor.shutdownNow();
        }
        
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.err.println("[Worker " + workerId + "] Error cerrando conexión: " + 
                             e.getMessage());
        }
        
        System.out.println("[Worker " + workerId + "] Apagado completado");
    }
    
    public static void main(String[] args) {
        String workerId = args.length > 0 ? args[0] : "worker-" + System.currentTimeMillis();
        String masterHost = args.length > 1 ? args[1] : "localhost";
        int masterPort = args.length > 2 ? Integer.parseInt(args[2]) : 8080;
        int maxTasks = args.length > 3 ? Integer.parseInt(args[3]) : 
                      Runtime.getRuntime().availableProcessors();
        
        Worker worker = new Worker(workerId, masterHost, masterPort, maxTasks);
        
        Runtime.getRuntime().addShutdownHook(new Thread(worker::shutdown));
        
        worker.start();
    }
}

