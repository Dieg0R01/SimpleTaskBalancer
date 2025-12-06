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

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de integración para verificar el funcionamiento básico del sistema distribuido.
 * Estos tests levantan contenedores Docker reales para simular un entorno distribuido.
 */
@Testcontainers
public class DistributedSystemTest {

    @SuppressWarnings("resource")
    private static Network network;
    @SuppressWarnings("resource")
    private static GenericContainer<?> master;
    @SuppressWarnings("resource")
    private static GenericContainer<?> worker1;
    @SuppressWarnings("resource")
    private static GenericContainer<?> worker2;

    @BeforeAll
    public static void setUp() {
        // Crear red Docker para los contenedores
        network = Network.newNetwork();
        // Construir imagen usando ImageFromDockerfile directamente
        // El segundo parámetro 'false' indica que no debe eliminar la imagen después de los tests
        ImageFromDockerfile image = new ImageFromDockerfile("taskbalancer-simpletaskbalancer:latest", false)
            .withDockerfile(Paths.get(System.getProperty("user.dir"), "Dockerfile"));

        // Crear Master usando ImageFromDockerfile
        // GenericContainer puede aceptar ImageFromDockerfile directamente
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

        // Crear Workers usando ImageFromDockerfile
        @SuppressWarnings("resource")
        GenericContainer<?> worker1Container = new GenericContainer<>(image)
                .withNetwork(network)
                .withCommand("java", "-cp", "bin", "com.taskbalancer.worker.Worker", 
                            "worker-1", "master", "8080", "4")
                .dependsOn(master)
                .withStartupTimeout(java.time.Duration.ofSeconds(30));
        worker1 = worker1Container;
        worker1.start();

        @SuppressWarnings("resource")
        GenericContainer<?> worker2Container = new GenericContainer<>(image)
                .withNetwork(network)
                .withCommand("java", "-cp", "bin", "com.taskbalancer.worker.Worker", 
                            "worker-2", "master", "8080", "4")
                .dependsOn(master)
                .withStartupTimeout(java.time.Duration.ofSeconds(30));
        worker2 = worker2Container;
        worker2.start();
        
        // Esperar a que los workers se registren
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @AfterAll
    public static void tearDown() {
        if (worker2 != null) worker2.stop();
        if (worker1 != null) worker1.stop();
        if (master != null) master.stop();
        if (network != null) network.close();
    }

    @Test
    public void testMasterWorkerConnection() {
        assertTrue(master.isRunning(), "Master debe estar corriendo");
        assertTrue(worker1.isRunning(), "Worker 1 debe estar corriendo");
        assertTrue(worker2.isRunning(), "Worker 2 debe estar corriendo");
    }

    @Test
    public void testTaskExecution() {
        String masterHost = master.getHost();
        int masterPort = master.getMappedPort(8080);
        
        Client client = new Client(masterHost, masterPort);
        
        Task task = Client.createPrimeTestTask(17L);
        
        Result result = client.submitTask(task);
        
        assertNotNull(result, "El resultado no debe ser null");
        assertEquals(task.getTaskId(), result.getTaskId(), "El taskId debe coincidir");
        assertTrue(result.isSuccess(), "La tarea debe completarse exitosamente");
        assertNotNull(result.getWorkerId(), "El resultado debe incluir el ID del worker");
    }

    @Test
    public void testMultipleTaskTypes() {
        String masterHost = master.getHost();
        int masterPort = master.getMappedPort(8080);
        
        Client client = new Client(masterHost, masterPort);
        
        // Test PRIME_TEST
        Task primeTask = Client.createPrimeTestTask(982451653L);
        Result primeResult = client.submitTask(primeTask);
        assertTrue(primeResult.isSuccess(), "PRIME_TEST debe completarse exitosamente");
        
        // Test PRIME_RANGE
        Task rangeTask = Client.createPrimeRangeTask(1000L, 1100L);
        Result rangeResult = client.submitTask(rangeTask);
        assertTrue(rangeResult.isSuccess(), "PRIME_RANGE debe completarse exitosamente");
        
        // Test FACTORIZE
        Task factorizeTask = Client.createFactorizeTask(123456789L);
        Result factorizeResult = client.submitTask(factorizeTask);
        assertTrue(factorizeResult.isSuccess(), "FACTORIZE debe completarse exitosamente");
    }
}
