package com.taskbalancer.common;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Representa una tarea a ser ejecutada por un Worker.
 * Contiene el tipo de tarea y los parámetros necesarios.
 */
public class Task implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String taskId;
    private String taskType;
    private Map<String, Object> parameters;
    private long submittedAt;
    
    public Task() {
        this.parameters = new HashMap<>();
        this.submittedAt = System.currentTimeMillis();
    }
    
    public Task(String taskId, String taskType, Map<String, Object> parameters) {
        this.taskId = taskId;
        this.taskType = taskType;
        this.parameters = parameters != null ? parameters : new HashMap<>();
        this.submittedAt = System.currentTimeMillis();
    }
    
    public String getTaskId() {
        return taskId;
    }
    
    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }
    
    public String getTaskType() {
        return taskType;
    }
    
    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }
    
    public Map<String, Object> getParameters() {
        return parameters;
    }
    
    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }
    
    public long getSubmittedAt() {
        return submittedAt;
    }
    
    public void setSubmittedAt(long submittedAt) {
        this.submittedAt = submittedAt;
    }
    
    @Override
    public String toString() {
        return "Task{" +
                "taskId='" + taskId + '\'' +
                ", taskType='" + taskType + '\'' +
                ", parameters=" + parameters +
                ", submittedAt=" + submittedAt +
                '}';
    }
}

