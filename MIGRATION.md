# Migration Guide

This guide provides instructions on how to migrate from the custom exporters in this repository to the standard OpenTelemetry OTLP exporters.

## Overview
Google Cloud now supports native OTLP (OpenTelemetry Protocol) ingestion for Cloud Trace and Cloud Monitoring via the [Telemetry API](https://docs.cloud.google.com/stackdriver/docs/reference/telemetry/overview). This allows you to use the standard OpenTelemetry OTLP exporters for sending telemetry data to Google Cloud.

## Migrate from OpenTelemetry Google Cloud Trace Exporter to OTLP exporter

To migrate from the deprecated Google Cloud Trace exporter to the standard OpenTelemetry OTLP exporter, follow these steps:

### 1. Add Dependencies

Add the following dependencies to your `build.gradle` file:

```groovy
implementation("io.opentelemetry:opentelemetry-exporter-otlp:1.56.0")
implementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure:1.56.0")
// Recommended for authentication when using autoconfigure module
implementation("io.opentelemetry.contrib:opentelemetry-gcp-auth-extension:1.52.0-alpha")
```

### 2. Configure the SDK

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

### Configuration Mapping

The following table maps the configurations available in `TraceConfiguration` to their OTLP equivalents:

| TraceConfiguration Option | OTLP Equivalent Property / Env Var | Notes |
| :--- | :--- | :--- |
| `setProjectId(String)` | Use resource attribute: `gcp.project_id` | If using the `opentelemetry-gcp-auth-extension`, the project ID can be inferred from the credentials or the environment. |
| `setCredentials(Credentials)` | Pass the bearer token as Authorization Header in the exporter | Handled automatically by `opentelemetry-gcp-auth-extension`. |
| `setTraceServiceEndpoint(String)` | `otel.exporter.otlp.endpoint` / `OTEL_EXPORTER_OTLP_ENDPOINT` | Default is `https://telemetry.googleapis.com`. |
| `setFixedAttributes(Map)` | `otel.resource.attributes` / `OTEL_RESOURCE_ATTRIBUTES` | Maps to Resource attributes in OTel, which are added to all telemetry data, not just spans. |
| `setDeadline(Duration)` | `otel.exporter.otlp.timeout` / `OTEL_EXPORTER_OTLP_TIMEOUT` | Default is 10 seconds. |

### Unsupported Features

The following features of the Google Cloud Trace exporter are not supported by the standard OTLP exporter:

*   **Attribute Mapping (`setAttributeMapping`)**: The OTLP exporter does not support renaming attributes (e.g., renaming OpenTelemetry standard attributes to legacy Stackdriver attributes). You should use standard OpenTelemetry attributes.
*   **Custom Trace Service Stub (`setTraceServiceStub`)**: You cannot pass a pre-configured `TraceServiceStub` to the OTLP exporter via configuration properties. If you need custom channel configuration, you must use programmatic configuration with `OtlpGrpcSpanExporter.builder()`.

### Migration Guide

For a complete migration guide, please visit the [Migrate from the Trace exporter to the OTLP endpoint](https://docs.cloud.google.com/trace/docs/migrate-to-otlp-endpoints) guide.

## Migrate from OpenTelemetry Google Cloud Monitoring Exporter to OTLP exporter

> [!NOTE] The Google Cloud OTLP metrics endpoint is currently in preview and the migration guides are being developed. 

TODO: Add migration guide for metrics exporter

## Migrate from OpenTelemetry Google Cloud Auto Exporter

The Auto exporter allowed the [auto-configuration module](https://github.com/open-telemetry/opentelemetry-java/tree/main/sdk-extensions/autoconfigure#opentelemetry-sdk-autoconfigure) of OpenTelemetry Java to work with OpenTelemetry Google Cloud Trace and Monitoring exporters in this repository.

The standard OpenTelemetry OTLP exporters natively support auto-configuration and are the recommended way to send telemetry to Google Cloud. You can configure the OTLP exporters using the standard [exporter properties](https://opentelemetry.io/docs/languages/java/configuration/#properties-exporters) that are supported by the autoconfiguration module.
