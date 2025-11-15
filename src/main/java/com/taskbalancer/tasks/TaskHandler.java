package com.taskbalancer.tasks;

import com.taskbalancer.common.Task;
import com.taskbalancer.common.Result;

/**
 * Interfaz para implementar diferentes tipos de tareas.
 */
public interface TaskHandler {
    /**
     * Ejecuta la tarea y devuelve el resultado.
     */
    Result execute(Task task);
    
    /**
     * Retorna el tipo de tarea que este handler puede procesar.
     */
    String getTaskType();
}

