FROM eclipse-temurin:23-alpine
ARG BUILD_VERSION=unknown
ENV BUILD_VERSION=${BUILD_VERSION}
COPY build/*.jar app.jar

EXPOSE 8060
HEALTHCHECK CMD wget --timeout=5 --no-verbose --tries=1 --spider http://localhost:8060 || exit 1
ENTRYPOINT ["java", "-jar", "--enable-preview", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8000", "/app.jar"]