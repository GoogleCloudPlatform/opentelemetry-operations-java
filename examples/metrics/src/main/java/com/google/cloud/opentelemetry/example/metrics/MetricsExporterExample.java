/*
 * Copyright 2023 Google LLC
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

import static com.google.api.client.util.Preconditions.checkNotNull;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.monitoring.v3.MetricServiceSettings;
import com.google.cloud.opentelemetry.metric.GoogleCloudMetricExporter;
import com.google.cloud.opentelemetry.metric.MetricConfiguration;
import io.grpc.ManagedChannelBuilder;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.contrib.gcp.resource.GCPResourceProvider;
import io.opentelemetry.exporter.logging.LoggingMetricExporter;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;
import java.io.IOException;
import java.time.Duration;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class MetricsExporterExample {
  private static SdkMeterProvider METER_PROVIDER;

  private static Meter METER;
  private static final Random RANDOM = new Random();

  private static MetricConfiguration generateMetricExporterConfig(boolean useDefaultConfig)
      throws IOException {
    if (useDefaultConfig) {
      System.out.println("Using default exporter configuration");
      return MetricConfiguration.builder().build();
    }

    System.out.println("Using custom configuration");
    // Configuring exporter through MetricServiceSettings
    Credentials credentials = GoogleCredentials.getApplicationDefault();
    MetricServiceSettings.Builder metricServiceSettingsBuilder = MetricServiceSettings.newBuilder();
    metricServiceSettingsBuilder
        .setCredentialsProvider(
            FixedCredentialsProvider.create(checkNotNull(credentials, "Credentials not provided.")))
        .setTransportChannelProvider(
            FixedTransportChannelProvider.create(
                GrpcTransportChannel.create(
                    ManagedChannelBuilder.forTarget(
                            MetricConfiguration.DEFAULT_METRIC_SERVICE_ENDPOINT)
                        // default 8 KiB
                        .maxInboundMetadataSize(16 * 1000)
                        .build())))
        .createMetricDescriptorSettings()
        .setSimpleTimeoutNoRetries(
            org.threeten.bp.Duration.ofMillis(MetricConfiguration.DEFAULT_DEADLINE.toMillis()))
        .build();

    // Any properties not set would be retrieved from the default configuration of the exporter.
    return MetricConfiguration.builder()
        .setMetricServiceSettings(metricServiceSettingsBuilder.build())
        .setInstrumentationLibraryLabelsEnabled(false)
        .build();
  }

  private static void setupMetricExporter(MetricConfiguration metricConfiguration) {
    GCPResourceProvider resourceProvider = new GCPResourceProvider();
    MetricExporter metricExporter =
        GoogleCloudMetricExporter.createWithConfiguration(metricConfiguration);
    MetricExporter metricDebugExporter = LoggingMetricExporter.create();
    METER_PROVIDER =
        SdkMeterProvider.builder()
            .setResource(Resource.create(resourceProvider.getAttributes()))
            .registerMetricReader(
                PeriodicMetricReader.builder(metricExporter)
                    .setInterval(Duration.ofSeconds(30))
                    .build())
            .registerMetricReader(
                PeriodicMetricReader.builder(metricDebugExporter)
                    .setInterval(Duration.ofSeconds(30))
                    .build())
            .build();

    METER =
        METER_PROVIDER
            .meterBuilder("instrumentation-library-name")
            .setInstrumentationVersion("semver:1.0.0")
            .build();
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

  // to run this from command line, execute `gradle run`
  public static void main(String[] args) throws InterruptedException, IOException {
    System.out.println("Starting the metrics-example application");
    boolean useDefaultConfig = true;
    if (args.length > 0) {
      if (args[0].equals("--custom-config")) {
        useDefaultConfig = false;
      }
    }
    setupMetricExporter(generateMetricExporterConfig(useDefaultConfig));

    try {
      int i = 0;
      while (i < 2) {
        System.out.println("Running example use case: #" + i);
        myUseCase();
        Thread.sleep(5000);
        i++;
      }
    } finally {
      System.out.println("Shutting down the metrics-example application");

      CompletableResultCode resultCode = METER_PROVIDER.shutdown();
      // Wait upto 60 seconds for job to complete
      resultCode.join(60, TimeUnit.SECONDS);
      if (resultCode.isSuccess()) {
        System.out.println("Shutdown completed successfully!");
      } else {
        System.out.println("Unable to shutdown gracefully!");
      }

      System.out.println("Exiting job");
    }
  }
}
