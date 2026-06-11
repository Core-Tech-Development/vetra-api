# Stage 1: Build nativo com Mandrel (GraalVM otimizada para Quarkus)
FROM quay.io/quarkus/ubi-quarkus-mandrel-builder-image:jdk-21 AS build
USER root
RUN microdnf install -y findutils
WORKDIR /app
RUN chown quarkus:quarkus /app
COPY --chown=quarkus:quarkus pom.xml .
COPY --chown=quarkus:quarkus .mvn .mvn
COPY --chown=quarkus:quarkus mvnw .
RUN chmod +x mvnw
USER quarkus
RUN ./mvnw dependency:go-offline -B
COPY --chown=quarkus:quarkus src src
RUN ./mvnw package -DskipTests -Dnative -B

# Stage 2: Runtime minimo com UBI micro
FROM quay.io/quarkus/quarkus-micro-image:2.0
WORKDIR /app
COPY --from=build /app/target/*-runner /app/application
EXPOSE 8080
ENTRYPOINT ["./application", "-Dquarkus.http.host=0.0.0.0"]
