# Auto-Instrumentation for OpenTelemetry in Google Cloud

[![Maven Central][maven-image]][maven-url]

## Setup

To instrument metrics and traces using the `opentelemetry-javaagent`, `opentelemetry-operations-java-auto-<version>.jar` can be used to provide opentelemetry exporters.

```
java -javaagent:path/to/opentelemetry-javaagent-<version>-all.jar \
     -Dotel.javaagent.experimental.exporter.jar=path/to/opentelemetry-operations-java-auto-<version>.jar \
     -jar myapp.jar
```


*NOTE: The use of custom exporter in Java auto instrumentation is still undergoing a lot of churn.  Until it stabilizes, this module is provided for proof-of-concept work.*

[maven-image]: https://maven-badges.herokuapp.com/maven-central/com.google.cloud.opentelemetry/exporter-auto/badge.svg
[maven-url]: https://maven-badges.herokuapp.com/maven-central/com.google.cloud.opentelemetry/exporter-auto
