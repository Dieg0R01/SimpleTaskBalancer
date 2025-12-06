# Tests de Integración y Arquitectura Docker

## Resumen Ejecutivo

Este documento describe el comportamiento de los tests de integración y cómo está organizada la arquitectura Docker en el proyecto SimpleTaskBalancer. El sistema utiliza **dos enfoques diferentes** para Docker: uno para la aplicación en producción/desarrollo (`docker-compose.yml`) y otro para los tests automatizados (Testcontainers con `ImageFromDockerfile`). Ambos comparten el mismo `Dockerfile` base pero difieren en su gestión y propósito.

---

## Arquitectura Docker: Dos Sistemas Paralelos

### 1. Docker Compose (Aplicación)

**Ubicación:** `docker-compose.yml`  
**Propósito:** Ejecución manual del sistema completo para desarrollo y pruebas manuales  
**Uso:** `docker-compose up`

#### Características:

- **Construcción de imagen:** Usa `build: .` que construye la imagen desde el `Dockerfile` usando Docker CLI estándar
- **Redes múltiples:** Implementa una arquitectura de múltiples subredes:
  - `master-network` (192.168.1.0/24): Red principal
  - `worker-network-1` (192.168.2.0/24): Subred para Worker 1
  - `worker-network-2` (192.168.3.0/24): Subred para Worker 2
  - `worker-network-3` (192.168.4.0/24): Subred para Worker 3
  - `client-network` (192.168.5.0/24): Subred para Clientes

- **Servicios definidos:**
  - `master`: 1 instancia, puerto 8080 expuesto al host
  - `worker-1` a `worker-3`: Workers básicos
  - `worker-4` y `worker-5`: Workers adicionales (perfiles: `load-test`, `scalability`)
  - `client`: Cliente de prueba (perfil: `client`)

- **Healthchecks:** Cada servicio tiene healthchecks configurados
- **Persistencia:** Los contenedores persisten hasta que se detengan manualmente
- **Gestión:** Controlada por el usuario mediante comandos Docker Compose

#### Ejemplo de uso:

```bash
# Levantar sistema básico (master + 3 workers)
docker-compose up

# Levantar con workers adicionales
docker-compose --profile load-test up

# Levantar con cliente
docker-compose --profile client up
```

---

### 2. Testcontainers (Tests Automatizados)

**Ubicación:** `src/test/java/com/taskbalancer/integration/*.java`  
**Propósito:** Tests automatizados de integración  
**Uso:** `mvn test`

#### Características:

- **Construcción de imagen:** Usa `ImageFromDockerfile` de Testcontainers que construye la imagen directamente desde el `Dockerfile` durante la ejecución de tests
- **Red única:** Cada test crea su propia red Docker aislada usando `Network.newNetwork()`
- **Gestión automática:** Testcontainers gestiona el ciclo de vida completo:
  - Construcción de imagen antes de los tests
  - Creación de contenedores en `@BeforeAll`
  - Limpieza automática en `@AfterAll`
  - Eliminación de contenedores y redes al finalizar

- **Aislamiento:** Cada clase de test tiene su propio conjunto de contenedores independientes
- **Sin persistencia:** Los contenedores se eliminan automáticamente después de cada ejecución de tests

#### Ventajas de este enfoque:

1. **Evita problemas de resolución de imágenes:** En macOS, Docker CLI y docker-java (usado por Testcontainers) pueden tener problemas para encontrar imágenes locales. Al construir la imagen directamente con `ImageFromDockerfile`, se evita este problema.

2. **Aislamiento completo:** Cada test ejecuta en su propio entorno limpio sin interferencias.

3. **Reproducibilidad:** Los tests siempre usan la versión más reciente del código compilado.

---

## Dockerfile Compartido

**Ubicación:** `Dockerfile`  
**Uso:** Compartido por ambos sistemas

```dockerfile
FROM eclipse-temurin:21-jdk

WORKDIR /app

# Copiar código compilado
COPY bin/ bin/
COPY pom.xml pom.xml

# La imagen contiene todo el código compilado
# El comando se especifica en docker-compose.yml o en los tests
```

### Características:

