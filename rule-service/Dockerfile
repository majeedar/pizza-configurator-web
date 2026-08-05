FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY common-contracts/pom.xml common-contracts/
COPY catalog-service/pom.xml catalog-service/
COPY pricing-service/pom.xml pricing-service/
COPY rule-service/pom.xml rule-service/
COPY order-service/pom.xml order-service/
COPY gateway/pom.xml gateway/
COPY ai-parser-service/pom.xml ai-parser-service/
COPY kitchen-service/pom.xml kitchen-service/
COPY notification-service/pom.xml notification-service/
COPY admin-service/pom.xml admin-service/
RUN mvn -q -pl rule-service -am dependency:go-offline
COPY common-contracts common-contracts/
COPY rule-service rule-service/
RUN mvn -q -pl rule-service -am package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/rule-service/target/rule-service.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
