package com.taskbalancer.master;

import com.taskbalancer.common.*;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Master/Balanceador que recibe tareas de clientes y las distribuye a workers.
 */
public class Master {
    
    private final int port;
    private ServerSocket serverSocket;
    
    private final WorkerRegistry workerRegistry;
    private final TaskQueue taskQueue;
    private final LoadBalancer loadBalancer;
    
    private final ExecutorService acceptorPool;
    private final ExecutorService assignerPool;
    private final ScheduledExecutorService monitorPool;
    
    private final Map<String, CompletableFuture<Result>> pendingResults;
    private final Map<String, ObjectOutputStream> clientStreams;
    private final AtomicBoolean running;
    
    private static final long WORKER_TIMEOUT_MS = 30000;
    private static final long MONITOR_INTERVAL_MS = 10000;
    
    public Master(int port) {
        this.port = port;
        this.workerRegistry = new WorkerRegistry();
        this.taskQueue = new TaskQueue();
        this.loadBalancer = new LoadBalancer(LoadBalancer.Strategy.LEAST_LOADED);
        
        this.acceptorPool = Executors.newCachedThreadPool();
        this.assignerPool = Executors.newFixedThreadPool(4);
        this.monitorPool = Executors.newScheduledThreadPool(1);
        
        this.pendingResults = new ConcurrentHashMap<>();
        this.clientStreams = new ConcurrentHashMap<>();
        this.running = new AtomicBoolean(false);
    }
    
    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            running.set(true);
            
            System.out.println("[Master] Iniciado en puerto " + port);
            
            startTaskAssigner();
            startMonitor();
            
