package com.taskbalancer.integration;

import org.testcontainers.images.ImagePullPolicy;
import org.testcontainers.utility.DockerImageName;

/**
 * Política de pull que nunca intenta hacer pull de imágenes,
 * asumiendo que las imágenes ya están disponibles localmente.
 * Esta es la forma estándar de usar imágenes locales con Testcontainers.
 */
public class LocalImagePullPolicy implements ImagePullPolicy {
    
    @Override
    public boolean shouldPull(DockerImageName imageName) {
        // Nunca hacer pull, usar imagen local
        return false;
    }
    
    public static ImagePullPolicy never() {
        return new LocalImagePullPolicy();
    }
}





