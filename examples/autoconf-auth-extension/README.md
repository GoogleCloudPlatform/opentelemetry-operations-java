# OTLP Trace and Metrics with Google Auth Extension Example

This sample demonstrates how to use the [OpenTelemetry GCP Auth Extension](https://github.com/open-telemetry/opentelemetry-java-contrib/tree/main/gcp-auth-extension) to configure the OpenTelemetry SDK in a manually instrumented application and export telemetry to Google Cloud using OTLP exporters for both traces and metrics.

## Prerequisites

First, ensure you have Google Cloud credentials available on your machine:

```shell
gcloud auth application-default login
```

Executing this command will save your application credentials to the default path:
 - Linux, macOS: `$HOME/.config/gcloud/application_default_credentials.json`
 - Windows: `%APPDATA%\gcloud\application_default_credentials.json`

Next, set the `GOOGLE_CLOUD_PROJECT` environment variable to your GCP project ID:
```shell
export GOOGLE_CLOUD_PROJECT="your-gcp-project-id"
```
This environment variable is used by the GCP Auth Extension to identify the project.

## Running the Sample

To run the sample from the repository root, use the following Gradle command:

```shell
./gradlew :examples-autoconf-auth-extension:run
```

Running this sample will generate traces and metrics and export them to Google Cloud via OTLP.

## Configuration Details

The sample uses OpenTelemetry SDK Autoconfigure. The following system properties are configured in `build.gradle` and can be overridden or set as environment variables:

- `otel.exporter.otlp.endpoint`: Set to `https://telemetry.googleapis.com` for Google Cloud OTLP receiver.
- `otel.traces.exporter`: Set to `otlp` to use OTLP exporter for traces.
- `otel.metrics.exporter`: Set to `otlp` to use OTLP exporter for metrics.
- `otel.exporter.otlp.protocol`: Set to `http/protobuf` as required by Google Cloud OTLP receiver.
