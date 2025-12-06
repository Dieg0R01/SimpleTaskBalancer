package com.taskbalancer.master;

import com.taskbalancer.common.Task;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class TaskQueueTest {

	@Test
	void encolaYDesencolaEnOrdenFIFO() throws Exception {
		TaskQueue queue = new TaskQueue();
		Task t1 = new Task("a", "PRIME_TEST", new HashMap<>());
		Task t2 = new Task("b", "PRIME_TEST", new HashMap<>());

		queue.enqueue(t1);
		queue.enqueue(t2);

		assertEquals(2, queue.size());
		assertSame(t1, queue.dequeue());
		assertSame(t2, queue.dequeue());
		assertTrue(queue.isEmpty());
	}

	@Test
	void dequeueConTimeoutRetornaNullSiVacio() throws Exception {
		TaskQueue queue = new TaskQueue();
		assertNull(queue.dequeue(50, TimeUnit.MILLISECONDS));
	}
}


