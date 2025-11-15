package com.taskbalancer.common;

import java.io.*;

/**
 * Utilidades para serialización de objetos usando ObjectOutputStream.
 * Facilita la transferencia de objetos entre procesos, almacenamiento persistente, y reduce la repetición de código.
 */
public class SerializationUtils {
    
    /**
     * Serializa un objeto a bytes.
     */
    public static byte[] serialize(Object obj) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(obj);
            oos.flush();
            return baos.toByteArray();
        }
    }
    
    /**
     * Deserializa bytes a un objeto.
     */
    public static Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
             ObjectInputStream ois = new ObjectInputStream(bais)) {
            return ois.readObject();
        }
    }
    
    /**
     * Escribe un objeto directamente al OutputStream.
     */
    public static void writeObject(OutputStream out, Object obj) throws IOException {
        ObjectOutputStream oos = new ObjectOutputStream(out);
        oos.writeObject(obj);
        oos.flush();
    }
    
    /**
     * Lee un objeto directamente del InputStream.
     */
    public static Object readObject(InputStream in) throws IOException, ClassNotFoundException {
        ObjectInputStream ois = new ObjectInputStream(in);
        return ois.readObject();
    }
}

