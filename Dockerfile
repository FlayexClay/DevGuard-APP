FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
# Capa de dependencias separada: cambiar codigo no reinstala el mundo
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests

# Etapa de runtime
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S devguard && adduser -S devguard -S devguard
WORKDIR /app
COPY --from=build /build/target/*.jar app.jar
USER devguard
EXPOSE 8080
# JAVA_OPTS llega desde el compose: el heap se define ahi, no aqui
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]