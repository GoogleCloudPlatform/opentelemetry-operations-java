# Metrics Exporter

TODO: Full Write up


## Configuration

You can configure the Cloud Monitoring Metrics Exporter via the following setup:

```java
import io.opentelemetry.sdk.metrics.export.IntervalMetricReader;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;

import com.google.cloud.opentelemetry.metric.MetricConfiguration;
import com.google.cloud.opentelemetry.metric.MetricDescriptorStrategy;

import java.util.Collections;

// Configure the exporter in code.
MetricExporter cloudMonitoringExporter =
  com.google.cloud.opentelemetry.metric.MetricExporter.createWithConfiguration(
      MetricConfiguration.builder()
      // Configure the cloud project id.  Note: this is autodiscovered by default.
      .setProjectId(...)
      // Set the credentials to use when writing to the Cloud Monitoring API
      .setCredentials(...)
      // Set the Deadline for exporting to Cloud Monitoring before giving up.
      .setDeadline(...)
      // Configure a strategy for how/when to configure metric descriptors.
      .setMetricDescriptorStrategy(MetricDescriptorStrategy.SEND_ONCE)
      .build()
  );
// Now set up IntervalMetricReader to use this Exporter
IntervalMetricReader reader =
   IntervalMetricReader.builder()
     // Set collection interval to 20 seconds.
     // See https://cloud.google.com/monitoring/quotas#custom_metrics_quotas
     // Rate at which data can be written to a single time series: one point each 10
     // seconds.
     .setExportIntervalMillis(20000)
     .setMetricExporter(cloudMonitoringExporter)
     // Wire our exporter into the "global" metrics from OpenTelemetry.
     // Note: You can also wire against your own MeterProvider instance to isolate
     // metrics being sent to Cloud Monitoring.
     .setMetricProducers(Collections.singleton(
       SdkMeterProvider.builder().buildAndRegisterGlobal()
     )).build()    
```

| Configuration | Environment Variable | JVM Property | Description | Default |
| ------------- | -------------------- | ------------ | ----------- | ------- |
| projectId     | GOOGLE_CLOUD_PROJECT or GOOGLE_APPLICATION_CREDENTIALS | ??? | The cloud project id.  This is autodiscovered. | The autodiscovered value. |
| credentials | GOOGLE_APPLICATION_CREDENTIALS | N/A | Credentials to use when talking to Cloud Monitoring API. | App Engine, Cloud Sheel, GCE built-in or provided by `gcloud auth applciation-default login` |
| deadline      | ??? | ??? | The deadline limit on exprot calls to Cloud Monitoring API | 10 seconds |
| metricDescriptorStrategy | ??? | ??? | How to adapt OpenTelemetry metric defintiion into google cloud. `ALWAYS_SEND` will try to create metric descriptors on every export.  `SEND_ONCE` will try to create metric descriptors once per Java instance/classloader. `NEVER_SEND` will rely on Cloud Monitoring's auto-generated MetricDescriptors from time series. | `SEND_ONCE` |


