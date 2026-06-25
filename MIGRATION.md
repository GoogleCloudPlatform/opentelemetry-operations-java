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
| `setTraceServiceEndpoint(String)` | `otel.exporter.otlp.endpoint` / `OTEL_EXPORTER_OTLP_ENDPOINT` | Set it to `https://telemetry.googleapis.com` to send traces to Google Cloud. |
| `setFixedAttributes(Map)` | `otel.resource.attributes` / `OTEL_RESOURCE_ATTRIBUTES` | Maps to Resource attributes in OTel, which are added to all telemetry data, not just spans. |
| `setDeadline(Duration)` | `otel.exporter.otlp.timeout` / `OTEL_EXPORTER_OTLP_TIMEOUT` | Default is 10 seconds. |

#### Unsupported Features

The following features of the Google Cloud Trace exporter are not supported by the standard OTLP exporter:

*   **Attribute Mapping (`setAttributeMapping`)**: The OTLP exporter does not support renaming attributes (e.g., renaming OpenTelemetry standard attributes to legacy Stackdriver attributes). You should use standard OpenTelemetry attributes.
*   **Custom Trace Service Stub (`setTraceServiceStub`)**: You cannot pass a pre-configured `TraceServiceStub` to the OTLP exporter via configuration properties. If you need custom channel configuration, you must use programmatic configuration with `OtlpGrpcSpanExporter.builder()`.

#### Complete Sample

For a complete sample demonstrating how to export traces to Google Cloud using OTLP, see the [examples/otlptrace](examples/otlptrace) folder.

## Migrate from OpenTelemetry Google Cloud Monitoring Exporter to OTLP exporter

> [!WARNING]
> **Breaking Change Warning:** Migrating from the legacy Google Cloud Monitoring exporter to the standard OTLP exporter introduces breaking changes to your metric names.
>
> *   **Legacy Exporter:** Ingests metrics under the `workload.googleapis.com/` domain (unless a custom prefix was configured).
> *   **OTLP Exporter:** Ingests metrics under the `prometheus.googleapis.com/` domain by default.
>
> Because of this domain change, your metric names in Cloud Monitoring will change. **This will break any existing dashboards, alerting policies, and will cause data discontinuity** between your historical and new metrics.

### Why Migrate?

While this migration introduces breaking changes, transitioning to the standard OTLP exporter is highly recommended for the following reasons:
*   **Standardization:** Aligns your application with the industry-standard OpenTelemetry Protocol (OTLP), ensuring vendor neutrality and compatibility with the broader OpenTelemetry ecosystem.
*   **Google Managed Prometheus (GMP):** Standard OTLP metrics are ingested into Google Managed Prometheus. GMP offers a robust, scalable, and cost-effective solution for monitoring (~20x cheaper than the Cloud Monitoring API ingestion), with long-term support and compatibility.
*   **Future-proofing:** The legacy Google Cloud Monitoring exporter is deprecated and will be archived after September 30th, 2026. Migrating now ensures your monitoring pipeline remains supported.

---

### Migration Strategies

We recommend three paths for migration, depending on your operational requirements:

1.  **Direct Migration (Recommended):** Migrate fully to the OTLP exporter and update your dashboards and alerts to use the new metric names under the `prometheus.googleapis.com/` domain.
2.  **Transition via Double-Writing (Alternative):** Run both the legacy exporter and the OTLP exporter in parallel. This allows you to validate the new OTLP pipeline and update dashboards/alerts without any monitoring downtime, at the cost of temporary double-ingestion charges.
3.  **Custom Wrapped Exporter (Alternative):** Wrap the standard OTLP exporter in-code to automatically prepend a prefix (like `workload.googleapis.com/`) to all metrics. This allows you to keep your existing dashboards and alerts working without changing your application's metric instrumentation code.

---

### Strategy 1: Direct Migration (Recommended)

Follow these steps to fully transition to the standard OTLP exporter.

#### 1. Update Dependencies

