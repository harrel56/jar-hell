FROM eclipse-temurin:23-alpine
ARG BUILD_VERSION=unknown
ENV BUILD_VERSION=${BUILD_VERSION}
COPY build/*.jar app.jar

EXPOSE 8060
HEALTHCHECK CMD wget --no-verbose --tries=1 --spider http://localhost:8060 || exit 1
ENTRYPOINT ["java", "-jar", "--enable-preview", "/app.jar"]