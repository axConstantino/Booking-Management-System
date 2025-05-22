FROM eclipse-temurin:17.0.11_9-jdk-jammy AS builder
WORKDIR /app

RUN apt-get update && \
    apt-get install -y --no-install-recommends bash dos2unix && \
    rm -rf /var/lib/apt/lists/*

COPY pom.xml mvnw ./
COPY .mvn .mvn
COPY src/ src/

RUN dos2unix mvnw && chmod +x mvnw

RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw dependency:resolve-plugins dependency:go-offline

RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:17.0.11_9-jre-jammy
WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

RUN addgroup --system javagroup && \
    adduser --system --ingroup javagroup javauser && \
    chown -R javauser:javagroup /app

USER javauser
EXPOSE 8080

ENTRYPOINT ["java","-XX:MaxRAMPercentage=75.0","-Dspring.profiles.active=prod","-Djava.security.egd=file:/dev/./urandom","-Dfile.encoding=UTF-8","-jar","app.jar"]

