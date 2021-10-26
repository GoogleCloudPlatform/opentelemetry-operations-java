# Auto-Configuration for OpenTelemetry in Google Cloud

[![Maven Central][maven-image]][maven-url]

## SDK AutoConfiguration

OpenTelemetry Java SDK provides an `autoconfigure` module where all configuration can be done entirely through Java system properties or environment variables.

To leverage the GCP extensions to this, simply add the following dependency:

```
<dependency>
  <groupId>com.google.cloud.opentelemetry</groupId>
  <artifactId>exporter-auto</artifactId>
  <version>0.18.0-alpha</version>
</dependency>
```

*Note: Make sure to use the latest release [![LatestRelease][maven-image]][maven-url].*

Then you can configure your sdk using the following system properties/environmnet variables:

| Property | Environment Variable | Value | Description |
| -------- | -------------------- | ----- | ----------- |
| otel.traces.exporter | N/A | google_cloud_trace | The exporter for traces. |
| otel.metrics.exporter | N/A | google_cloud_monitoring | The exporter for metrics. |
| GOOGLE_CLOUD_PROJECT | GOOGLE_CLOUD_PROJECT | autodiscovered | The project_id to report metrics/traces againt. |
| GOOGLE_APPLICATION_CREDENTIALS | GOOGLE_APPLICATION_CREDENTIALS | autodiscovered | Credentials to use when talking to GCP APIs. |

*See the [autoconfigure][autooconf] SDK module for general environmental setup configuration.*


## AutoInstrumentation Setup

You can use the auto-configuration jar as an [Extension][auto-extensions] to the Java auto instrumentation agent.

To instrument metrics and traces using the `opentelemetry-javaagent`, `opentelemetry-operations-java-auto-<version>.jar` can be used to provide opentelemetry exporters.

```
java -javaagent:path/to/opentelemetry-javaagent-<version>-all.jar \
     -Dotel.javaagent.experimental.extensions=path/to/opentelemetry-operations-java-auto-<version>.jar \
     -Dotel.traces.exporter=google_cloud_trace \
     -Dotel.metrics.exporter=google_cloud_monitoring \
     -jar myapp.jar
```


*NOTE: The use of custom exporter in Java auto instrumentation is still undergoing a lot of churn.  Until it stabilizes, this module is provided for proof-of-concept work.*

[autooconf]: https://github.com/open-telemetry/opentelemetry-java/tree/main/sdk-extensions/autoconfigure
[auto-extensions]: https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/examples/extension/README.md
[maven-image]: https://maven-badges.herokuapp.com/maven-central/com.google.cloud.opentelemetry/exporter-auto/badge.svg
[maven-url]: https://maven-badges.herokuapp.com/maven-central/com.google.cloud.opentelemetry/exporter-auto
