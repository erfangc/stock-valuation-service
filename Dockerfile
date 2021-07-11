FROM openjdk:11

WORKDIR /app
COPY target/stock-valuation-service-0.0.1-SNAPSHOT.jar /app

CMD ["java", "-jar", "stock-valuation-service-0.0.1-SNAPSHOT.jar"]
