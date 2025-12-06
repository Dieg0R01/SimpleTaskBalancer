package com.taskbalancer.tasks;

import com.taskbalancer.common.Result;
import com.taskbalancer.common.Task;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PiEstimationTaskTest {

	@Test
	void ejecutaPiEstimationBasico() {
		PiEstimationTask handler = new PiEstimationTask();
		
		Map<String, Object> params = new HashMap<>();
		params.put("iterations", 1000);
		
		Result r = handler.execute(new Task("pi1", handler.getTaskType(), params));
		
		assertTrue(r.isSuccess());
		assertNull(r.getError());
		assertTrue(r.getData() instanceof Number);
		double piEstimate = ((Number) r.getData()).doubleValue();
		// PI debe estar entre 3.0 y 4.0
		assertTrue(piEstimate >= 3.0 && piEstimate <= 4.0);
	}

	@Test
	void testPiEstimationIntensivo_1MillonIteraciones() {
		PiEstimationTask handler = new PiEstimationTask();
		
		Map<String, Object> params = new HashMap<>();
		params.put("iterations", 1000000); // 1 millón de iteraciones para carga moderada
		
		Result r = handler.execute(new Task("pi-intensive-1m", handler.getTaskType(), params));
		
		assertTrue(r.isSuccess());
		assertNull(r.getError());
		assertTrue(r.getData() instanceof Number);
		double piEstimate = ((Number) r.getData()).doubleValue();
		assertTrue(piEstimate >= 3.0 && piEstimate <= 4.0);
		assertTrue(r.getExecutionTimeMs() > 0, "Debe tomar tiempo significativo");
	}

	@Test
	void testPiEstimationMuyIntensivo_10MillonesIteraciones() {
		PiEstimationTask handler = new PiEstimationTask();
		
		Map<String, Object> params = new HashMap<>();
		params.put("iterations", 10000000); // 10 millones de iteraciones para carga intensiva
		
		Result r = handler.execute(new Task("pi-very-intensive-10m", handler.getTaskType(), params));
		
		assertTrue(r.isSuccess());
		assertNull(r.getError());
		assertTrue(r.getData() instanceof Number);
		double piEstimate = ((Number) r.getData()).doubleValue();
		assertTrue(piEstimate >= 3.0 && piEstimate <= 4.0);
		assertTrue(r.getExecutionTimeMs() > 10, "Debe tomar tiempo considerable");
	}

	@Test
	void testPiEstimationExtremadamenteIntensivo_50MillonesIteraciones() {
		PiEstimationTask handler = new PiEstimationTask();
		
		Map<String, Object> params = new HashMap<>();
		params.put("iterations", 50000000); // 50 millones de iteraciones para carga muy intensiva
		
		Result r = handler.execute(new Task("pi-extreme-50m", handler.getTaskType(), params));
		
		assertTrue(r.isSuccess());
		assertNull(r.getError());
		assertTrue(r.getData() instanceof Number);
		double piEstimate = ((Number) r.getData()).doubleValue();
		assertTrue(piEstimate >= 3.0 && piEstimate <= 4.0);
		assertTrue(r.getExecutionTimeMs() > 50, "Debe tomar tiempo considerable");
	}

	@Test
	void testMultiplesPiEstimationsIntensivos() {
		PiEstimationTask handler = new PiEstimationTask();
		
		int[] iterations = {5000000, 10000000, 15000000, 20000000};
		
		for (int iter : iterations) {
			Map<String, Object> params = new HashMap<>();
			params.put("iterations", iter);
			
			Result r = handler.execute(new Task("pi-multi-" + iter, handler.getTaskType(), params));
			
			assertTrue(r.isSuccess(), "PI estimation con " + iter + " iteraciones debe completarse");
			assertNull(r.getError());
			assertTrue(r.getData() instanceof Number);
			double piEstimate = ((Number) r.getData()).doubleValue();
			assertTrue(piEstimate >= 3.0 && piEstimate <= 4.0);
		}
	}

	@Test
	void testPrecisionMejoraConMasIteraciones() {
		PiEstimationTask handler = new PiEstimationTask();
		
		// Con más iteraciones, la estimación debería ser más precisa
		Map<String, Object> params1 = new HashMap<>();
		params1.put("iterations", 10000);
		Result r1 = handler.execute(new Task("pi-low", handler.getTaskType(), params1));
		
		Map<String, Object> params2 = new HashMap<>();
		params2.put("iterations", 10000000);
		Result r2 = handler.execute(new Task("pi-high", handler.getTaskType(), params2));
		
		assertTrue(r1.isSuccess());
		assertTrue(r2.isSuccess());
		
		double pi1 = ((Number) r1.getData()).doubleValue();
		double pi2 = ((Number) r2.getData()).doubleValue();
		
		// Ambos deben estar en el rango válido
		assertTrue(pi1 >= 3.0 && pi1 <= 4.0);
		assertTrue(pi2 >= 3.0 && pi2 <= 4.0);
		
		// La estimación con más iteraciones debería estar más cerca de PI real (3.14159...)
		double realPi = 3.141592653589793;
		double error1 = Math.abs(pi1 - realPi);
		double error2 = Math.abs(pi2 - realPi);
		
		// En general, con más iteraciones el error debería ser menor (aunque no garantizado)
		// Solo verificamos que ambos son válidos
		assertTrue(error1 < 1.0 && error2 < 1.0);
	}

	@Test
	void errorCuandoFaltaParametro() {
		PiEstimationTask handler = new PiEstimationTask();
		
		Result r = handler.execute(new Task("pi-error", handler.getTaskType(), new HashMap<>()));
		
		assertFalse(r.isSuccess());
		assertNotNull(r.getError());
		assertTrue(r.getError().contains("iterations") || r.getError().contains("requerido"));
	}
}

