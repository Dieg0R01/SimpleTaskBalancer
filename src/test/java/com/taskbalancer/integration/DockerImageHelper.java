package com.taskbalancer.integration;

import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.DockerImageName;

import java.io.File;
import java.nio.file.Paths;

/**
 * Helper para asegurar que la imagen Docker esté disponible antes de ejecutar tests.
 * Usa ImageFromDockerfile para construir la imagen directamente con Testcontainers,
 * evitando problemas de resolución de imágenes locales en macOS.
 */
public class DockerImageHelper {

    private static final String IMAGE_NAME = "taskbalancer-simpletaskbalancer:latest";
    private static volatile ImageFromDockerfile cachedImage = null;

    /**
     * Retorna un ImageFromDockerfile que Testcontainers construirá directamente.
     * Esto evita problemas de resolución de imágenes locales en macOS.
     */
    public static DockerImageName ensureImageExists() {
        if (cachedImage == null) {
            synchronized (DockerImageHelper.class) {
                if (cachedImage == null) {
                    // Verificar que el Dockerfile existe
                    File dockerfile = new File("Dockerfile");
                    if (!dockerfile.exists()) {
                        throw new RuntimeException("Dockerfile no encontrado en: " + dockerfile.getAbsolutePath());
                    }

                    // Verificar que el código está compilado
                    File binDir = new File("bin");
                    if (!binDir.exists() || !binDir.isDirectory()) {
                        throw new RuntimeException("Directorio 'bin' no encontrado. " +
                            "Ejecuta 'mvn clean compile' primero.");
                    }

                    // Construir la imagen usando Testcontainers directamente
                    // Esto evita problemas de resolución de imágenes locales
                    System.out.println("Construyendo imagen usando Testcontainers ImageFromDockerfile...");
                    cachedImage = new ImageFromDockerfile(IMAGE_NAME, false)
                        .withDockerfile(Paths.get("Dockerfile"));
                }
            }
        }

        // Retornar el DockerImageName de la imagen que se construirá
        return DockerImageName.parse(IMAGE_NAME);
    }
}