Replace the legacy Google Cloud metrics exporter dependency with the standard OTLP exporter and the GCP auth extension in your `build.gradle` file:

```groovy
// Remove: implementation("com.google.cloud:opentelemetry-operations-exporter-metrics:<version>")

// Add:
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

For more information, see [OpenTelemetry environment variables and system properties](https://opentelemetry.io/docs/languages/java/configuration/#environment-variables-and-system-properties).

#### 3. Initialize the SDK in Code

With the OTLP exporter and the `opentelemetry-gcp-auth-extension` added to your dependencies, you can initialize the OpenTelemetry SDK using `AutoConfiguredOpenTelemetrySdk`. The extension automatically handles authentication, so you don't need to write custom code to add authorization headers.

```java
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import java.util.concurrent.TimeUnit;

public class MyApplication {
  private static OpenTelemetrySdk openTelemetrySdk;

  public static void main(String[] args) {
    // Configure the OpenTelemetry pipeline with Auto configuration
    openTelemetrySdk = AutoConfiguredOpenTelemetrySdk.initialize().getOpenTelemetrySdk();

    // Application-specific logic here

    // Flush all buffered metrics on shutdown
    openTelemetrySdk.getSdkMeterProvider().shutdown().join(10, TimeUnit.SECONDS);
  }
}
```

#### 4. Adding Attributes

OpenTelemetry uses **Resource Attributes** to describe the entity producing telemetry (e.g., service name, host) and **Metric Attributes** to describe the specific measurement (e.g., HTTP method, status code).

##### Resource Attributes

You can set resource attributes using the `OTEL_RESOURCE_ATTRIBUTES` environment variable or system property. This is a good replacement for:
*   `setProjectId(String)`: Use the `gcp.project_id` resource attribute. Note that if you are using `opentelemetry-gcp-auth-extension`, you do not need to set this explicitly.

Example:
```bash
export OTEL_RESOURCE_ATTRIBUTES="service.name=my-service,gcp.project_id=my-project"
# Or pass as a system property flag to the JVM
-Dotel.resource.attributes=gcp.project_id=my-project
```

##### Metric Attributes

Add attributes to individual metrics when recording measurements. This is the standard way to add dimensions to your metrics.

```java
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;

// ... inside your method ...

Meter meter = openTelemetrySdk.getMeter("my-instrumentation");

// Metrics without prefix will be sent to the default domain - prometheus.googleapis.com
// and will be stored in Google Managed Prometheus with prometheus_target as the Monitored Resource.
LongCounter counter = meter.counterBuilder("processed_jobs").build();

// Metrics with custom prefix will be sent to the custom domain.
// In this case, Monitored Resource is determined by the attached OpenTelemetry Resource Attributes.
LongCounter customCounter = meter.counterBuilder("custom.googleapis.com/my_counter").build();

// Add attributes to the measurement
Attributes attributes = Attributes.of(AttributeKey.stringKey("job_type"), "import");
counter.add(1, attributes);
```

#### 5. Update Dashboards and Alerts

Once you deploy the OTLP exporter, your metrics will be ingested under the `prometheus.googleapis.com/` domain. You must update your Cloud Monitoring dashboards and alerting policies:
*   Identify all queries referencing `workload.googleapis.com/<metric_name>`.
*   Update them to reference `prometheus.googleapis.com/<metric_name>_<type>` or the corresponding Prometheus metric name format.

---

### Strategy 2: Transition via Double-Writing (Alternative)

To avoid gaps in monitoring and validate the OTLP pipeline before making it the source of truth, you can export metrics to both destinations simultaneously.

> [!IMPORTANT]
> **Cost Warning:** Double-writing metrics will double your metric ingestion volume, which will increase your Google Cloud Monitoring costs during the transition period. It also increases CPU and memory usage on your application.

#### 1. Keep Legacy and Add OTLP Dependencies

Keep your existing dependency on `opentelemetry-operations-exporter-metrics` and add the OTLP exporter dependencies to your `build.gradle`:

```groovy
// Keep your existing legacy exporter dependency
implementation("com.google.cloud:opentelemetry-operations-exporter-metrics:<version>")

