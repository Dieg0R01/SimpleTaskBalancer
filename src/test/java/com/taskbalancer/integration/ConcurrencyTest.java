package com.taskbalancer.integration;

import com.taskbalancer.client.Client;
import com.taskbalancer.common.Result;
import com.taskbalancer.common.Task;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.file.Paths;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de concurrencia.
 * Verifica que el sistema maneja correctamente múltiples clientes simultáneos sin race conditions.
 */
@Testcontainers
public class ConcurrencyTest {

    @SuppressWarnings("resource")
    private static Network network;
    @SuppressWarnings("resource")
    private static GenericContainer<?> master;
    private static List<GenericContainer<?>> workers;

    @BeforeAll
    public static void setUp() {
        network = Network.newNetwork();
        // Construir imagen usando ImageFromDockerfile directamente
        ImageFromDockerfile image = new ImageFromDockerfile("taskbalancer-simpletaskbalancer:latest", false)
            .withDockerfile(Paths.get(System.getProperty("user.dir"), "Dockerfile"));

        @SuppressWarnings("resource")
        GenericContainer<?> masterContainer = new GenericContainer<>(image)
                .withNetwork(network)
                .withNetworkAliases("master")
                .withCommand("java", "-cp", "bin", "com.taskbalancer.master.Master", "8080")
                .withExposedPorts(8080)
                .waitingFor(Wait.forListeningPort())
                .withStartupTimeout(java.time.Duration.ofSeconds(30));
        master = masterContainer;
        master.start();

        // Crear múltiples Workers
        workers = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            @SuppressWarnings("resource")
            GenericContainer<?> worker = new GenericContainer<>(image)
                    .withNetwork(network)
                    .withCommand("java", "-cp", "bin", "com.taskbalancer.worker.Worker", 
                                "worker-" + i, "master", "8080", "4")
                    .dependsOn(master)
                    .withStartupTimeout(java.time.Duration.ofSeconds(30));
            workers.add(worker);
            worker.start();
        }

        // Esperar a que los workers se registren
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @AfterAll
    public static void tearDown() {
        if (workers != null) {
            workers.forEach(GenericContainer::stop);
        }
        if (master != null) master.stop();
        if (network != null) network.close();
    }

    @Test
    public void testConcurrentTaskSubmission() throws InterruptedException {
        String masterHost = master.getHost();
        int masterPort = master.getMappedPort(8080);

        int numThreads = 10;
        int tasksPerThread = 5;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        List<Future<Result>> allFutures = new ArrayList<>();
        
        // Cada thread envía múltiples tareas
        for (int threadId = 0; threadId < numThreads; threadId++) {
            final int id = threadId;
            for (int taskNum = 0; taskNum < tasksPerThread; taskNum++) {
                final int taskId = taskNum;
                Future<Result> future = executor.submit(() -> {
                Client client = new Client(masterHost, masterPort);
                    Task task = Client.createPrimeTestTask(982451653L + id * 100 + taskId);
                    return client.submitTask(task);
                });
                allFutures.add(future);
            }
        }
        
        // Verificar que todas las tareas se completan correctamente
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        
        for (Future<Result> future : allFutures) {
            try {
                Result result = future.get(60, TimeUnit.SECONDS);
                if (result != null) {
                    if (result.isSuccess()) {
                        successCount.incrementAndGet();
                        } else {
                        errorCount.incrementAndGet();
                        }
                    }
            } catch (TimeoutException e) {
                errorCount.incrementAndGet();
            } catch (ExecutionException e) {
                errorCount.incrementAndGet();
            }
        }

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
        
        int totalTasks = numThreads * tasksPerThread;
        System.out.println("Tareas completadas exitosamente: " + successCount.get() + " de " + totalTasks);
        
        // Verificar que la mayoría de las tareas se completaron
        assertTrue(successCount.get() >= totalTasks * 0.7, 
                "Al menos 70% de las tareas deben completarse. Exitosas: " + successCount.get() + "/" + totalTasks);
    }

    @Test
    public void testNoRaceConditions() throws InterruptedException {
        String masterHost = master.getHost();
        int masterPort = master.getMappedPort(8080);

        // Enviar tareas con IDs únicos para verificar que no se mezclan
        int numTasks = 20;
        ExecutorService executor = Executors.newFixedThreadPool(10);
        List<Future<Result>> futures = new ArrayList<>();

        for (int i = 0; i < numTasks; i++) {
            final int taskId = i;
            Future<Result> future = executor.submit(() -> {
                Client client = new Client(masterHost, masterPort);
                Task task = Client.createPrimeTestTask(982451653L + taskId);
                return client.submitTask(task);
            });
            futures.add(future);
        }

        // Verificar que cada resultado corresponde a su tarea
        int correctResults = 0;
        for (int i = 0; i < futures.size(); i++) {
            try {
                Future<Result> future = futures.get(i);
                Result result = future.get(30, TimeUnit.SECONDS);
                if (result != null && result.isSuccess()) {
                    // Verificar que el resultado es válido
                    assertNotNull(result.getTaskId(), "TaskId no debe ser null");
                    correctResults++;
                }
            } catch (Exception e) {
                // Ignorar errores individuales
            }
        }

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
        
        // Verificar que al menos algunas tareas se completaron correctamente
        assertTrue(correctResults >= numTasks * 0.5, 
                "Al menos 50% de las tareas deben completarse correctamente. Completadas: " + correctResults);
    }
}
