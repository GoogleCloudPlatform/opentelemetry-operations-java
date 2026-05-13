# Migration Guide

This guide provides instructions on how to migrate from the custom exporters in this repository to the standard OpenTelemetry OTLP exporters.

## Overview
Google Cloud now supports native OTLP (OpenTelemetry Protocol) ingestion for Cloud Trace and Cloud Monitoring via the [Telemetry API](https://docs.cloud.google.com/stackdriver/docs/reference/telemetry/overview). This allows you to use the standard OpenTelemetry OTLP exporters for sending telemetry data to Google Cloud.

## Migrate from OpenTelemetry Google Cloud Trace Exporter to OTLP exporter

To migrate from the deprecated Google Cloud Trace exporter to the standard OpenTelemetry OTLP exporter, follow these steps:

#### 1. Add Dependencies

Add the following dependencies to your `build.gradle` file:

```groovy
implementation("io.opentelemetry:opentelemetry-exporter-otlp:1.56.0")
implementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure:1.56.0")
// Recommended for authentication when using autoconfigure module
implementation("io.opentelemetry.contrib:opentelemetry-gcp-auth-extension:1.52.0-alpha")
```

#### 2. Configure the SDK

Use the OpenTelemetry SDK Autoconfigure module to configure the SDK. You can set the following system properties or environment variables:

```bash
# System Properties
-Dotel.exporter.otlp.endpoint=https://telemetry.googleapis.com
-Dotel.traces.exporter=otlp
-Dotel.exporter.otlp.protocol=http/protobuf

# Or Environment Variables
OTEL_EXPORTER_OTLP_ENDPOINT=https://telemetry.googleapis.com
OTEL_TRACES_EXPORTER=otlp
OTEL_EXPORTER_OTLP_PROTOCOL=http/protobuf
```

#### 3. Follow the Migration Guide

For a code walkthrough, follow the migration guide published at [Migrate from the Trace exporter to the OTLP endpoint](https://docs.cloud.google.com/trace/docs/migrate-to-otlp-endpoints).

### Mapping and Limitations 

#### Configuration Mapping

The following table maps the configurations available in `TraceConfiguration` to their OTLP equivalents:

| TraceConfiguration Option | OTLP Equivalent Property / Env Var | Notes |
| :--- | :--- | :--- |
| `setProjectId(String)` | Use resource attribute: `gcp.project_id` | If using the `opentelemetry-gcp-auth-extension`, the project ID can be inferred from the credentials or the environment. |
| `setCredentials(Credentials)` | Pass the bearer token as Authorization Header in the exporter | Handled automatically by `opentelemetry-gcp-auth-extension`. |
| `setTraceServiceEndpoint(String)` | `otel.exporter.otlp.endpoint` / `OTEL_EXPORTER_OTLP_ENDPOINT` | Default is `https://telemetry.googleapis.com`. |
| `setFixedAttributes(Map)` | `otel.resource.attributes` / `OTEL_RESOURCE_ATTRIBUTES` | Maps to Resource attributes in OTel, which are added to all telemetry data, not just spans. |
| `setDeadline(Duration)` | `otel.exporter.otlp.timeout` / `OTEL_EXPORTER_OTLP_TIMEOUT` | Default is 10 seconds. |

#### Unsupported Features

The following features of the Google Cloud Trace exporter are not supported by the standard OTLP exporter:

*   **Attribute Mapping (`setAttributeMapping`)**: The OTLP exporter does not support renaming attributes (e.g., renaming OpenTelemetry standard attributes to legacy Stackdriver attributes). You should use standard OpenTelemetry attributes.
*   **Custom Trace Service Stub (`setTraceServiceStub`)**: You cannot pass a pre-configured `TraceServiceStub` to the OTLP exporter via configuration properties. If you need custom channel configuration, you must use programmatic configuration with `OtlpGrpcSpanExporter.builder()`.

#### Complete Sample

For a complete sample demonstrating how to export traces to Google Cloud using OTLP, see the [examples/otlptrace](examples/otlptrace) folder.

## Migrate from OpenTelemetry Google Cloud Monitoring Exporter to OTLP exporter

To migrate from the deprecated Google Cloud Monitoring exporter to the standard OpenTelemetry OTLP exporter, follow these steps:

#### 1. Add Dependencies

Add the following dependencies to your `build.gradle` file:

```groovy
implementation("io.opentelemetry:opentelemetry-exporter-otlp:1.56.0")
implementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure:1.56.0")
// Recommended for authentication when using autoconfigure module
implementation("io.opentelemetry.contrib:opentelemetry-gcp-auth-extension:1.52.0-alpha")
```

#### 2. Configure the SDK

Use the OpenTelemetry SDK Autoconfigure module to configure the SDK. You can set the following system properties or environment variables:

```bash
# System Properties
-Dotel.exporter.otlp.endpoint=https://telemetry.googleapis.com
-Dotel.metrics.exporter=otlp
-Dotel.exporter.otlp.protocol=http/protobuf

# Or Environment Variables
OTEL_EXPORTER_OTLP_ENDPOINT=https://telemetry.googleapis.com
OTEL_METRICS_EXPORTER=otlp
OTEL_EXPORTER_OTLP_PROTOCOL=http/protobuf
```

#### 3. Code Walkthrough

##### Legacy Configuration

In the legacy setup using `GoogleCloudMetricExporter`, you typically configured it using `MetricConfiguration` as shown in [MetricsExporterExample.java](file:///usr/local/google/home/sharmapranav/Projects/opentelemetry-operations-java/examples/metrics/src/main/java/com/google/cloud/opentelemetry/example/metrics/MetricsExporterExample.java):

```java
MetricConfiguration configuration = MetricConfiguration.builder()
    // Configure project ID, credentials, etc.
    .build();
MetricExporter metricExporter = GoogleCloudMetricExporter.createWithConfiguration(configuration);
```

##### OTLP Configuration

With the OTLP exporter, you can use the `AutoConfiguredOpenTelemetrySdk` to automatically pick up configuration from environment variables or system properties. If you need to manually add authorization headers (e.g., if not using the `gcp-auth-extension`), you can customize the exporter as shown in [OTLPMetricExample.java](file:///usr/local/google/home/sharmapranav/Projects/opentelemetry-operations-java/examples/otlpmetric/src/main/java/com/google/cloud/opentelemetry/example/otlpmetric/OTLPMetricExample.java):

```java
AutoConfiguredOpenTelemetrySdk autoConfOTelSdk =
    AutoConfiguredOpenTelemetrySdk.builder()
        .addMetricExporterCustomizer(
            (exporter, configProperties) -> addAuthorizationHeaders(exporter, credentials))
        .build();
```

The `addAuthorizationHeaders` method computes the headers required for Google Cloud, including the `Authorization` bearer token:

```java
private static MetricExporter addAuthorizationHeaders(
    MetricExporter exporter, GoogleCredentials credentials) {
  if (exporter instanceof OtlpHttpMetricExporter) {
    return ((OtlpHttpMetricExporter) exporter)
        .toBuilder().setHeaders(() -> getRequiredHeaderMap(credentials)).build();
  } else if (exporter instanceof OtlpGrpcMetricExporter) {
    return ((OtlpGrpcMetricExporter) exporter)
        .toBuilder().setHeaders(() -> getRequiredHeaderMap(credentials)).build();
  }
  return exporter;
}
```

### Mapping and Limitations

#### Configuration Mapping

The following table maps the configurations available in `MetricConfiguration` to their OTLP equivalents:

| MetricConfiguration Option | OTLP Equivalent Property / Env Var | Notes |
| :--- | :--- | :--- |
| `setProjectId(String)` | Use resource attribute: `gcp.project_id` | If using the `opentelemetry-gcp-auth-extension`, the project ID can be inferred from the credentials or the environment. |
| `setCredentials(Credentials)` | Pass the bearer token as Authorization Header in the exporter | Handled automatically by `opentelemetry-gcp-auth-extension`. |
| `setMetricServiceEndpoint(String)` | `otel.exporter.otlp.endpoint` / `OTEL_EXPORTER_OTLP_ENDPOINT` | Default is `https://telemetry.googleapis.com`. |
| `setDeadline(Duration)` | `otel.exporter.otlp.timeout` / `OTEL_EXPORTER_OTLP_TIMEOUT` | Default is 10 seconds. |
| `setPrefix(String)` | N/A | The Telemetry API automatically prefixes metrics with `workload.googleapis.com/` by default. Custom prefixes are not directly supported via OTLP exporter configuration. |

#### Unsupported Features

The following features of the `GoogleCloudMetricExporter` are not supported by the standard OTLP exporter:

*   **Metric Descriptor Strategy (`setDescriptorStrategy`)**: OTLP exporters do not send metric descriptors separately. Metadata is handled automatically by the backend.
*   **Custom Monitored Resource Mapping (`setMonitoredResourceDescription`)**: OTLP relies on standard OTel resources. GCP maps these to monitored resources automatically.
*   **Predicate-based Resource Attribute Filtering (`setResourceAttributesFilter`)**: OTLP exporters send all resource attributes by default. If you need to filter them, you must do so before they reach the exporter (e.g., via resource configuration or a processor if using a collector).
*   **Use Service Time Series (`setUseServiceTimeSeries`)**: This option is specific to the Cloud Monitoring API and is not available in OTLP exporters.
*   **Instrumentation Library Labels Toggle (`setInstrumentationLibraryLabelsEnabled`)**: OTLP exporters send instrumentation scope information by default. Disabling it requires dropping the attributes via views or processors.
*   **Custom Metric Service Settings (`setMetricServiceSettings`)**: You cannot pass `MetricServiceSettings` to OTLP exporters. If you need custom channel or client configuration, you must use programmatic configuration with `OtlpGrpcMetricExporter.builder()` or `OtlpHttpMetricExporter.builder()`.

#### Complete Sample

For a complete sample demonstrating how to export metrics to Google Cloud using OTLP, see the [examples/otlpmetric](examples/otlpmetric) folder.

## Migrate from OpenTelemetry Google Cloud Auto Exporter

The Auto exporter allowed the [auto-configuration module](https://github.com/open-telemetry/opentelemetry-java/tree/main/sdk-extensions/autoconfigure#opentelemetry-sdk-autoconfigure) of OpenTelemetry Java to work with OpenTelemetry Google Cloud Trace and Monitoring exporters in this repository.

The standard OpenTelemetry OTLP exporters natively support auto-configuration and are the recommended way to send telemetry to Google Cloud. You can configure the OTLP exporters using the standard [exporter properties](https://opentelemetry.io/docs/languages/java/configuration/#properties-exporters) that are supported by the autoconfiguration module.
