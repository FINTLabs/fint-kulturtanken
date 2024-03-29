FROM gradle:7.3-jdk17-alpine as builder
USER root
COPY . .
RUN gradle --no-daemon build

FROM gcr.io/distroless/java17
ENV JAVA_TOOL_OPTIONS -XX:+ExitOnOutOfMemoryError
COPY --from=builder /home/gradle/build/libs/fint-kulturtanken-*.jar /data/fint-kulturtanken.jar
CMD ["/data/fint-kulturtanken.jar"]