## Pruebas automatizadas (JUnit 5 + Mockito) en SimpleTaskBalancer

- Objetivo: validar el funcionamiento esencial del sistema Master–Worker con una batería mínima pero significativa de tests.
- Alcance: cubrimos los caminos felices y los puntos críticos (colas, registro de workers, ejecución de tareas y validaciones). Evitamos E2E con sockets para mantener la suite rápida y estable.

Tecnologías y por qué
- JUnit 5 (Jupiter): API moderna, aserciones claras, integración nativa con Maven Surefire.
- Mockito: crear dobles (mocks/stubs/spies) de `TaskHandler` sin añadir complejidad al código de producción.
- Maven Surefire Plugin: ejecución de tests vía `mvn test`, integración con IDEs (IntelliJ / VS Code) y pipelines CI.

Estructura de tests
- Carpeta: `src/test/java` replicando el paquete `com.taskbalancer.*`.
- Dependencias de test en `pom.xml`: `junit-jupiter-api`, `junit-jupiter-engine`, `mockito-core`, `mockito-junit-jupiter`, y `maven-surefire-plugin` 3.x.
- Estilo: pruebas unitarias concisas, con aserciones explícitas y uso de Mockito solo cuando aporta valor.

Casos cubiertos (mínimos pero suficientes)
1) TaskExecutorTest
   - ejecutaHandlerRegistrado_yDevuelveResultadoExitoso
     - Crea un `Task` de tipo `PRIME_TEST` con parámetro válido.
     - Verifica: `success=true`, `taskId` propagado, `data` booleana verdadera, `error` vacío.
     - Justificación: valida el flujo “happy path” de delegación a un handler real.
   - retornaErrorSiNoExisteHandlerParaTipo
     - Envía un `Task` con `taskType` desconocido.
     - Verifica: `success=false` y mensaje de error con “Tipo de tarea desconocido”.
     - Justificación: cubre el control de errores cuando no hay handler disponible.
   - manejaExcepcionDeHandler_lanzaExcepcionDelHandler
     - Inyecta (vía reflexión) un `TaskHandler` mock que lanza `RuntimeException` al ejecutar.
     - Verifica: `executeTask` propaga la excepción y que el mock fue invocado.
     - Justificación: documenta el comportamiento actual ante fallos del handler (propagación), importante para entender responsabilidades y endurecer el contrato.

2) TaskQueueTest
   - encolaYDesencolaEnOrdenFIFO
     - Encola dos tareas y comprueba que `dequeue()` respeta el orden de llegada.
     - Justificación: garantiza semántica FIFO del `LinkedBlockingQueue` envuelta por `TaskQueue`.
   - dequeueConTimeoutRetornaNullSiVacio
     - Verifica que `dequeue(timeout)` devuelve `null` si no hay elementos.
     - Justificación: contrato de temporización para consumidores no bloqueantes.

3) WorkerRegistryTest
   - registra_y_recupera_workers_y_actualiza_heartbeat
     - `registerWorker`/`getWorker`/`getAllWorkers`/`getAvailableWorkers`/`getActiveWorkerCount` + `updateWorkerHeartbeat` (incluye `currentTasks`).
     - Justificación: asegura la base de la contabilidad y disponibilidad de workers.
   - expira_worker_por_timeout
     - Simula paso del tiempo con `Thread.sleep` y valida que `checkTimeouts` marca inactivo.
     - Justificación: timeout correcto es clave para resiliencia del balanceador.
   - desregistra_worker
     - `unregisterWorker` elimina y no es recuperable.
     - Justificación: limpieza consistente del registro.

4) PrimeTestTaskTest
   - devuelveTrueParaPrimos_yFalseParaNoPrimos
     - Verifica 17 (primo) → true, 15 (no primo) → false.
     - Justificación: exactitud de lógica aritmética núcleo.
   - errorCuandoFaltaParametro
     - Sin `number` en parámetros → `success=false` y mensaje de error.
     - Justificación: validación de entrada.

5) MatrixMultTaskTest
   - tamañoValido_devuelveExitoYResumen
     - `size=2` → `success=true` y mensaje con “Matriz 2x2 … Elemento [0][0] = …”.
     - Justificación: ejecución nominal sin necesidad de fijar semilla de aleatoriedad.
   - tamañoExcedeMaximo_devuelveError
     - `size=1000` → `success=false` y mensaje con “Tamaño máximo”.
     - Justificación: control de parámetros y protección de recursos.

Por qué esta batería es suficiente ahora
- Cubre los invariantes críticos del dominio:
  - Delegación de ejecución de tareas (TaskExecutor) y manejo de tipos desconocidos/errores.
  - Orden y comportamiento temporal de la cola (TaskQueue).
  - Registro, disponibilidad y expiración de workers (WorkerRegistry).
  - Correctitud base de tareas algorítmicas representativas.
- Mantiene la suite rápida y estable (sin red/sockets, sin E2E pesados), ideal para integración continua.
- Deja espacio para extender con pruebas de integración cuando se añada lógica de red (`Master`, `Worker`, `Client`).

Cómo ejecutar los tests
- Desde CLI:
  ```bash
  mvn test
  ```
- Desde el IDE (IntelliJ/VS Code):
  - Asegura que el proyecto importa el `pom.xml`.
  - Ejecuta cada clase de test o el ciclo “Run tests” del proyecto.

Ubicación de los tests
- `src/test/java/com/taskbalancer/worker/TaskExecutorTest.java`
- `src/test/java/com/taskbalancer/master/TaskQueueTest.java`
- `src/test/java/com/taskbalancer/master/WorkerRegistryTest.java`
- `src/test/java/com/taskbalancer/tasks/PrimeTestTaskTest.java`
- `src/test/java/com/taskbalancer/tasks/MatrixMultTaskTest.java`

Notas
- No se mockean tareas “puras” (p. ej. `PrimeTestTask`) para mantener pruebas más realistas.
- En `TaskExecutorTest`, se usa Mockito solo para simular un handler que lanza excepción y verificar la invocación.
- Si en el futuro se cambia el contrato de `TaskExecutor` para capturar excepciones y devolver `Result` de error, se ajustará el test correspondiente.

