package com.taskbalancer.tasks;

import com.taskbalancer.common.Result;
import com.taskbalancer.common.Task;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MatrixMultTaskTest {

	@Test
	void tamañoValido_devuelveExitoYResumen() {
		MatrixMultTask handler = new MatrixMultTask();
		Map<String, Object> p = new HashMap<>();
		p.put("size", 2);

		Result r = handler.execute(new Task("m1", handler.getTaskType(), p));

		assertTrue(r.isSuccess());
		assertNull(r.getError());
		assertTrue(r.getData() instanceof String);
		String summary = (String) r.getData();
		assertTrue(summary.startsWith("Matriz 2x2 multiplicada."));
		assertTrue(summary.contains("Elemento [0][0] ="));
	}

	@Test
	void tamañoExcedeMaximo_devuelveError() {
		MatrixMultTask handler = new MatrixMultTask();
		Map<String, Object> p = new HashMap<>();
		p.put("size", 1000);

		Result r = handler.execute(new Task("m2", handler.getTaskType(), p));

		assertFalse(r.isSuccess());
		assertNotNull(r.getError());
		assertTrue(r.getError().contains("Tamaño máximo"));
	}
}


