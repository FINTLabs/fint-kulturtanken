FROM gradle:8.14.3-jdk17-alpine AS builder
USER root
COPY . .
RUN gradle --no-daemon build

FROM eclipse-temurin:17-jre
ENV JAVA_TOOL_OPTIONS="-XX:+ExitOnOutOfMemoryError"
COPY --from=builder /home/gradle/build/libs/fint-kulturtanken-*.jar /data/fint-kulturtanken.jar
ENTRYPOINT ["java", "-jar", "/data/fint-kulturtanken.jar"]