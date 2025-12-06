package com.taskbalancer.master;

import org.junit.jupiter.api.Test;

import java.io.ObjectOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class WorkerRegistryTest {

	@Test
	void registra_y_recupera_workers_y_actualiza_heartbeat() throws Exception {
		WorkerRegistry registry = new WorkerRegistry();
		WorkerInfo w = new WorkerInfo("w1", 2, (ObjectOutputStream) null);
		registry.registerWorker(w);

		assertEquals(1, registry.getWorkerCount());
		assertNotNull(registry.getWorker("w1"));
		assertTrue(registry.getAllWorkers().size() == 1);
		assertTrue(registry.getAvailableWorkers().size() == 1);
		assertTrue(registry.getActiveWorkerCount() == 1);

		// update heartbeat + current tasks
		registry.updateWorkerHeartbeat("w1", 1);
		WorkerInfo fetched = registry.getWorker("w1");
		assertEquals(1, fetched.getCurrentTasks());
		assertTrue(fetched.isActive());
	}

	@Test
	void expira_worker_por_timeout() throws Exception {
		WorkerRegistry registry = new WorkerRegistry();
		WorkerInfo w = new WorkerInfo("w2", 1, (ObjectOutputStream) null);
		registry.registerWorker(w);

		Thread.sleep(20);
		registry.checkTimeouts(10); // timeout de 10ms

		WorkerInfo fetched = registry.getWorker("w2");
		assertNotNull(fetched);
		assertFalse(fetched.isActive());
	}

	@Test
	void desregistra_worker() {
		WorkerRegistry registry = new WorkerRegistry();
		WorkerInfo w = new WorkerInfo("w3", 1, (ObjectOutputStream) null);
		registry.registerWorker(w);
		assertEquals(1, registry.getWorkerCount());

		registry.unregisterWorker("w3");
		assertEquals(0, registry.getWorkerCount());
		assertNull(registry.getWorker("w3"));
	}
}


