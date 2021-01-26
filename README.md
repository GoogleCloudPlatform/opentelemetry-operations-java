# Open-Telemetry Operations Exporters for Java

[![Build Status][circleci-image]][circleci-url]

Provides OpenTelemetry Exporters for Google Cloud Operations. 

[circleci-image]: https://circleci.com/gh/GoogleCloudPlatform/opentelemetry-operations-java.svg?style=shield 
[circleci-url]: https://circleci.com/gh/GoogleCloudPlatform/opentelemetry-operations-java



## Building

This project requires a mock server for Google Cloud APIs.  To build and test, do the following:

```
$ source get_mock_server.sh
$ ./gradlew test -Dmock.server.path=$MOCKSERVER
```


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