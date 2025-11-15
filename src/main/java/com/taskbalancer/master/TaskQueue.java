package com.taskbalancer.master;

import com.taskbalancer.common.Task;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Cola de tareas pendientes de asignación.
 * Wrapper para centralizar lógica, facilitar extensiones , separar funcionalidad y mantener buenas prácticas OO.
 */
public class TaskQueue {
    
    private final BlockingQueue<Task> queue;
    
    public TaskQueue() {
        this.queue = new LinkedBlockingQueue<>();
    }
    
    public TaskQueue(int capacity) {
        this.queue = new LinkedBlockingQueue<>(capacity);
    }
    
    public void enqueue(Task task) throws InterruptedException {
        queue.put(task);
        System.out.println("[Master] Tarea encolada: " + task.getTaskId() + 
                        " (Cola: " + queue.size() + ")");
    }
    
    public Task dequeue() throws InterruptedException {
        return queue.take();
    }
    
    public Task dequeue(long timeout, TimeUnit unit) throws InterruptedException {
        return queue.poll(timeout, unit);
    }
    
    public int size() {
        return queue.size();
    }
    
    public boolean isEmpty() {
        return queue.isEmpty();
    }
    
    public void clear() {
        queue.clear();
    }
}

