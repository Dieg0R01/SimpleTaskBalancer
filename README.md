# SimpleTaskBalancer

# Introducción

Este proyecto consiste en el desarrollo de un sistema distribuido basado en el patrón **Master-Worker** (o Despachador-Trabajador) llamado **SimpleTaskBalancer**. El objetivo es crear un servicio que reciba tareas computacionalmente intensivas de diferentes clientes y las distribuya de forma eficiente entre un conjunto de nodos "trabajadores" (Workers) para su ejecución paralela.

El sistema debe desacoplar la solicitud de la tarea de su ejecución, permitiendo que el sistema sea **escalable** (añadir más workers para aumentar la capacidad) y **concurrente** (atender a múltiples clientes y gestionar múltiples workers simultáneamente, por red, a investigar solución).

Workers ejecutan tareas de CPU intensivo (ej. test de primalidad, factorización) y devuelven resultados.

Comunicación por sockets TCP con mensajes JSON simples.

Concurrencia en master y workers con `java.util.concurrent` (`ExecutorService`, `BlockingQueue`, `CompletableFuture`).

No se usan librerías externas. Todo implementado con la SDK Java estándar.

# Objetivo funcional

- Recibir tareas de clientes y encolar.
- Distribuir tareas a workers disponibles.
- Ejecutar tareas en workers con concurrencia (multihilo).
- Recolectar resultados y devolvérselos al cliente.
- Manejar desconexiones, timeouts y reintentos.
- Monitorizar carga básica y exponer métricas simples por socket o consola.

## APIs a usar

- `java.net.ServerSocket`, `Socket` para comunicación.
- `java.util.concurrent` para concurrencia.
- `java.util.stream` para procesamiento local si procede.
- `java.io` para serialización básica (JSON hecho a mano o `javax.json` si disponible en el JDK; preferir JSON textual simple).

## Partes implicadas

| **Cliente**                | **Master / Balanceador**               | **Worker**                          |
| -------------------------- | -------------------------------------- | ----------------------------------- |
| • Envía tareas al Master.  | • Endpoint de recepción de tareas.    | • Ejecuta tareas en paralelo.      |
| • Recibe resultado final.  | • Cola de tareas.                      | • Pool de hilos concurrentes.      |
|                            | • Registro y gestión de workers.       | • Envía heartbeats al Master.      |
|                            | • Algoritmo de balanceo.               | • Devuelve resultados.             |
|                            | • Reintentos y timeouts.               |                                     |
|                            | • Componente de logging y métricas.    |                                     |

## Diseño concurrente (decisiones)

- **Master**
  - `ExecutorService acceptorPool` para aceptar conexiones.
  - `ExecutorService assignerPool` para tareas de despacho.
  - `ScheduledExecutorService` para checks de heartbeat y timeouts.
  - `BlockingQueue<Task>` para cola principal.
  - `synchronized` o `Atomic*` para actualizar cargas worker.
- **Worker**
  - `ExecutorService workerPool = Executors.newFixedThreadPool(cores)` para ejecutar tareas en paralelo.
  - Un hilo para lectura de socket y otro para envío de heartbeats.
- **Cliente**
  - Bloqueante: envía y espera; no necesario multihilo.

## Algoritmos de balanceo

- **Preferido**: Least-loaded.
  - Seleccionar worker con `currentTasks < maxTasks` y mínimo `currentTasks/maxTasks`.
- **Alternativo**: Round-Robin simple.
- **Fallback**: Si ningún worker disponible, encolar y esperar.

# **Dominio cerrado de tareas**

## Catálogo de posibles tareas soportadas

1. PRIME_TEST: comprueba si un número grande es primo.
2. PRIME_RANGE: calcula todos los primos entre dos números.
3. FACTORIZE: devuelve los factores primos de un número.
4. HASH_STRESS: aplica funciones hash repetidas veces sobre una cadena.
5. SORT_RANDOM: genera una lista de números aleatorios y los ordena.
6. PI_ESTIMATION: estima el valor de π por el método Monte Carlo.

MATRIX_MULT: multiplica dos matrices cuadradas de tamaño N×N.

### POSIBLE AMPLIACIÓN PARA LAS TAREAS

Para abstraer las tareas en contraposición a un catálogo cerrado de tipos de tarea:

- **Carga dinámica de código**: El master puede enviar código serializado (`.class` o Java source string)

![Diseño](diagram.png)

