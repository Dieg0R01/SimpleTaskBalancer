package com.taskbalancer.worker;

import com.taskbalancer.common.Result;
import com.taskbalancer.common.Task;
import com.taskbalancer.tasks.TaskHandler;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TaskExecutorTest {

	@Test
	void ejecutaHandlerRegistrado_yDevuelveResultadoExitoso() {
		TaskExecutor executor = new TaskExecutor(); // registra handlers por defecto

		Map<String, Object> params = new HashMap<>();
		params.put("number", 17L);
		Task task = new Task("t1", "PRIME_TEST", params);

		Result result = executor.executeTask(task);

		assertTrue(result.isSuccess());
		assertEquals("t1", result.getTaskId());
		assertTrue(result.getError() == null || result.getError().isEmpty());
		assertTrue(result.getExecutionTimeMs() >= 0);
		assertTrue((Boolean) result.getData());
	}

	@Test
	void ejecutaTareaIntensiva_MatrixMultGrande() {
		TaskExecutor executor = new TaskExecutor();

		Map<String, Object> params = new HashMap<>();
		params.put("size", 300); // Matriz 300x300 para carga intensiva
		Task task = new Task("t-matrix-large", "MATRIX_MULT", params);

		Result result = executor.executeTask(task);

		assertTrue(result.isSuccess());
		assertEquals("t-matrix-large", result.getTaskId());
		assertTrue(result.getError() == null || result.getError().isEmpty());
		assertTrue(result.getExecutionTimeMs() > 0, "Debe tomar tiempo significativo");
		assertTrue(result.getData() instanceof String);
	}

	@Test
	void ejecutaTareaIntensiva_HashStress() {
		TaskExecutor executor = new TaskExecutor();

		Map<String, Object> params = new HashMap<>();
		params.put("input", "test-string-for-hashing");
		params.put("iterations", 100000); // 100,000 iteraciones para carga intensiva
		Task task = new Task("t-hash-stress", "HASH_STRESS", params);

		Result result = executor.executeTask(task);

		assertTrue(result.isSuccess());
		assertEquals("t-hash-stress", result.getTaskId());
		assertTrue(result.getError() == null || result.getError().isEmpty());
		assertTrue(result.getExecutionTimeMs() > 0, "Debe tomar tiempo significativo");
		assertTrue(result.getData() instanceof String);
	}

	@Test
	void ejecutaTareaIntensiva_PiEstimation() {
		TaskExecutor executor = new TaskExecutor();

		Map<String, Object> params = new HashMap<>();
		params.put("iterations", 10000000); // 10 millones de iteraciones para carga intensiva
		Task task = new Task("t-pi-estimation", "PI_ESTIMATION", params);

		Result result = executor.executeTask(task);

		assertTrue(result.isSuccess());
		assertEquals("t-pi-estimation", result.getTaskId());
		assertTrue(result.getError() == null || result.getError().isEmpty());
		assertTrue(result.getExecutionTimeMs() > 0, "Debe tomar tiempo significativo");
		assertTrue(result.getData() instanceof Number);
		double piEstimate = ((Number) result.getData()).doubleValue();
		// PI debe estar entre 3.0 y 4.0
		assertTrue(piEstimate >= 3.0 && piEstimate <= 4.0);
	}

	@Test
	void ejecutaTareaIntensiva_Factorize() {
		TaskExecutor executor = new TaskExecutor();

		Map<String, Object> params = new HashMap<>();
		// Semiprimo (producto de dos primos grandes) para factorización más intensiva
		params.put("number", 982451653L * 982451659L);
		Task task = new Task("t-factorize", "FACTORIZE", params);

		Result result = executor.executeTask(task);

		assertTrue(result.isSuccess());
		assertEquals("t-factorize", result.getTaskId());
		assertTrue(result.getError() == null || result.getError().isEmpty());
		// Verificar que se ejecutó correctamente (tiempo puede variar según hardware)
		assertTrue(result.getExecutionTimeMs() >= 0, "Debe ejecutarse correctamente");
		assertTrue(result.getData() instanceof java.util.List);
		
		// Verificar que los factores son correctos
		@SuppressWarnings("unchecked")
		java.util.List<Long> factors = (java.util.List<Long>) result.getData();
		long product = 1;
		for (Long factor : factors) {
			product *= factor;
		}
		assertEquals(982451653L * 982451659L, product);
	}

	@Test
	void retornaErrorSiNoExisteHandlerParaTipo() {
		TaskExecutor executor = new TaskExecutor();
		Task task = new Task("t2", "UNKNOWN_TYPE", new HashMap<>());

		Result result = executor.executeTask(task);

		assertFalse(result.isSuccess());
		assertEquals("t2", result.getTaskId());
		assertNotNull(result.getError());
		assertTrue(result.getError().contains("Tipo de tarea desconocido"));
	}

	@Test
	@SuppressWarnings("unchecked")
	void manejaExcepcionDeHandler_lanzaExcepcionDelHandler() throws Exception {
		TaskExecutor executor = new TaskExecutor();

		// Preparamos un mock de TaskHandler que lanza excepción
		TaskHandler throwingHandler = Mockito.mock(TaskHandler.class);
		when(throwingHandler.getTaskType()).thenReturn("THROWING");
		when(throwingHandler.execute(any())).thenThrow(new RuntimeException("boom"));

		// Inyectamos el mock en el mapa privado 'handlers'
		Field f = TaskExecutor.class.getDeclaredField("handlers");
		f.setAccessible(true);
		Map<String, TaskHandler> map = (Map<String, TaskHandler>) f.get(executor);
		map.put("THROWING", throwingHandler);

		Task task = new Task("t3", "THROWING", new HashMap<>());
		RuntimeException ex = assertThrows(RuntimeException.class, () -> executor.executeTask(task));
		assertTrue(ex.getMessage().contains("boom"));
		verify(throwingHandler, times(1)).execute(task);
	}
}