// Add standard OTLP dependencies
implementation("io.opentelemetry:opentelemetry-exporter-otlp:1.56.0")
// Recommended for authentication
implementation("io.opentelemetry.contrib:opentelemetry-gcp-auth-extension:1.52.0-alpha")
```

#### 2. Configure SDK for Dual Export in Code

When double-writing, you cannot rely solely on the Autoconfigure module for metrics, as you need to register multiple metric readers. You must configure the SDK programmatically:

```java
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import com.google.cloud.opentelemetry.metric.GoogleCloudMetricExporter;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class MyApplication {
  private static OpenTelemetrySdk openTelemetrySdk;

  public static void main(String[] args) {
    // 1. Create the legacy Google Cloud exporter (writes to workload.googleapis.com)
    MetricExporter gcpExporter = GoogleCloudMetricExporter.createWithDefaultConfiguration();

    // 2. Create the standard OTLP exporter (writes to prometheus.googleapis.com)
    // Note: Assumes OTEL_EXPORTER_OTLP_ENDPOINT and other configuration env vars are set,
    // or defaults to standard OTLP endpoints.
    MetricExporter otlpExporter = OtlpGrpcMetricExporter.builder().build();

    // 3. Register both exporters as MetricReaders in the SdkMeterProvider
    SdkMeterProvider meterProvider = SdkMeterProvider.builder()
        .registerMetricReader(
            PeriodicMetricReader.builder(gcpExporter)
                .setInterval(Duration.ofSeconds(60))
                .build())
        .registerMetricReader(
            PeriodicMetricReader.builder(otlpExporter)
                .setInterval(Duration.ofSeconds(60))
                .build())
        .build();

    // 4. Build and register the OpenTelemetry SDK
    openTelemetrySdk = OpenTelemetrySdk.builder()
        .setMeterProvider(meterProvider)
        .buildAndRegisterGlobal();

    // Application-specific logic here

    // Flush and shutdown on exit
    openTelemetrySdk.getSdkMeterProvider().shutdown().join(10, TimeUnit.SECONDS);
  }
}
```

#### 3. Validate and Cutover

1.  **Verify Dual Ingestion:** Ensure metrics are appearing in Cloud Monitoring under both the old name (`workload.googleapis.com/...`) and the new name (`prometheus.googleapis.com/...`).
2.  **Migrate Dashboards/Alerts:** Create duplicates of your dashboards and alerts, updating them to use the new `prometheus.googleapis.com` metrics. Verify they show identical trends and trigger correctly.
3.  **Decommission Legacy Exporter:** Once validated, remove the legacy exporter dependency and the programmatic initialization code, reverting to the standard OTLP-only configuration (Strategy 1).

---

### Strategy 3: Custom Wrapped Exporter (Alternative)

If you want to use the standard OTLP exporter and the Autoconfigure module, but still need to prepend a prefix (e.g., `workload.googleapis.com/`) to all your metrics to keep your existing dashboards and alerts working *without* changing your individual metric instrumentation code, you can implement a custom wrapped exporter.

This approach intercepts the metrics just before they are exported, wraps them to prepend the prefix to their names, and then delegates the actual export to the standard OTLP exporter.

#### How it Works

1.  **Create a Wrapper Exporter**: Implement the `MetricExporter` interface to wrap the standard OTLP exporter and prepend the prefix to metric names.
2.  **Use OpenTelemetry SPI (AutoConfigurationCustomizerProvider)**: Register a customizer that automatically wraps the default OTLP exporter during SDK autoconfiguration.

#### Implementation Steps (Java)

##### 1. Create the `PrefixedMetricData` Wrapper

This class wraps standard `MetricData` and overrides `getName()` to prepend the desired prefix.

```java
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.metrics.data.Data;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.data.MetricDataType;
import io.opentelemetry.sdk.resources.Resource;

