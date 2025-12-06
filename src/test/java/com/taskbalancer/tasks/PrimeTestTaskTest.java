package com.taskbalancer.tasks;

import com.taskbalancer.common.Result;
import com.taskbalancer.common.Task;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PrimeTestTaskTest {

	@Test
	void devuelveTrueParaPrimos_yFalseParaNoPrimos() {
		PrimeTestTask handler = new PrimeTestTask();

		Map<String, Object> p17 = new HashMap<>();
		p17.put("number", 17L);
		Result r1 = handler.execute(new Task("p1", handler.getTaskType(), p17));
		assertTrue(r1.isSuccess());
		assertEquals(true, r1.getData());

		Map<String, Object> p15 = new HashMap<>();
		p15.put("number", 15L);
		Result r2 = handler.execute(new Task("p2", handler.getTaskType(), p15));
		assertTrue(r2.isSuccess());
		assertEquals(false, r2.getData());
	}

	@Test
	void testNumeroPrimoMuyGrande() {
		// Número primo de 15 dígitos: 982451653 (primo conocido)
		// Usamos un primo muy grande para generar carga de CPU
		PrimeTestTask handler = new PrimeTestTask();
		
		// Primo de 15 dígitos: 982451653
		Map<String, Object> params = new HashMap<>();
		params.put("number", 982451653L);
		Result r = handler.execute(new Task("p-large", handler.getTaskType(), params));
		
		assertTrue(r.isSuccess());
		assertEquals(true, r.getData());
		assertTrue(r.getExecutionTimeMs() >= 0);
	}

	@Test
	void testNumeroPrimoExtremadamenteGrande() {
		// Número primo de 18 dígitos para carga intensiva de CPU
		PrimeTestTask handler = new PrimeTestTask();
		
		// Primo conocido: 982451653 (ya usado) -> usamos uno más grande
		// 982451653 es primo, pero probamos con números aún más grandes
		// 982451653 * 1000000 + 1 para hacerlo más difícil
		Map<String, Object> params = new HashMap<>();
		params.put("number", 982451653000001L); // Número grande para forzar muchas iteraciones
		Result r = handler.execute(new Task("p-huge", handler.getTaskType(), params));
		
		assertTrue(r.isSuccess());
		// No sabemos si es primo, pero debe ejecutarse sin error
		assertNotNull(r.getData());
		assertTrue(r.getExecutionTimeMs() > 0, "Debe tomar tiempo significativo");
	}

	@Test
	void testMultiplesNumerosGrandes() {
		// Test con múltiples números grandes para generar más carga
		PrimeTestTask handler = new PrimeTestTask();
		
		long[] testNumbers = {
			982451653L,           // Primo conocido
			982451653000001L,    // Número grande
			982451653000003L,    // Otro número grande
			982451653000007L,    // Otro número grande
			982451653000009L     // Otro número grande
		};
		
		for (int i = 0; i < testNumbers.length; i++) {
			Map<String, Object> params = new HashMap<>();
			params.put("number", testNumbers[i]);
			Result r = handler.execute(new Task("p-multi-" + i, handler.getTaskType(), params));
			
			assertTrue(r.isSuccess(), "Tarea " + i + " debe completarse");
			assertNotNull(r.getData());
			assertTrue(r.getExecutionTimeMs() >= 0);
		}
	}

	@Test
	void errorCuandoFaltaParametro() {
		PrimeTestTask handler = new PrimeTestTask();
		Result r = handler.execute(new Task("p3", handler.getTaskType(), new HashMap<>()));
		assertFalse(r.isSuccess());
		assertNotNull(r.getError());
		assertTrue(r.getError().contains("number"));
	}
}


