# Stage 1: Build
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app

# Cache do Gradle (ESSENCIAL)
ENV GRADLE_USER_HOME=/gradle-cache

# Copia apenas o necessário primeiro (melhor cache)
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./

# Baixa dependências (cacheável)
RUN ./gradlew build -x test --parallel --build-cache || true

# Agora copia o código
COPY src src

# Build final (rápido porque já cacheou)
RUN ./gradlew bootJar -x test --parallel --build-cache

# Stage 2: Runtime (menor possível)
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

RUN apt-get update && \
    apt-get install -y --no-install-recommends ffmpeg ca-certificates && \
    rm -rf /var/lib/apt/lists/*

COPY --from=build /app/build/libs/*.jar app.jar

# ENTRYPOINT corrigido em linha única para evitar erro de parser
ENTRYPOINT ["java", "-XX:+UseSerialGC", "-Xms128m", "-Xmx512m", "-Dspring.threads.virtual.enabled=true", "-jar", "app.jar"]