package com.taskbalancer.common;

import java.io.Serializable;

/**
 * Mensaje genérico para la comunicación entre componentes del sistema.
 * Tipos: TASK, RESULT, WORKER_REGISTER, HEARTBEAT, ACK, ERROR
 */
public class Message implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String type;
    private Object payload;
    private long timestamp;
    
    public Message() {
        this.timestamp = System.currentTimeMillis();
    }
    
    public Message(String type, Object payload) {
        this.type = type;
        this.payload = payload;
        this.timestamp = System.currentTimeMillis();
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public Object getPayload() {
        return payload;
    }
    
    public void setPayload(Object payload) {
        this.payload = payload;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    @Override
    public String toString() {
        return "Message{" +
                "type='" + type + '\'' +
                ", payload=" + payload +
                ", timestamp=" + timestamp +
                '}';
    }
}

