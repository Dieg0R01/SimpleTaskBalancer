package com.taskbalancer.tasks;

import com.taskbalancer.common.Result;
import com.taskbalancer.common.Task;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HashStressTaskTest {

	@Test
	void ejecutaHashStressBasico() {
		HashStressTask handler = new HashStressTask();
		
		Map<String, Object> params = new HashMap<>();
		params.put("input", "test-string");
		params.put("iterations", 10);
		
		Result r = handler.execute(new Task("h1", handler.getTaskType(), params));
		
		assertTrue(r.isSuccess());
		assertNull(r.getError());
		assertTrue(r.getData() instanceof String);
		String hash = (String) r.getData();
		assertEquals(64, hash.length()); // SHA-256 produce 64 caracteres hex
	}

	@Test
	void testHashStressIntensivo_10000Iteraciones() {
		HashStressTask handler = new HashStressTask();
		
		Map<String, Object> params = new HashMap<>();
		params.put("input", "test-string-for-intensive-hashing");
		params.put("iterations", 10000); // 10,000 iteraciones para carga moderada
		
		Result r = handler.execute(new Task("h-intensive-10k", handler.getTaskType(), params));
		
		assertTrue(r.isSuccess());
		assertNull(r.getError());
		assertTrue(r.getData() instanceof String);
		assertTrue(r.getExecutionTimeMs() > 0, "Debe tomar tiempo significativo");
	}

	@Test
	void testHashStressMuyIntensivo_100000Iteraciones() {
		HashStressTask handler = new HashStressTask();
		
		Map<String, Object> params = new HashMap<>();
		params.put("input", "test-string-for-very-intensive-hashing");
		params.put("iterations", 100000); // 100,000 iteraciones para carga intensiva
		
		Result r = handler.execute(new Task("h-very-intensive-100k", handler.getTaskType(), params));
		
		assertTrue(r.isSuccess());
		assertNull(r.getError());
		assertTrue(r.getData() instanceof String);
		String hash = (String) r.getData();
		assertEquals(64, hash.length());
		assertTrue(r.getExecutionTimeMs() > 10, "Debe tomar tiempo considerable");
	}

	@Test
	void testHashStressExtremadamenteIntensivo_500000Iteraciones() {
		HashStressTask handler = new HashStressTask();
		
		Map<String, Object> params = new HashMap<>();
		params.put("input", "test-string-for-extremely-intensive-hashing");
		params.put("iterations", 500000); // 500,000 iteraciones para carga muy intensiva
		
		Result r = handler.execute(new Task("h-extreme-500k", handler.getTaskType(), params));
		
		assertTrue(r.isSuccess());
		assertNull(r.getError());
		assertTrue(r.getData() instanceof String);
		assertTrue(r.getExecutionTimeMs() > 50, "Debe tomar tiempo considerable");
	}

	@Test
	void testMultiplesHashStressIntensivos() {
		HashStressTask handler = new HashStressTask();
		
		int[] iterations = {50000, 100000, 150000, 200000};
		
		for (int iter : iterations) {
			Map<String, Object> params = new HashMap<>();
			params.put("input", "test-string-" + iter);
			params.put("iterations", iter);
			
			Result r = handler.execute(new Task("h-multi-" + iter, handler.getTaskType(), params));
			
			assertTrue(r.isSuccess(), "Hash con " + iter + " iteraciones debe completarse");
			assertNull(r.getError());
			assertTrue(r.getData() instanceof String);
		}
	}

	@Test
	void errorCuandoFaltaParametroInput() {
		HashStressTask handler = new HashStressTask();
		
		Map<String, Object> params = new HashMap<>();
		params.put("iterations", 100);
		
		Result r = handler.execute(new Task("h-error", handler.getTaskType(), params));
		
		assertFalse(r.isSuccess());
		assertNotNull(r.getError());
		assertTrue(r.getError().contains("input") || r.getError().contains("requerido"));
	}

	@Test
	void errorCuandoFaltaParametroIterations() {
		HashStressTask handler = new HashStressTask();
		
		Map<String, Object> params = new HashMap<>();
		params.put("input", "test");
		
		Result r = handler.execute(new Task("h-error2", handler.getTaskType(), params));
		
		assertFalse(r.isSuccess());
		assertNotNull(r.getError());
		assertTrue(r.getError().contains("iterations") || r.getError().contains("requerido"));
	}
}

