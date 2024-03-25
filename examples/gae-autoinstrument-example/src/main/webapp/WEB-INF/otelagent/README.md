## Source of these agents
- The [opentelemetry-javaagent](opentelemetry-javaagent.jar) is downloaded from [Releases](https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases) section of [opentelemetry-java-instrumentation](https://github.com/open-telemetry/opentelemetry-java-instrumentation).
  - This is the Java agent that automatically instruments the application.
- The [google-exporter-auto](google-exporter-auto.jar) is a shadowJar/fatJar that is downloaded from [exporter-auto-0.25.0-alpha](https://repo1.maven.org/maven2/com/google/cloud/opentelemetry/exporter-auto/0.25.0-alpha/exporter-auto-0.25.0-alpha-sources.jar).
  - This is the agent that autoconfigures the application to export telemetry to Google Cloud.
  - Updated versions can be browsed at [Maven Central](https://search.maven.org/artifact/com.google.cloud.opentelemetry/exporter-auto/0.25.0-alpha/jar) page.
