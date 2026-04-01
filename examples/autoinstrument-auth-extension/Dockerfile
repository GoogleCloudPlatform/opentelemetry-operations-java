# Stage 1: Download Agent and Extensions
FROM eclipse-temurin:17-jdk-jammy AS tools
RUN apt-get update && apt-get install -y wget
RUN wget -O /opentelemetry-javaagent.jar https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.26.1/opentelemetry-javaagent.jar
RUN wget -O /gcp-auth-extension.jar https://repo1.maven.org/maven2/io/opentelemetry/contrib/opentelemetry-gcp-auth-extension/1.54.0-alpha/opentelemetry-gcp-auth-extension-1.54.0-alpha-shadow.jar

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Copy jars from tools
COPY --from=tools /opentelemetry-javaagent.jar /app/opentelemetry-javaagent.jar
COPY --from=tools /gcp-auth-extension.jar /app/gcp-auth-extension.jar

COPY build/install/autoinstrument-auth-extension /app

# Hardcode all configuration in ENTRYPOINT
# TODO: cloud.region should be picked up by the resource detector
ENTRYPOINT ["java", \
  "-javaagent:/app/opentelemetry-javaagent.jar", \
  "-Dotel.javaagent.extensions=/app/gcp-auth-extension.jar", \
  "-Dotel.traces.exporter=otlp", \
  "-Dotel.metrics.exporter=otlp", \
  "-Dotel.resource.attributes=cloud.region=us-central1", \
  "-Dotel.exporter.otlp.endpoint=https://telemetry.googleapis.com", \
  "-Dotel.exporter.otlp.protocol=grpc", \
  "-Dotel.service.name=autoinstrument-auth-extension-demo", \
  "-Dotel.resource.providers.gcp.enabled=true", \
  "-cp", "/app/lib/*", "com.example.App"]

EXPOSE 8080
