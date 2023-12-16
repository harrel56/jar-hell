FROM bellsoft/liberica-openjdk-alpine:21
COPY build/*.jar app.jar

EXPOSE 8060
ENTRYPOINT ["java", "-jar", "/app.jar"]