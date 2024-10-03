# Open-Telemetry Operations Exporters for Java

[![Maven Central][maven-image]][maven-url]

Provides OpenTelemetry Exporters for Google Cloud Operations. 

To get started with instrumentation in Google Cloud, see [Generate traces and metrics with
Java](https://cloud.google.com/stackdriver/docs/instrumentation/setup/java).

To learn more about instrumentation and observability, including opinionated recommendations
for Google Cloud Observability, visit [Instrumentation and
observability](https://cloud.google.com/stackdriver/docs/instrumentation/overview).

## Building

> [!IMPORTANT]
> This project requires Java 11 to build and test. All artifacts published from this project support Java 8 or higher, unless otherwise noted.

This project requires a mock server for Google Cloud APIs. To build and test, do the following:

```
$ ./gradlew test
```

Note: This project uses [Test Containers](http://testcontainers.org), which requires
docker to be runnable locally by the current users.  Please verify `docker run hello-world` works, and if not configure your local docker before building.


## Contributing

See [contributing guide](docs/contributing.md).


### Enforcing Style

This project uses the spotless plugin to enforce style.  You can automatically correct any issues by running:

```
$ ./gradlew spotlessApply
```


## Google Cloud Trace Exporter

See [Tracing Readme](exporters/trace/README.md) for installation and usage instructions.

## Google Cloud Monitoring Exporter

See [Metrics Readme](exporters/metrics/README.md) for installation and usage instructions.

## Google Cloud Autoconfigure module

*Note: This is an alpha-release.*

See [Autoconfigure Readme](exporters/auto/README.md) for installation and usage instructions.


[maven-image]: https://img.shields.io/maven-central/v/com.google.cloud.opentelemetry/exporter-trace?color=dark-green
[maven-url]: https://central.sonatype.com/artifact/com.google.cloud.opentelemetry/exporter-trace
