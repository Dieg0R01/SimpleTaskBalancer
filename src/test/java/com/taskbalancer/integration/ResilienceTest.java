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
 * Tests de resiliencia del sistema.
 * Verifica que el sistema se recupera correctamente cuando un worker falla.
 */
@Testcontainers
public class ResilienceTest {

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
    public void testSystemContinuesAfterWorkerFailure() throws InterruptedException {
        String masterHost = master.getHost();
        int masterPort = master.getMappedPort(8080);

        // Enviar algunas tareas iniciales
        Client client = new Client(masterHost, masterPort);
        List<Task> initialTasks = new ArrayList<>();
        
        for (int i = 0; i < 5; i++) {
            Task task = Client.createPrimeTestTask(982451653L + i);
            initialTasks.add(task);
        }
        
        // Enviar tareas en paralelo
        ExecutorService executor = Executors.newFixedThreadPool(5);
        List<Future<Result>> initialFutures = new ArrayList<>();
        
        for (Task task : initialTasks) {
            Future<Result> future = executor.submit(() -> client.submitTask(task));
            initialFutures.add(future);
        }

        // Esperar un poco y luego detener un worker
        Thread.sleep(2000);

        if (!workers.isEmpty()) {
            GenericContainer<?> failedWorker = workers.get(0);
        failedWorker.stop();
            System.out.println("Worker detenido para simular fallo");
        }
        
        // Esperar a que las tareas iniciales se completen
        for (Future<Result> future : initialFutures) {
            try {
                future.get(30, TimeUnit.SECONDS);
            } catch (Exception e) {
                // Algunas tareas pueden fallar si estaban asignadas al worker que falló
            }
        }

        // Enviar más tareas después del fallo
        Thread.sleep(3000); // Dar tiempo para que el sistema detecte el fallo
        
        List<Future<Result>> recoveryFutures = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Task task = Client.createPrimeTestTask(982451653L + 100 + i);
            Future<Result> future = executor.submit(() -> client.submitTask(task));
            recoveryFutures.add(future);
        }

        // Verificar que las nuevas tareas se completan
        int recoverySuccess = 0;
        for (Future<Result> future : recoveryFutures) {
            try {
                Result result = future.get(30, TimeUnit.SECONDS);
                if (result != null && result.isSuccess()) {
                    recoverySuccess++;
                }
            } catch (Exception e) {
                // Ignorar errores individuales
            }
        }

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
        
        // El sistema debe continuar funcionando con los workers restantes
        assertTrue(recoverySuccess >= 3, 
                "Al menos 3 de 5 tareas después del fallo deben completarse. Completadas: " + recoverySuccess);
    }

    @Test
    public void testMasterRemainsAvailable() {
        assertTrue(master.isRunning(), "Master debe seguir corriendo después de fallos de workers");
        
        String masterHost = master.getHost();
        int masterPort = master.getMappedPort(8080);
        
        // Intentar enviar una tarea después de un fallo
        Client client = new Client(masterHost, masterPort);
        Task task = Client.createPrimeTestTask(982451653L);
        
        Result result = client.submitTask(task);
        
        // El master debe seguir respondiendo (aunque puede que no haya workers disponibles)
        assertNotNull(result, "Master debe responder incluso después de fallos");
    }
}
