/*
 * Copyright 2022 Google
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.cloud.opentelemetry.example.metrics;

import com.google.cloud.opentelemetry.metric.MetricExporter;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import java.io.IOException;
import java.util.Random;

public class MetricsExporterExample {
  private static SdkMeterProvider METER_PROVIDER;

  private static Meter METER;
  private static final Random RANDOM = new Random();

  private static void setupMetricExporter() {
    try {
      MetricExporter metricExporter = MetricExporter.createWithDefaultConfiguration();
      METER_PROVIDER =
          SdkMeterProvider.builder()
              .registerMetricReader(
                  PeriodicMetricReader.builder(metricExporter)
                      .setInterval(java.time.Duration.ofSeconds(30))
                      .newMetricReaderFactory())
              .build();

      METER =
          METER_PROVIDER
              .meterBuilder("instrumentation-library-name")
              .setInstrumentationVersion("semver:1.0.0")
              .build();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static void myUseCase() {
    LongCounter counter =
        METER
            .counterBuilder("example_counter")
            .setDescription("Processed jobs")
            .setUnit("1")
            .build();
    doWork(counter);
  }

  private static void doWork(LongCounter counter) {
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
    System.out.println("Starting the metrics-example application");
    setupMetricExporter();
    try {
      int i = 0;
      while (true) {
        System.out.println("Running example use case: #" + i);
        myUseCase();
        Thread.sleep(10000);
        i++;
      }
    } finally {
      System.out.println("Shutting down the metrics-example application");

      METER_PROVIDER.shutdown();

      System.out.println("Shutdown complete");
    }
  }
}
