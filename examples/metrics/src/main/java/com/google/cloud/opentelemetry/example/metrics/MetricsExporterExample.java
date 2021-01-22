package com.google.cloud.opentelemetry.example.metrics;

import static java.util.Collections.singleton;

import com.google.cloud.opentelemetry.metric.MetricExporter;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.LongUpDownCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.GlobalMetricsProvider;
import io.opentelemetry.sdk.metrics.export.IntervalMetricReader;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import java.io.IOException;
import java.util.Random;

public class MetricsExporterExample {
  private static final Meter METER =
    GlobalMetricsProvider.getMeter("instrumentation-library-name", "semver:1.0.0");
  private static final Random RANDOM = new Random();
  private static io.opentelemetry.sdk.metrics.export.MetricExporter metricExporter;
  private static IntervalMetricReader intervalMetricReader;

  private static void setupMetricExporter() {
    try {
      metricExporter = MetricExporter.createWithDefaultConfiguration();
      intervalMetricReader =
          IntervalMetricReader.builder()
              // See https://cloud.google.com/monitoring/quotas#custom_metrics_quotas
              // Rate at which data can be written to a single time series: one point each 10
              // seconds.
              .setExportIntervalMillis(20000)
              .setMetricExporter(metricExporter)
              .setMetricProducers(
                  singleton(SdkMeterProvider.builder().buildAndRegisterGlobal()))
              .build();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static void myUseCase() {
    LongUpDownCounter counter =
        METER
            .longUpDownCounterBuilder("processed_jobs")
            .setDescription("Processed jobs")
            .setUnit("1")
            .build();
    doWork(counter);
  }

  private static void doWork(LongUpDownCounter counter) {
    try {
      for (int i = 0; i < 10; i++) {
        counter.add(RANDOM.nextInt(100));
        Thread.sleep(RANDOM.nextInt(1000));
      }
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  public static void main(String[] args) throws InterruptedException {
    setupMetricExporter();

    myUseCase();
    Thread.sleep(10000);

    intervalMetricReader.shutdown();
    metricExporter.flush();
    metricExporter.shutdown();
  }
}
