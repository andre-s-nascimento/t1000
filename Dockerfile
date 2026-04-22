# Stage 1: Build
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app

# Passo 1: Copia apenas o wrapper do Gradle e definições de dependências
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle .

# Passo 2: Baixa o Gradle e as dependências (Camada pesada de cache)
# Usamos o 'dependencies' ou apenas checamos a versão para forçar o download
RUN ./gradlew --version
RUN ./gradlew dependencies --no-daemon

# Passo 3: Agora sim copia o código-fonte (Camada que muda sempre)
COPY src src

# Passo 4: Build do JAR (já com as dependências no cache do Docker)
RUN ./gradlew bootJar --no-daemon -x test

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Instala FFMPEG e limpa o cache do apt para reduzir o tamanho da imagem
RUN apt-get update && \
    apt-get install -y ffmpeg ca-certificates && \
    rm -rf /var/lib/apt/lists/*

# Copia o JAR do estágio anterior
COPY --from=build /app/build/libs/*.jar app.jar

# Otimização para Java 21 (Virtual Threads e Performance)
ENTRYPOINT ["java", \
    "-XX:+UseZGC", \
    "-XX:+ZGenerational", \
    "-Dspring.threads.virtual.enabled=true", \
    "-jar", "app.jar"]