            acceptConnections();
            
        } catch (IOException e) {
            System.err.println("[Master] Error: " + e.getMessage());
            shutdown();
        }
    }
    
    private void acceptConnections() {
        while (running.get()) {
            try {
                Socket clientSocket = serverSocket.accept();
                System.out.println("[Master] Nueva conexión desde " + 
                                    clientSocket.getInetAddress());
                
                acceptorPool.submit(() -> handleConnection(clientSocket));
                
            } catch (IOException e) {
                if (running.get()) {
                    System.err.println("[Master] Error aceptando conexión: " + e.getMessage());
                }
            }
        }
    }
    
    private void handleConnection(Socket socket) {
        try {
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            
            // Leer solo el PRIMER mensaje (registro o tarea de cliente)
            Object obj = in.readObject();
            if (obj instanceof Message) {
                Message message = (Message) obj;
                String type = message.getType();
                
                if ("WORKER_REGISTER".equals(type)) {
                    handleWorkerRegistration(message, in, out, socket);
                    // NO cerrar el socket, maintainWorkerConnection lo manejará
                    return;
                } else if ("TASK".equals(type)) {
                    handleClientTask(message, out);
                    // Mantener la conexión para enviar el resultado
                    return;
                }
            }
            
        } catch (EOFException e) {
            // Conexión cerrada normalmente
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("[Master] Error en conexión: " + e.getMessage());
        }
    }
    
    
    private void handleWorkerRegistration(Message message, ObjectInputStream in, 
                                         ObjectOutputStream out, Socket socket) {
        try {
            // Usamos esta anotación para suprimir el warning unchecked cast
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) message.getPayload();
            
            String workerId = (String) data.get("workerId");
            int maxTasks = ((Number) data.get("maxTasks")).intValue();
            
            WorkerInfo workerInfo = new WorkerInfo(workerId, maxTasks, out);
            workerRegistry.registerWorker(workerInfo);
            
            Message ack = new Message("ACK", "Worker registrado exitosamente");
            synchronized (out) {
                out.writeObject(ack);
                out.flush();
            }
            
            // Delegar la lectura continua a un thread dedicado
            Thread workerThread = new Thread(() -> maintainWorkerConnection(socket, workerId, in, out));
            workerThread.setName("Worker-" + workerId);
            workerThread.setDaemon(true);
            workerThread.start();
            
        } catch (Exception e) {
            System.err.println("[Master] Error registrando worker: " + e.getMessage());
        }
    }
    
    private void maintainWorkerConnection(Socket socket, String workerId, 
                                         ObjectInputStream in, ObjectOutputStream out) {
        try {
            while (true) {
                Object obj = in.readObject();
                if (obj instanceof Message) {
                    Message message = (Message) obj;
                    
                    if ("HEARTBEAT".equals(message.getType())) {
                        handleHeartbeat(message);
                    } else if ("RESULT".equals(message.getType())) {
                        handleResult(message);
                    }
                }
            }
        } catch (EOFException e) {
            System.err.println("[Master] Worker desconectado: " + workerId);
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("[Master] Conexión worker perdida: " + workerId);
        } finally {
            workerRegistry.unregisterWorker(workerId);
        }
    }
    
    private void handleHeartbeat(Message message) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) message.getPayload();
            
            String workerId = (String) data.get("workerId");
            int currentTasks = ((Number) data.get("currentTasks")).intValue();
            
            workerRegistry.updateWorkerHeartbeat(workerId, currentTasks);
            
        } catch (Exception e) {
            System.err.println("[Master] Error procesando heartbeat: " + e.getMessage());
        }
    }
    
    private void handleResult(Message message) {
        try {
            Result result = (Result) message.getPayload();
            
            System.out.println("[Master] Resultado recibido: " + result.getTaskId() + 
                             " de " + result.getWorkerId());
            
            CompletableFuture<Result> future = pendingResults.remove(result.getTaskId());
            if (future != null) {
                future.complete(result);
            }
            
        } catch (Exception e) {
            System.err.println("[Master] Error procesando resultado: " + e.getMessage());
        }
    }
    
    private void handleClientTask(Message message, ObjectOutputStream clientOut) {
        try {
            Task task = (Task) message.getPayload();
            
            System.out.println("[Master] Tarea recibida de cliente: " + task.getTaskId());
            
            CompletableFuture<Result> resultFuture = new CompletableFuture<>();
            pendingResults.put(task.getTaskId(), resultFuture);
            clientStreams.put(task.getTaskId(), clientOut);
            
            taskQueue.enqueue(task);
            
            Message ack = new Message("ACK", "Tarea recibida: " + task.getTaskId());
            synchronized (clientOut) {
                clientOut.writeObject(ack);
                clientOut.flush();
            }
            
            resultFuture.orTimeout(60, TimeUnit.SECONDS)
                       .thenAccept(result -> {
                           try {
                               Message resultMsg = new Message("RESULT", result);
                               synchronized (clientOut) {
                                   clientOut.writeObject(resultMsg);
                                   clientOut.flush();
                               }
                               clientStreams.remove(task.getTaskId());
                           } catch (IOException e) {
                               System.err.println("[Master] Error enviando resultado: " + 
                                                e.getMessage());
                           }
                       })
                       .exceptionally(ex -> {
                           try {
                               Result errorResult = new Result();
                               errorResult.setTaskId(task.getTaskId());
                               errorResult.setSuccess(false);
                               errorResult.setError("Timeout o error: " + ex.getMessage());
                               
                               Message errorMsg = new Message("RESULT", errorResult);
                               synchronized (clientOut) {
                                   clientOut.writeObject(errorMsg);
                                   clientOut.flush();
                               }
                               clientStreams.remove(task.getTaskId());
                           } catch (IOException e) {
                               System.err.println("[Master] Error enviando error: " + 
                                                e.getMessage());
                           }
                           return null;
                       });
            
        } catch (Exception e) {
            System.err.println("[Master] Error procesando tarea de cliente: " + e.getMessage());
            
            try {
                Message errorMsg = new Message("ERROR", e.getMessage());
                synchronized (clientOut) {
                    clientOut.writeObject(errorMsg);
                    clientOut.flush();
                }
            } catch (IOException ex) {
                System.err.println("[Master] Error enviando mensaje de error: " + 
                                 ex.getMessage());
            }
        }
    }
    
    private void startTaskAssigner() {
        for (int i = 0; i < 4; i++) {
            assignerPool.submit(() -> {
                while (running.get()) {
                    try {
                        Task task = taskQueue.dequeue(1, TimeUnit.SECONDS);
                        
                        if (task != null) {
                            assignTaskToWorker(task);
                        }
                        
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
        }
    }
    
    private void assignTaskToWorker(Task task) {
        int retries = 0;
        int maxRetries = 5;
        
        while (retries < maxRetries) {
            List<WorkerInfo> available = workerRegistry.getAvailableWorkers();
            
            if (available.isEmpty()) {
                System.out.println("[Master] No hay workers disponibles, esperando...");
                try {
                    Thread.sleep(2000);
                    retries++;
                    continue;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            
            WorkerInfo selectedWorker = loadBalancer.selectWorker(available);
            
            if (selectedWorker != null) {
                System.out.println("[Master] Asignando tarea " + task.getTaskId() + 
                                 " a " + selectedWorker.getWorkerId());
                
                try {
                    Message taskMessage = new Message("TASK", task);
                    synchronized (selectedWorker.getOutputStream()) {
                        selectedWorker.getOutputStream().writeObject(taskMessage);
                        selectedWorker.getOutputStream().flush();
                        selectedWorker.getOutputStream().reset();
                    }
                    selectedWorker.incrementTasks();
                    
                    return;
                } catch (IOException e) {
                    System.err.println("[Master] Error enviando tarea a worker: " + 
                                     e.getMessage());
                    workerRegistry.unregisterWorker(selectedWorker.getWorkerId());
                }
            }
            
            retries++;
        }
        
        System.err.println("[Master] No se pudo asignar tarea después de " + maxRetries + 
                         " intentos: " + task.getTaskId());
    }
    
    private void startMonitor() {
        monitorPool.scheduleAtFixedRate(() -> {
            try {
                workerRegistry.checkTimeouts(WORKER_TIMEOUT_MS);
                printMetrics();
            } catch (Exception e) {
                System.err.println("[Master] Error en monitor: " + e.getMessage());
            }
        }, MONITOR_INTERVAL_MS, MONITOR_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }
    
    private void printMetrics() {
        System.out.println("\n=== Métricas del Sistema ===");
        System.out.println("Workers activos: " + workerRegistry.getActiveWorkerCount() + 
                         "/" + workerRegistry.getWorkerCount());
        System.out.println("Tareas en cola: " + taskQueue.size());
        System.out.println("Resultados pendientes: " + pendingResults.size());
        workerRegistry.printStatus();
    }
    
    public void shutdown() {
        if (!running.getAndSet(false)) {
            return;
        }
        
        System.out.println("[Master] Apagando...");
        
        monitorPool.shutdown();
        assignerPool.shutdown();
        acceptorPool.shutdown();
        
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("[Master] Error cerrando servidor: " + e.getMessage());
        }
        
        System.out.println("[Master] Apagado completado");
    }
    
    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        
        Master master = new Master(port);
        
        // Añade un hook de apagado para garantizar la liberación limpia de recursos y evitar fugas en caso de cierre inesperado.
        Runtime.getRuntime().addShutdownHook(new Thread(master::shutdown));
        
        master.start();
    }
}

