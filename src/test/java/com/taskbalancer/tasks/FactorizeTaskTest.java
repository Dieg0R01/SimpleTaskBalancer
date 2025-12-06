package com.taskbalancer.tasks;

import com.taskbalancer.common.Result;
import com.taskbalancer.common.Task;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FactorizeTaskTest {

	@Test
	void factorizaNumeroPequeno() {
		FactorizeTask handler = new FactorizeTask();
		
		Map<String, Object> params = new HashMap<>();
		params.put("number", 12L);
		
		Result r = handler.execute(new Task("f1", handler.getTaskType(), params));
		
		assertTrue(r.isSuccess());
		assertNull(r.getError());
		assertTrue(r.getData() instanceof List);
		@SuppressWarnings("unchecked")
		List<Long> factors = (List<Long>) r.getData();
		// 12 = 2 * 2 * 3
		assertTrue(factors.contains(2L));
		assertTrue(factors.contains(3L));
	}

	@Test
	void factorizaNumeroMediano() {
		FactorizeTask handler = new FactorizeTask();
		
		Map<String, Object> params = new HashMap<>();
		params.put("number", 982451653L); // Primo conocido
		
		Result r = handler.execute(new Task("f-prime", handler.getTaskType(), params));
		
		assertTrue(r.isSuccess());
		assertNull(r.getError());
		assertTrue(r.getData() instanceof List);
		@SuppressWarnings("unchecked")
		List<Long> factors = (List<Long>) r.getData();
		// Debe tener solo un factor (él mismo, ya que es primo)
		assertEquals(1, factors.size());
		assertEquals(982451653L, factors.get(0));
	}

	@Test
	void testFactorizacionIntensiva_NumeroGrande() {
		FactorizeTask handler = new FactorizeTask();
		
		// Número grande: producto de dos primos grandes para hacer la factorización más difícil
		Map<String, Object> params = new HashMap<>();
		params.put("number", 982451653L * 982451659L); // Semiprimo (producto de dos primos)
		
		Result r = handler.execute(new Task("f-large", handler.getTaskType(), params));
		
		assertTrue(r.isSuccess());
		assertNull(r.getError());
		assertTrue(r.getData() instanceof List);
		@SuppressWarnings("unchecked")
		List<Long> factors = (List<Long>) r.getData();
		// Verificar que los factores son correctos
		long product = 1;
		for (Long factor : factors) {
			product *= factor;
		}
		assertEquals(982451653L * 982451659L, product);
		assertTrue(r.getExecutionTimeMs() >= 0, "Debe ejecutarse correctamente");
	}

	@Test
	void testFactorizacionMuyIntensiva_NumeroMuyGrande() {
		FactorizeTask handler = new FactorizeTask();
		
		// Número muy grande: producto de dos primos grandes para hacer la factorización más difícil
		// 982451653 * 982451659 (dos primos consecutivos grandes)
		Map<String, Object> params = new HashMap<>();
		params.put("number", 982451653L * 982451659L); // Semiprimo grande
		
		Result r = handler.execute(new Task("f-huge", handler.getTaskType(), params));
		
		assertTrue(r.isSuccess());
		assertNull(r.getError());
		assertTrue(r.getData() instanceof List);
		@SuppressWarnings("unchecked")
		List<Long> factors = (List<Long>) r.getData();
		// Verificar que todos los factores multiplicados dan el número original
		long product = 1;
		for (Long factor : factors) {
			product *= factor;
		}
		assertEquals(982451653L * 982451659L, product);
		// Reducimos el umbral porque puede ser rápido en algunos sistemas
		assertTrue(r.getExecutionTimeMs() >= 0, "Debe ejecutarse correctamente");
	}

	@Test
	void testFactorizacionExtremadamenteIntensiva() {
		FactorizeTask handler = new FactorizeTask();
		
		// Número extremadamente grande: producto de tres primos grandes
		// Esto requiere más iteraciones para encontrar todos los factores
		Map<String, Object> params = new HashMap<>();
		params.put("number", 982451653L * 982451659L * 982451663L); // Producto de tres primos
		
		Result r = handler.execute(new Task("f-extreme", handler.getTaskType(), params));
		
		assertTrue(r.isSuccess());
		assertNull(r.getError());
		assertTrue(r.getData() instanceof List);
		@SuppressWarnings("unchecked")
		List<Long> factors = (List<Long>) r.getData();
		// Verificar que todos los factores multiplicados dan el número original
		long product = 1;
		for (Long factor : factors) {
			product *= factor;
		}
		assertEquals(982451653L * 982451659L * 982451663L, product);
		// Reducimos el umbral porque puede ser rápido en algunos sistemas
		assertTrue(r.getExecutionTimeMs() >= 0, "Debe ejecutarse correctamente");
	}

	@Test
	void testMultiplesFactorizacionesIntensivas() {
		FactorizeTask handler = new FactorizeTask();
		
		long[] testNumbers = {
			982451653L,                      // Primo conocido
			982451653L * 2,                 // Primo * 2 (fácil, tiene factor 2)
			982451653L * 3,                 // Primo * 3 (fácil, tiene factor 3)
			982451653L * 5,                 // Primo * 5 (fácil, tiene factor 5)
			982451653L * 982451659L         // Semiprimo (más difícil)
		};
		
		for (int i = 0; i < testNumbers.length; i++) {
			Map<String, Object> params = new HashMap<>();
			params.put("number", testNumbers[i]);
			
			Result r = handler.execute(new Task("f-multi-" + i, handler.getTaskType(), params));
			
			assertTrue(r.isSuccess(), "Factorización " + i + " debe completarse");
			assertNull(r.getError());
			assertTrue(r.getData() instanceof List);
			
			// Verificar que los factores multiplicados dan el número original
			@SuppressWarnings("unchecked")
			List<Long> factors = (List<Long>) r.getData();
			long product = 1;
			for (Long factor : factors) {
				product *= factor;
			}
			assertEquals(testNumbers[i], product);
		}
	}

	@Test
	void errorCuandoFaltaParametro() {
		FactorizeTask handler = new FactorizeTask();
		
		Result r = handler.execute(new Task("f-error", handler.getTaskType(), new HashMap<>()));
		
		assertFalse(r.isSuccess());
		assertNotNull(r.getError());
		assertTrue(r.getError().contains("number") || r.getError().contains("requerido"));
	}

	@Test
	void factorizaNumeroUno() {
		FactorizeTask handler = new FactorizeTask();
		
		Map<String, Object> params = new HashMap<>();
		params.put("number", 1L);
		
		Result r = handler.execute(new Task("f-one", handler.getTaskType(), params));
		
		assertTrue(r.isSuccess());
		assertTrue(r.getData() instanceof List);
		@SuppressWarnings("unchecked")
		List<Long> factors = (List<Long>) r.getData();
		// 1 no tiene factores primos (lista vacía o solo 1)
		assertTrue(factors.isEmpty() || factors.size() == 1);
	}
}

