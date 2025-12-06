# 🐛 Bugfix: Race Condition en ObjectOutputStream

## 📋 Resumen Ejecutivo

Se identificó y corrigió una **race condition crítica** en el Master que causaba la desconexión inmediata de los Workers después del registro, provocando que todas las tareas fallaran con `TimeoutException`.

## 🔍 Síntomas Observados

### Comportamiento del Sistema

```
[Master] Worker registrado: worker-1
[Master] Worker registrado: worker-2
[Master] Worker registrado: worker-3
[Master] Tarea recibida de cliente: xxx
[Master] Asignando tarea xxx a worker-2
[Master] Conexión worker perdida: worker-2         ← ❌ DESCONEXIÓN INMEDIATA
[Master] Error en conexión: invalid type code: 00  ← ❌ ERROR CRÍTICO
[Master] Conexión worker perdida: worker-3
[Master] Conexión worker perdida: worker-1
```

### Resultados en el Cliente

```
Resultado: Result{
    success=false, 
    error='Timeout o error: java.util.concurrent.TimeoutException',
    workerId='null'
}
```

### Métricas del Sistema

- ✅ Workers se registraban correctamente
- ✅ Workers se conectaban al Master
- ❌ **Workers se desconectaban inmediatamente después del primer mensaje**
- ❌ Tareas encoladas pero nunca ejecutadas
- ❌ 100% de tareas con timeout (60 segundos)

---

## 🔬 Análisis de Causa Raíz

### El Problema: Race Condition con ObjectInputStream/ObjectOutputStream

#### Arquitectura Problemática Original

```java
// Master.java - CÓDIGO PROBLEMÁTICO
private void handleConnection(Socket socket) {
    ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
    ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
  
    while (true) {  // ← Thread 1: Bucle de lectura principal
        Object obj = in.readObject();
        if (obj instanceof Message) {
            processMessage((Message) obj, in, out, socket);
        }
    }
}

private void handleWorkerRegistration(...) {
    // ... registro del worker ...
  
    // ❌ PROBLEMA: Crear un segundo thread que lee del MISMO InputStream
    new Thread(() -> maintainWorkerConnection(socket, workerId, in, out)).start();
    // Thread 2: Bucle de lectura dedicado al worker
}
```

#### ¿Por Qué Falla?

**Dos hilos intentando leer del mismo `ObjectInputStream` simultáneamente:**

1. **Thread 1** (`handleConnection`): Continúa en el bucle `while(true)`, esperando leer el próximo objeto
2. **Thread 2** (`maintainWorkerConnection`): También inicia un bucle para leer mensajes del worker

**Resultado**:

- Ambos threads compiten por leer del stream
- Los bytes del stream se dividen entre ambos threads de forma impredecible
- **Corrupción del protocolo de serialización de Java**
- Error: `invalid type code: 00` (el stream está corrompido)
- Desconexión inmediata del worker

### Diagrama del Problema

```
                    ┌─────────────┐
                    │   Worker    │
                    └──────┬──────┘
                           │
                      ObjectStream
                           │
                    ┌──────▼──────┐
                    │   Master    │
                    │             │
      ┌─────────────┼─────────────┼─────────────┐
      │             │             │             │
  Thread 1     Thread 2      Thread 3      Thread 4
 (acceptor)   (acceptor)   (maintainWorker) (assigner)
      │             │             │             │
      └─────────┬───┴─────────────┘             │
                │                               │
          [RACE CONDITION]               [BlockingQueue]
          Ambos leen de IN             
          Ambos escriben a OUT
```

### Tipo de Race Condition

**Nombre técnico**: Thread-unsafe stream access / Multiple readers-writers problem

**Características**:

- Acceso concurrente no sincronizado a recurso compartido
- Violación del contrato de `ObjectInputStream` (single-threaded)
- Corrupción de estado del stream de serialización

---

## 🔧 Solución Implementada

### Cambios Arquitectónicos

#### 1. **Separación de Responsabilidades de Lectura**

**ANTES**: Múltiples threads leyendo del mismo stream
**DESPUÉS**: Un solo thread por stream de entrada

```java
// Master.java - CÓDIGO CORREGIDO
private void handleConnection(Socket socket) {
    ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
    ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
  
    // ✅ Leer SOLO el primer mensaje para determinar el tipo de conexión
    Object obj = in.readObject();
    if (obj instanceof Message) {
        Message message = (Message) obj;
        String type = message.getType();
      
        if ("WORKER_REGISTER".equals(type)) {
            handleWorkerRegistration(message, in, out, socket);
            // ✅ DELEGAR la lectura continua al thread dedicado
            return; // Este thread termina aquí
        } else if ("TASK".equals(type)) {
            handleClientTask(message, out);
            return; // Mantener conexión para resultado
        }
    }
}
```

#### 2. **Thread Dedicado por Worker**

```java
private void handleWorkerRegistration(...) {
    // ... registro ...
  
    // ✅ Thread dedicado CON NOMBRE para debugging
    Thread workerThread = new Thread(() -> 
        maintainWorkerConnection(socket, workerId, in, out)
    );
    workerThread.setName("Worker-" + workerId);  // ✅ Identificación clara
    workerThread.setDaemon(true);                // ✅ No bloquea shutdown
    workerThread.start();
  
    // El thread original YA NO lee más del stream
}
```

#### 3. **Sincronización de Escrituras**

**Problema**: Múltiples threads escribiendo al mismo `ObjectOutputStream`