- **Base:** `eclipse-temurin:21-jdk` (Java 21)
- **Contenido:** Solo el código compilado (`bin/`) y `pom.xml`
- **Comando:** Se especifica externamente (en `docker-compose.yml` o en los tests)
- **Flexibilidad:** La misma imagen puede ejecutar Master, Workers o Cliente según el comando

---

## Tests de Integración: Comportamiento Detallado

### Estructura General

Todos los tests de integración siguen el mismo patrón:

1. **`@BeforeAll`:** Configuración inicial
   - Crear red Docker
   - Construir imagen usando `ImageFromDockerfile`
   - Crear y iniciar contenedores (Master + Workers)
   - Esperar inicialización (5 segundos)

2. **Tests:** Ejecución de pruebas específicas

3. **`@AfterAll`:** Limpieza
   - Detener todos los contenedores
   - Cerrar la red Docker

### Tests Disponibles

#### 1. DistributedSystemTest

**Propósito:** Verificar funcionamiento básico del sistema distribuido

**Configuración:**
- 1 Master (puerto 8080 expuesto)
- 2 Workers (worker-1, worker-2)

**Tests:**
- `testMasterWorkerConnection()`: Verifica que todos los contenedores están corriendo
- `testTaskExecution()`: Ejecuta una tarea simple y verifica el resultado
- `testMultipleTaskTypes()`: Prueba diferentes tipos de tareas (PRIME_TEST, PRIME_RANGE, FACTORIZE)

**Tiempo de ejecución:** ~17 segundos

---

#### 2. ConcurrencyTest

**Propósito:** Verificar manejo de concurrencia y ausencia de race conditions

**Configuración:**
- 1 Master
- 3 Workers

**Tests:**
- `testConcurrentTaskSubmission()`: 
  - 10 threads, cada uno envía 5 tareas (50 tareas totales)
  - Verifica que al menos 70% se completen exitosamente
  - Tiempo máximo: 60 segundos por tarea

- `testNoRaceConditions()`:
  - 20 tareas con IDs únicos enviadas concurrentemente
  - Verifica que cada resultado corresponde a su tarea
  - Verifica que al menos 50% se completen correctamente

**Tiempo de ejecución:** ~27 segundos

---

#### 3. LoadBalancingTest

**Propósito:** Verificar distribución de carga entre workers

**Configuración:**
- 1 Master
- 3 Workers

**Tests:**
- `testLoadDistribution()`:
  - Envía 20 tareas concurrentemente
  - Verifica que al menos 15 se completen exitosamente
  - Tiempo máximo: 30 segundos por tarea

- `testMultipleClients()`:
  - Simula 5 clientes concurrentes
  - Cada cliente envía 5 tareas (25 tareas totales)
  - Verifica que al menos 20 se completen exitosamente

**Tiempo de ejecución:** ~23 segundos

---

#### 4. ResilienceTest

**Propósito:** Verificar recuperación del sistema ante fallos

**Configuración:**
- 1 Master
- 3 Workers

**Tests:**
- `testSystemContinuesAfterWorkerFailure()`:
  1. Envía 5 tareas iniciales
  2. Detiene un worker después de 2 segundos
  3. Espera a que las tareas iniciales se completen
  4. Envía 5 tareas adicionales después del fallo
  5. Verifica que al menos 3 de las nuevas tareas se completen

- `testMasterRemainsAvailable()`:
  - Verifica que el Master sigue respondiendo después de fallos
  - Envía una tarea de prueba

**Tiempo de ejecución:** ~11 segundos

---

## Diferencias Clave Entre los Dos Sistemas

| Aspecto | Docker Compose | Testcontainers |
|---------|----------------|----------------|
| **Construcción de imagen** | Docker CLI (`docker build`) | Testcontainers (`ImageFromDockerfile`) |
| **Gestión de redes** | Múltiples subredes predefinidas | Red única por test |
| **Ciclo de vida** | Manual (usuario controla) | Automático (JUnit gestiona) |
| **Persistencia** | Contenedores persisten | Contenedores efímeros |
| **Aislamiento** | Compartido entre ejecuciones | Aislado por test |
| **Propósito** | Desarrollo/Producción | Testing automatizado |
| **Configuración** | `docker-compose.yml` | Código Java en tests |
| **Limpieza** | Manual (`docker-compose down`) | Automática (`@AfterAll`) |

