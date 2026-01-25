FROM eclipse-temurin:21-jre
WORKDIR /app
COPY build/bill-assistant.jar app.jar
ENTRYPOINT ["java","-jar","/app/app.jar"]
