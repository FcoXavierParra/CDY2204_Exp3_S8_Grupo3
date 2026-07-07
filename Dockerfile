# =============================================================
#  Etapa 1: compilar con Maven
# =============================================================
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

# Pre-descargar dependencias (cache de capas Docker)
COPY pom.xml .
RUN apk add --no-cache maven && mvn dependency:go-offline -q

# Compilar omitiendo tests (los tests corren en CI antes de esta etapa)
COPY src ./src
RUN mvn clean package -DskipTests -q

# =============================================================
#  Etapa 2: imagen final liviana (solo JRE)
# =============================================================
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=build /app/target/gestion-pedidos-1.0.0.jar app.jar

EXPOSE 8080

# Healthcheck contra el endpoint publico /health
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD wget -q --spider http://localhost:8080/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