```java
// ✅ SOLUCIÓN: Sincronizar TODAS las escrituras

// En assignTaskToWorker()
synchronized (selectedWorker.getOutputStream()) {
    selectedWorker.getOutputStream().writeObject(taskMessage);
    selectedWorker.getOutputStream().flush();
    selectedWorker.getOutputStream().reset();
}

// En handleClientTask() - ACK
synchronized (clientOut) {
    clientOut.writeObject(ack);
    clientOut.flush();
}

// En CompletableFuture callbacks
synchronized (clientOut) {
    clientOut.writeObject(resultMsg);
    clientOut.flush();
}
```

#### 4. **Mejora en Worker: Gestión de Reset**

```java
// Worker.java
private synchronized void sendMessage(Message message) {
    if (out != null) {
        try {
            out.writeObject(message);
            out.flush();
          
            // ✅ Solo reset en mensajes críticos (no en heartbeats)
            if (!"HEARTBEAT".equals(message.getType())) {
                out.reset();
            }
        } catch (IOException e) {
            // ✅ Si falla el envío, marcar para shutdown
            running.set(false);
        }
    }
}
```

---

## 📊 Comparación Antes/Después

### Flujo de Mensajes - ANTES (Problemático)

```
Worker conecta → Master acepta
                    ↓
        Thread A lee registro ──┐
                    ↓           │
        Registra worker         │
                    ↓           │ ← RACE CONDITION
        Crea Thread B ──────────┤
                    ↓           │
        Thread A sigue leyendo  │
        Thread B también lee ←──┘
                    ↓
           Stream corrupto
                    ↓
          Desconexión ❌
```

### Flujo de Mensajes - DESPUÉS (Corregido)

```
Worker conecta → Master acepta
                    ↓
        Thread A lee registro
                    ↓
        Registra worker
                    ↓
        Crea Thread B
                    ↓
        Thread A TERMINA ✅
                    ↓
        Solo Thread B lee ✅
                    ↓
     Conexión estable ✅
```

---

## 🎯 Resultados y Validación

### Métricas Post-Fix

✅ **Conexiones Estables**

- Workers permanecen conectados
- Heartbeats cada 5 segundos funcionando
- Sin errores "invalid type code"

✅ **Ejecución de Tareas**

- Tareas asignadas correctamente
- Workers procesan y devuelven resultados
- 0% de timeouts

✅ **Concurrencia Funcional**

- Múltiples workers simultáneos
- Pool de threads funcionando
- Sin race conditions

### Prueba Exitosa

```
[Master] Worker registrado: worker-1 ✅
[Master] Worker registrado: worker-2 ✅
[Master] Worker registrado: worker-3 ✅
[Master] Tarea recibida: xxx
[Master] Asignando tarea xxx a worker-2
[Worker worker-2] Tarea recibida: xxx (PRIME_TEST)
[Worker worker-2] Tarea completada: xxx en 1ms ✅
[Master] Resultado recibido: xxx de worker-2 ✅

Resultado: Result{
    success=true, 
    data=true,
    executionTimeMs=1,
    workerId='worker-2'
} ✅
```

---

## 📚 Lecciones Aprendidas

### 1. **ObjectInputStream/ObjectOutputStream NO son Thread-Safe**

```java
❌ NUNCA hacer esto:
Thread 1: in.readObject()  ┐
Thread 2: in.readObject()  ┴─ Race condition
Thread 3: out.writeObject()┐
Thread 4: out.writeObject()┴─ Necesita sincronización
```

### 2. **Un Stream = Un Propietario**

**Regla de oro**: Un solo thread debe ser el "dueño" de un `InputStream`.

**Si necesitas múltiples lectores**: Usa un thread coordinador con colas internas.

### 3. **Sincronización de Escrituras**

Aunque múltiples threads puedan necesitar escribir, TODAS las escrituras al mismo `OutputStream` deben estar sincronizadas:

```java
synchronized (outputStream) {
    outputStream.writeObject(...);
    outputStream.flush();
}
```

### 4. **Naming Threads para Debugging**

```java
// ✅ BUENA PRÁCTICA
Thread workerThread = new Thread(...);
workerThread.setName("Worker-" + workerId);

// En logs y debugging:
[Worker-worker-1] ...  ← Identificación clara
```

### 5. **Daemon Threads para Workers**

```java
workerThread.setDaemon(true);
```

Permite que el Master se cierre limpiamente sin esperar a threads de workers.

---

## 🔐 Garantías de Concurrencia

### Invariantes Establecidos

1. **Un stream, un lector**: Cada `ObjectInputStream` tiene exactamente un thread que lo lee
2. **Escrituras sincronizadas**: Todas las escrituras a `ObjectOutputStream` están protegidas por locks
3. **Thread ownership**: Cada worker tiene su propio thread dedicado con nombre identificable
4. **Graceful shutdown**: Threads daemon no bloquean el cierre del sistema

### Patrones Aplicados

- **Single Reader Pattern**: Un solo thread lee de cada stream
- **Synchronized Writer Pattern**: Múltiples writers con sincronización explícita
- **Thread-per-Connection**: Modelo clásico y probado para servidores
- **Daemon Threads**: Para workers que no deben bloquear shutdown

---

## 🔜 Mejoras Futuras Potenciales

### Corto Plazo

- [ ] Añadir timeout de heartbeat configurable
- [ ] Logging más detallado de eventos de conexión/desconexión
- [ ] Métricas de latencia de red

### Largo Plazo

- [ ] Considerar NIO (java.nio) para mayor escalabilidad
- [ ] Pool de threads configurable dinámicamente
- [ ] Implementar reconexión automática de workers

---

## 📖 Referencias

- Java ObjectInputStream Documentation: [docs.oracle.com](https://docs.oracle.com/javase/8/docs/api/java/io/ObjectInputStream.html)
- Java Concurrency in Practice - Brian Goetz
- Effective Java (3rd Edition) - Item 84: Don't depend on the thread scheduler
