# Stage 1: Build
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app
COPY . .
RUN chmod +x gradlew
RUN ./gradlew bootJar --no-daemon

# Stage 2: Runtime (Forçando Ubuntu Jammy para ter o apt-get)
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Instala FFMPEG e certificados (importante para chamadas HTTPS das APIs)
RUN apt-get update && \
    apt-get install -y ffmpeg ca-certificates && \
    rm -rf /var/lib/apt/lists/*

COPY --from=build /app/build/libs/*.jar app.jar

# Se você criou o AppConfig.java para as Virtual Threads, o Spring vai rodar liso
ENTRYPOINT ["java", "-jar", "app.jar"]