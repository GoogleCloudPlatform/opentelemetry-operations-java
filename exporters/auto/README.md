# Auto-Coonfiguration for OpenTelemetry in Google Cloud

[![Maven Central][maven-image]][maven-url]

## SDK AutoConfiguration

TODO:

- Define how to depend on OTEL sdk-autoconfigure *and* this package.
- Just use global OTEL without any other setup
- Link to configurable JVM/ENV parameters

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
