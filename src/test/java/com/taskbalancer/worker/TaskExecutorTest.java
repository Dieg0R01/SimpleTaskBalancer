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


