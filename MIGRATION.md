# Migration Guide

This guide provides instructions on how to migrate from the custom exporters in this repository to the standard OpenTelemetry OTLP exporters.

## Overview
Google Cloud now supports native OTLP (OpenTelemetry Protocol) ingestion for Cloud Trace and Cloud Monitoring via the [Telemetry API](https://docs.cloud.google.com/stackdriver/docs/reference/telemetry/overview). This allows you to use the standard OpenTelemetry OTLP exporters for sending telemetry data to Google Cloud.

## Migrate from OpenTelemetry Google Cloud Trace Exporter to OTLP exporter

Follow the [Migrate from the Trace exporter to the OTLP endpoint](https://docs.cloud.google.com/trace/docs/migrate-to-otlp-endpoints) guide for migration instructions.

## Migrate from OpenTelemetry Google Cloud Monitoring Exporter to OTLP exporter

> [!NOTE] The Google Cloud OTLP metrics endpoint is currently in preview and the migration guides are being developed. 

TODO: Add migration guide for metrics exporter

## Migrate from OpenTelemetry Google Cloud Auto Exporter

The Auto exporter allowed the [auto-configuration module](https://github.com/open-telemetry/opentelemetry-java/tree/main/sdk-extensions/autoconfigure#opentelemetry-sdk-autoconfigure) of OpenTelemetry Java to work with OpenTelemetry Google Cloud Trace and Monitoring exporters in this repository.

The standard OpenTelemetry OTLP exporters natively support auto-configuration and are the recommended way to send telemetry to Google Cloud. You can configure the OTLP exporters using the standard [exporter properties](https://opentelemetry.io/docs/languages/java/configuration/#properties-exporters) that are supported by the autoconfiguration module.
