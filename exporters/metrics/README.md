# Metrics Exporter

[![Maven Central][maven-image]][maven-url]

*NOTE: Metrics are still Alpha in OpenTelemetry, and the API is not guaranteed to be stable across versions.*

Opentelemetry Google Monitoring Metrics Exporter allows users to send collected metrics
to Google Cloud.

[Google Cloud Monitoring](https://cloud.google.com/monitoring) provides visibility into the performance, uptime, and overall health of cloud-powered applications. It collects metrics, events, and metadata from Google Cloud, Amazon Web Services, hosted uptime probes, application instrumentation, and a variety of common application components including Cassandra, Nginx, Apache Web Server, Elasticsearch, and many others. Operations ingests that data and generates insights via dashboards, charts, and alerts. Cloud Monitoring alerting helps you collaborate by integrating with Slack, PagerDuty, and more.

## Setup

Google Cloud Monitoring is a managed service provided by Google Cloud Platform. Google Cloud Monitoring requires to set up "Workspace" in advance. The guide to create a new Workspace is available on [the official document](https://cloud.google.com/monitoring/workspaces/create).

## Usage

TODO(jsuereth): Write this.

See [the code example](../../examples/metrics) for details.

## Authentication

TODO(jsuereth): Write this section.

## Configuration

You can configure the Cloud Monitoring Metrics Exporter via the following setup:

```java
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
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
// Now set up PeriodicMetricReader to use this Exporter
SdkMeterProvider.builder()
  .registerMetricReader(
    // Set collection interval to 20 seconds.
    // See https://cloud.google.com/monitoring/quotas#custom_metrics_quotas
    // Rate at which data can be written to a single time series: one point each 10
    // seconds.
    PeriodicMetricReader.create(metricExporter, java.time.Duration.ofSeconds(20)))
  .buildAndRegisterGlobal();
```

| Configuration | Environment Variable | JVM Property | Description | Default |
| ------------- | -------------------- | ------------ | ----------- | ------- |
| projectId     | GOOGLE_CLOUD_PROJECT or GOOGLE_APPLICATION_CREDENTIALS | ??? | The cloud project id.  This is autodiscovered. | The autodiscovered value. |
| credentials | GOOGLE_APPLICATION_CREDENTIALS | N/A | Credentials to use when talking to Cloud Monitoring API. | App Engine, Cloud Shell, GCE built-in or provided by `gcloud auth application-default login` |
| deadline      | ??? | ??? | The deadline limit on export calls to Cloud Monitoring API | 10 seconds |
| metricDescriptorStrategy | ??? | ??? | How to adapt OpenTelemetry metric definition into google cloud. `ALWAYS_SEND` will try to create metric descriptors on every export.  `SEND_ONCE` will try to create metric descriptors once per Java instance/classloader. `NEVER_SEND` will rely on Cloud Monitoring's auto-generated MetricDescriptors from time series. | `SEND_ONCE` |



[maven-image]: https://maven-badges.herokuapp.com/maven-central/com.google.cloud.opentelemetry/exporter-metrics/badge.svg
[maven-url]: https://maven-badges.herokuapp.com/maven-central/com.google.cloud.opentelemetry/exporter-metrics