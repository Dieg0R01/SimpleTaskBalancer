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
	void testMatrizGrande_100x100() {
		// Matriz 100x100 = 10,000 elementos, multiplicación O(n³) = 1,000,000 operaciones
		MatrixMultTask handler = new MatrixMultTask();
		Map<String, Object> p = new HashMap<>();
		p.put("size", 100);

		Result r = handler.execute(new Task("m-large-100", handler.getTaskType(), p));

		assertTrue(r.isSuccess());
		assertNull(r.getError());
		assertTrue(r.getData() instanceof String);
		String summary = (String) r.getData();
		assertTrue(summary.contains("Matriz 100x100 multiplicada"));
		assertTrue(r.getExecutionTimeMs() > 0, "Debe tomar tiempo significativo");
	}

	@Test
	void testMatrizMuyGrande_300x300() {
		// Matriz 300x300 = 90,000 elementos, multiplicación O(n³) = 27,000,000 operaciones
		MatrixMultTask handler = new MatrixMultTask();
		Map<String, Object> p = new HashMap<>();
		p.put("size", 300);

		Result r = handler.execute(new Task("m-huge-300", handler.getTaskType(), p));

		assertTrue(r.isSuccess());
		assertNull(r.getError());
		assertTrue(r.getData() instanceof String);
		String summary = (String) r.getData();
		assertTrue(summary.contains("Matriz 300x300 multiplicada"));
		assertTrue(r.getExecutionTimeMs() > 10, "Debe tomar tiempo considerable");
	}

	@Test
	void testMatrizCasiMaxima_450x450() {
		// Matriz 450x450 cerca del límite de 500, genera carga intensiva
		// 450³ = 91,125,000 operaciones de multiplicación
		MatrixMultTask handler = new MatrixMultTask();
		Map<String, Object> p = new HashMap<>();
		p.put("size", 450);

		Result r = handler.execute(new Task("m-max-450", handler.getTaskType(), p));

		assertTrue(r.isSuccess());
		assertNull(r.getError());
		assertTrue(r.getData() instanceof String);
		String summary = (String) r.getData();
		assertTrue(summary.contains("Matriz 450x450 multiplicada"));
		assertTrue(r.getExecutionTimeMs() > 20, "Debe tomar tiempo considerable");
	}

	@Test
	void testMultiplesMatricesGrandes() {
		// Ejecutar múltiples matrices grandes para generar más carga
		MatrixMultTask handler = new MatrixMultTask();
		
		int[] sizes = {100, 150, 200, 250, 300};
		
		for (int size : sizes) {
			Map<String, Object> p = new HashMap<>();
			p.put("size", size);
			Result r = handler.execute(new Task("m-multi-" + size, handler.getTaskType(), p));
			
			assertTrue(r.isSuccess(), "Matriz " + size + "x" + size + " debe completarse");
			assertNull(r.getError());
			assertTrue(r.getExecutionTimeMs() >= 0);
		}
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


