# Open-Telemetry Operations Exporters for Java

[![Maven Central][maven-image]][maven-url]

Provides OpenTelemetry Exporters for Google Cloud Operations. 

## Building

This project requires a mock server for Google Cloud APIs.  To build and test, do the following:

```
$ ./gradlew test
```

Note: This project uses [Test Containers](http://testcontainers.org), which requires
docker to be runnable locally by the current users.  Please verify `docker run hello-world` works, and if not configure your local docker before building.


## Contributing

TODO(jsuereth): Add full contributing section.


### Enforcing Style

This project uses the spotless plugin to enforce style.  You can automatically correct any issues by running:

```
$ ./gradlew spotlessApply
```


## Google Cloud Trace Exporter

See [Tracing Readme](exporters/trace/README.md) for installation and usage instructions.

## Google Cloud Monitoring Exporter

*Note: This is an alpha-release.*

See [Metrics Readme](exporters/metrics/README.md) for installation and usage instructions.



[maven-image]: https://maven-badges.herokuapp.com/maven-central/com.google.cloud.opentelemetry/exporter-trace/badge.svg
[maven-url]: https://maven-badges.herokuapp.com/maven-central/com.google.cloud.opentelemetry/exporter-trace