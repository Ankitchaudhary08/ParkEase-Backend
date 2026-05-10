FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY eureka-server/pom.xml eureka-server/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY auth-service/pom.xml auth-service/pom.xml
COPY parking-service/pom.xml parking-service/pom.xml
COPY booking-vehicle-service/pom.xml booking-vehicle-service/pom.xml
COPY payment-service/pom.xml payment-service/pom.xml
COPY notification-analytics-service/pom.xml notification-analytics-service/pom.xml
COPY api-gateway/src api-gateway/src
RUN apk add --no-cache maven && mvn -pl api-gateway package -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/api-gateway/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
