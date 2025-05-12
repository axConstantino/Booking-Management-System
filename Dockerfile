FROM eclipse-temurin:17.0.11_9-jdk-jammy AS builder

WORKDIR /app
COPY pom.xml ./
COPY mvnw mvnw
COPY .mvn .mvn
COPY src/ src/

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

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-Dspring.profiles.active=prod", "-Djava.security.egd=file:/dev/./urandom", "-Dfile.encoding=UTF-8", "-jar", "app.jar"]

