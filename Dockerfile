FROM eclipse-temurin:23-alpine
COPY build/*.jar app.jar

EXPOSE 8060
ENTRYPOINT ["java", "-jar", "--enable-preview", "/app.jar"]