public class PrefixedMetricData implements MetricData {
  private final MetricData delegate;
  private final String prefix;

  public PrefixedMetricData(MetricData delegate, String prefix) {
    this.delegate = delegate;
    this.prefix = prefix;
  }

  @Override
  public Resource getResource() { return delegate.getResource(); }

  @Override
  public InstrumentationScopeInfo getInstrumentationScopeInfo() { return delegate.getInstrumentationScopeInfo(); }

  @Override
  public String getName() { return prefix + delegate.getName(); }

  @Override
  public String getDescription() { return delegate.getDescription(); }

  @Override
  public String getUnit() { return delegate.getUnit(); }

  @Override
  public MetricDataType getType() { return delegate.getType(); }

  @Override
  public Data<?> getData() { return delegate.getData(); }
}
```

##### 2. Create the `PrefixedMetricExporter` Wrapper

This class wraps the standard `MetricExporter` and maps the incoming metrics to `PrefixedMetricData` before exporting.

```java
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.common.export.MemoryMode;
import io.opentelemetry.sdk.metrics.Aggregation;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class PrefixedMetricExporter implements MetricExporter {
  private final MetricExporter delegate;
  private final String prefix;

  public PrefixedMetricExporter(MetricExporter delegate, String prefix) {
    this.delegate = delegate;
    this.prefix = prefix;
  }

  @Override
  public CompletableResultCode export(Collection<MetricData> metrics) {
    List<MetricData> prefixedMetrics =
        metrics.stream()
            .map(metric -> new PrefixedMetricData(metric, prefix))
            .collect(Collectors.toList());
    return delegate.export(prefixedMetrics);
  }

  @Override
  public CompletableResultCode flush() { return delegate.flush(); }

  @Override
  public CompletableResultCode shutdown() { return delegate.shutdown(); }

  @Override
  public void close() { delegate.close(); }

  @Override
  public Aggregation getDefaultAggregation(InstrumentType instrumentType) {
    return delegate.getDefaultAggregation(instrumentType);
  }

  @Override
  public io.opentelemetry.sdk.metrics.data.AggregationTemporality getAggregationTemporality(
      InstrumentType instrumentType) {
    return delegate.getAggregationTemporality(instrumentType);
  }

  @Override
  public MemoryMode getMemoryMode() { return delegate.getMemoryMode(); }
}
```

##### 3. Register the Customizer via SPI

Create a customizer class that implements `AutoConfigurationCustomizerProvider` to intercept and wrap the OTLP exporter.

```java
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;

public class OtlpMetricCustomizer implements AutoConfigurationCustomizerProvider {

  @Override
  public void customize(AutoConfigurationCustomizer autoConfigurationCustomizer) {
    autoConfigurationCustomizer.addMetricExporterCustomizer(
        (metricExporter, configProperties) -> {
          // Wrap only the OTLP exporter
          if (metricExporter.getClass().getName().contains("Otlp")) {
            // Replace "workload.googleapis.com/" with your desired prefix
            return new PrefixedMetricExporter(metricExporter, "workload.googleapis.com/");
          }
          return metricExporter;
        });
  }