---

## Flujo de Ejecución de Tests

### 1. Compilación

```bash
mvn clean compile
```

- Compila el código Java a `bin/`
- Requisito previo para construir la imagen Docker

### 2. Ejecución de Tests

```bash
mvn test
```

**Proceso interno:**

1. **Compilación de tests:** `mvn test-compile`
2. **Para cada clase de test:**
   - `@BeforeAll` ejecuta:
     - Crea red Docker: `Network.newNetwork()`
     - Construye imagen: `new ImageFromDockerfile(...).withDockerfile(...)`
     - Crea contenedores: `new GenericContainer<>(image)`
     - Configura red, comandos, puertos
     - Inicia contenedores: `container.start()`
     - Espera inicialización: `Thread.sleep(5000)`
   
3. **Ejecución de tests:** Cada método `@Test` se ejecuta
   
4. **Limpieza:**
   - `@AfterAll` ejecuta:
     - Detiene contenedores: `container.stop()`
     - Cierra red: `network.close()`
     - Testcontainers elimina contenedores y redes automáticamente

### 3. Resultado

- **Tests unitarios:** 12 tests (sin Docker)
- **Tests de integración:** 9 tests (con Docker)
- **Total:** 21 tests
- **Tiempo total:** ~80 segundos (incluyendo construcción de imágenes)

---

## Ventajas del Enfoque Actual

### 1. Separación de Responsabilidades

- **Docker Compose:** Para desarrollo manual y demostraciones
- **Testcontainers:** Para validación automatizada continua

### 2. Aislamiento de Tests

Cada test ejecuta en su propio entorno limpio, evitando:
- Contaminación entre tests
- Dependencias de estado previo
- Problemas de limpieza

### 3. Reproducibilidad

Los tests siempre usan:
- La versión más reciente del código compilado
- Una imagen Docker construida desde cero
- Un entorno limpio y predecible

### 4. Compatibilidad con macOS

El uso de `ImageFromDockerfile` evita problemas conocidos de docker-java en macOS con imágenes locales.

---

## Consideraciones de Rendimiento

### Construcción de Imagen

- **Primera ejecución:** ~15-20 segundos (construcción completa)
- **Ejecuciones subsecuentes:** Testcontainers puede cachear la imagen si no hay cambios

### Tiempos de Ejecución

- **DistributedSystemTest:** ~17s
- **ConcurrencyTest:** ~27s
- **LoadBalancingTest:** ~23s
- **ResilienceTest:** ~11s

**Total:** ~80 segundos (sin contar construcción inicial)

---

## Mantenimiento

### Agregar un Nuevo Test de Integración

1. Crear nueva clase en `src/test/java/com/taskbalancer/integration/`
2. Anotar con `@Testcontainers`
3. Seguir el patrón estándar:
   ```java
   @BeforeAll
   public static void setUp() {
       network = Network.newNetwork();
       ImageFromDockerfile image = new ImageFromDockerfile(
           "taskbalancer-simpletaskbalancer:latest", false)
           .withDockerfile(Paths.get(System.getProperty("user.dir"), "Dockerfile"));
       
       // Crear contenedores...
   }
   
   @AfterAll
   public static void tearDown() {
       // Limpiar contenedores y red...
   }
   ```

### Modificar el Dockerfile

Cualquier cambio en `Dockerfile` afecta a ambos sistemas:
- Docker Compose reconstruirá la imagen en el próximo `docker-compose up --build`
- Testcontainers reconstruirá la imagen automáticamente en el próximo `mvn test`

---

## Conclusión

El proyecto SimpleTaskBalancer utiliza una arquitectura Docker dual que separa claramente:

1. **Sistema de aplicación (Docker Compose):** Para ejecución manual con configuración compleja de redes múltiples
2. **Sistema de tests (Testcontainers):** Para validación automatizada con aislamiento completo

Ambos sistemas comparten el mismo `Dockerfile` base, garantizando consistencia, pero difieren en su gestión y propósito. Esta separación proporciona flexibilidad para desarrollo manual mientras mantiene tests automatizados confiables y reproducibles.

