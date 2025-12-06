FROM eclipse-temurin:21-jdk

WORKDIR /app

# Copiar código compilado
COPY bin/ bin/
COPY pom.xml pom.xml

# La imagen contiene todo el código compilado
# El comando se especifica en docker-compose.yml
