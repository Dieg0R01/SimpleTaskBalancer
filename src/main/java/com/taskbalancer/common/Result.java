package com.taskbalancer.common;

import java.io.Serializable;

/**
 * Representa el resultado de la ejecución de una tarea.
 */
public class Result implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String taskId;
    private boolean success;
    private Object data;
    private String error;
    private long executionTimeMs;
    private String workerId;
    
    public Result() {
    }
    
    public Result(String taskId, boolean success, Object data, String error) {
        this.taskId = taskId;
        this.success = success;
        this.data = data;
        this.error = error;
    }
    
    public String getTaskId() {
        return taskId;
    }
    
    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public Object getData() {
        return data;
    }
    
    public void setData(Object data) {
        this.data = data;
    }
    
    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
    }
    
    public long getExecutionTimeMs() {
        return executionTimeMs;
    }
    
    public void setExecutionTimeMs(long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }
    
    public String getWorkerId() {
        return workerId;
    }
    
    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }
    
    @Override
    public String toString() {
        return "Result{" +
                "taskId='" + taskId + '\'' +
                ", success=" + success +
                ", data=" + data +
                ", error='" + error + '\'' +
                ", executionTimeMs=" + executionTimeMs +
                ", workerId='" + workerId + '\'' +
                '}';
    }
}

