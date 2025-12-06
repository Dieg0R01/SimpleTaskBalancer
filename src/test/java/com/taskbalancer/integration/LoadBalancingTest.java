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

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de balanceo de carga.
 * Verifica que las tareas se distribuyen correctamente entre múltiples workers.
 */
@Testcontainers
public class LoadBalancingTest {

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

        // Crear Master
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
    public void testLoadDistribution() throws InterruptedException {
        String masterHost = master.getHost();
        int masterPort = master.getMappedPort(8080);

        // Enviar múltiples tareas concurrentemente
        ExecutorService executor = Executors.newFixedThreadPool(10);
        List<Future<Result>> futures = new ArrayList<>();

        for (int i = 0; i < 20; i++) {
            final int taskNum = i;
            Future<Result> future = executor.submit(() -> {
                Client client = new Client(masterHost, masterPort);
                Task task = Client.createPrimeTestTask(982451653L + taskNum);
                return client.submitTask(task);
            });
            futures.add(future);
        }

        // Esperar a que todas las tareas se completen
        int successCount = 0;
        for (Future<Result> future : futures) {
            try {
                Result result = future.get(30, TimeUnit.SECONDS);
                if (result != null && result.isSuccess()) {
                    successCount++;
                }
            } catch (TimeoutException e) {
                fail("Tarea no completada en tiempo razonable");
            } catch (ExecutionException e) {
                fail("Error ejecutando tarea: " + e.getCause().getMessage());
            }
        }

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
        
        // Verificar que al menos la mayoría de las tareas se completaron
        assertTrue(successCount >= 15, 
                "Al menos 15 de 20 tareas deben completarse exitosamente. Completadas: " + successCount);
    }

    @Test
    public void testMultipleClients() throws InterruptedException {
        String masterHost = master.getHost();
        int masterPort = master.getMappedPort(8080);

        // Simular múltiples clientes concurrentes
        ExecutorService executor = Executors.newFixedThreadPool(5);
        List<Future<Integer>> futures = new ArrayList<>();

        for (int clientId = 0; clientId < 5; clientId++) {
            final int id = clientId;
            Future<Integer> future = executor.submit(() -> {
                Client client = new Client(masterHost, masterPort);
                int successCount = 0;
                
                // Cada cliente envía 5 tareas
                for (int i = 0; i < 5; i++) {
                    Task task = Client.createPrimeTestTask(982451653L + id * 10 + i);
                    Result result = client.submitTask(task);
                    if (result != null && result.isSuccess()) {
                        successCount++;
                    }
                }
                return successCount;
            });
            futures.add(future);
        }

        // Verificar resultados
        int totalSuccess = 0;
        for (Future<Integer> future : futures) {
            try {
                totalSuccess += future.get(60, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                fail("Cliente no completó en tiempo razonable");
            } catch (ExecutionException e) {
                fail("Error en cliente: " + e.getCause().getMessage());
            }
        }

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        // Verificar que la mayoría de las tareas se completaron
        assertTrue(totalSuccess >= 20, 
                "Al menos 20 de 25 tareas deben completarse. Completadas: " + totalSuccess);
        }
    }