  @Override
  public int order() {
    return Integer.MAX_VALUE; // Apply towards the end
  }
}
```

Register this customizer by creating a file named `io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider` in your project's `src/main/resources/META-INF/services/` directory.

Add the fully qualified name of your customizer class to this file:
```
com.yourpackage.OtlpMetricCustomizer
```

#### Pros & Cons

*   **Pros:**
    *   **No Instrumentation Changes:** You don't need to change the names of metrics in your application code.
    *   **Compatible with Autoconfigure:** Works seamlessly with the standard OpenTelemetry Autoconfigure module.
    *   **No Infrastructure Overhead:** Avoids the need to deploy and manage an OpenTelemetry Collector just for renaming metrics.
*   **Cons:**
    *   **Code Maintenance:** Requires adding and maintaining wrapper classes in your application codebase.
    *   **Language Specific:** You must implement this wrapper for each language used in your services (the example above is for Java).

Note: A complete implementation of this strategy can be found in [this example](https://github.com/GoogleCloudPlatform/opentelemetry-samples/tree/main/java/otlpmetric-exporter-wrapper).

---

### Mapping and Limitations

#### Configuration Mapping

The following table maps the configurations available in `MetricConfiguration` to their OTLP equivalents:

| MetricConfiguration Option | OTLP Equivalent Property / Env Var | Notes |
| :--- | :--- | :--- |
| `setProjectId(String)` | Use resource attribute: `gcp.project_id` | If using the `opentelemetry-gcp-auth-extension`, the project ID can be inferred from the credentials or the environment. |
| `setCredentials(Credentials)` | Pass the bearer token as Authorization Header in the exporter | Handled automatically by `opentelemetry-gcp-auth-extension`. |
| `setMetricServiceEndpoint(String)` | `otel.exporter.otlp.endpoint` / `OTEL_EXPORTER_OTLP_ENDPOINT` | Set it to `https://telemetry.googleapis.com` to send metrics to Google Cloud. |
| `setDeadline(Duration)` | `otel.exporter.otlp.timeout` / `OTEL_EXPORTER_OTLP_TIMEOUT` | Default is 10 seconds. |
| `setPrefix(String)` | N/A | The Telemetry API automatically prefixes metrics with `prometheus.googleapis.com/` by default. Custom prefixes are not supported via OTLP exporter configuration. If you must keep a custom prefix during transition, you must include it in the metric name in your instrumentation, or use the double-writing strategy. |

#### Unsupported Features

The following features of the `GoogleCloudMetricExporter` are not supported by the standard OTLP exporter:

*   **Metric Descriptor Strategy (`setDescriptorStrategy`)**: OTLP exporters do not send metric descriptors separately. Metadata is handled automatically by the backend.
*   **Custom Monitored Resource Mapping (`setMonitoredResourceDescription`)**: OTel relies on standard OTel resources. GCP maps these to monitored resources automatically. This feature was added to support internal use-cases only.
*   **Predicate-based Resource Attribute Filtering (`setResourceAttributesFilter`)**: OTLP exporters send all resource attributes by default. If you need to filter them, you must do so before they reach the exporter (e.g., via resource configuration or a processor if using a collector).
*   **Use Service Time Series (`setUseServiceTimeSeries`)**: This option is specific to the Cloud Monitoring API and is not available in OTLP exporters. This feature is for supporting internal use-cases only.
*   **Instrumentation Library Labels Toggle (`setInstrumentationLibraryLabelsEnabled`)**: OTLP exporters send instrumentation scope information by default. Disabling it requires dropping the attributes via views or processors.
*   **Custom Metric Service Settings (`setMetricServiceSettings`)**: You cannot pass `MetricServiceSettings` to OTLP exporters. If you need custom channel or client configuration, you must use programmatic configuration with `OtlpGrpcMetricExporter.builder()` or `OtlpHttpMetricExporter.builder()`.

#### Complete Sample

For a complete sample demonstrating how to export metrics to Google Cloud using OTLP, see the [otlpmetric sample](https://github.com/GoogleCloudPlatform/opentelemetry-samples/tree/main/java/otlpmetric).

## Migrate from OpenTelemetry Google Cloud Auto Exporter

The Auto exporter allowed the [auto-configuration module](https://github.com/open-telemetry/opentelemetry-java/tree/main/sdk-extensions/autoconfigure#opentelemetry-sdk-autoconfigure) of OpenTelemetry Java to work with OpenTelemetry Google Cloud Trace and Monitoring exporters in this repository.

The standard OpenTelemetry OTLP exporters natively support auto-configuration and are the recommended way to send telemetry to Google Cloud. You can configure the OTLP exporters using the standard [exporter properties](https://opentelemetry.io/docs/languages/java/configuration/#properties-exporters) that are supported by the autoconfiguration module.
