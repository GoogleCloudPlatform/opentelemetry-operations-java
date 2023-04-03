FROM eclipse-temurin:11
ARG project_id
ARG credentials_location
ENV GOOGLE_CLOUD_PROJECT $project_id
ENV GOOGLE_APPLICATION_CREDENTIALS $credentials_location
RUN mkdir /opt/app
COPY ./build/libs/examples-autoinstrument-docker-0.0.1-SNAPSHOT.jar /opt/app/example.jar
RUN wget https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar
RUN mv opentelemetry-javaagent.jar /opt/app/otel-agent.jar
ENTRYPOINT ["java", "-javaagent:/opt/app/otel-agent.jar", "-Dotel.javaagent.experimental.extensions=/exporter-auto.jar", "-Dotel.traces.exporter=google_cloud_trace", "-Dotel.metrics.exporter=google_cloud_monitoring", "-jar", "/opt/app/example.jar"]
