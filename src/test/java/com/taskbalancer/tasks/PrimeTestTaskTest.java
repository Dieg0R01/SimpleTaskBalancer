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
	void errorCuandoFaltaParametro() {
		PrimeTestTask handler = new PrimeTestTask();
		Result r = handler.execute(new Task("p3", handler.getTaskType(), new HashMap<>()));
		assertFalse(r.isSuccess());
		assertNotNull(r.getError());
		assertTrue(r.getError().contains("number"));
	}
}


