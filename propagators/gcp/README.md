# Google X-Cloud-Trace-Context propagtors

[![Maven Central][maven-image]][maven-url]

*NOTE: While OpenTelemetry SDK is stable, the Autoconfigure SDK extension is still Alpha in OpenTelemetry, and some features are not guaranteed to work across versions.*

This module allows attaching trace context with Google Cloud's [X-Cloud-Trace-Context header](https://cloud.google.com/trace/docs/setup#force-trace).


## Setup

The preferred mechanism is to use the [SDK autoconfigure extension](https://github.com/open-telemetry/opentelemetry-java/tree/main/sdk-extensions/autoconfigure).

- Add a runtime dependency from your java project to this library.
- You can now use `oneway-gcp` and `gcp` as viable propagation strings in [the `otel.propagators` flag](https://github.com/open-telemetry/opentelemetry-java/tree/main/sdk-extensions/autoconfigure#propagator)
- When using the autoinstrumentation agent, you also need to pass our propagators to the autoinstrumentation "extension" classpath. This can be done through the `otel.javaagent.experimental.extensions` flag. 
    ```
    -Dotel.javaagent.experimental.extensions=/path/to/downloaded/gcp-propagator
    ```
    For a complete example see [here](https://github.com/GoogleCloudPlatform/opentelemetry-operations-java/blob/main/examples/autoinstrument/build.gradle#L63).

## Manual Configuration

Instead of configuring with the SDK autoconfigure extension, you can instead directly register this propagator as 
follows:

```java
public class MyMain {
    public static void main(String[] args) {
        ContextPropagators propagators = ContextPropagators.create(
            TextMapPropagator.composite(
                W3CTraceContextPropagator.getInstance(),
                new XCloudTraceContextPropagator(/*oneway=*/true)));
        OpenTelemetrySdk sdk = OpenTelemetrySdk.builder()
            .setPropagators(propagators)
            // Other setup
            .build();
    }
}
```

[maven-image]: https://maven-badges.herokuapp.com/maven-central/com.google.cloud.opentelemetry/propagators-gcp/badge.svg
[maven-url]: https://maven-badges.herokuapp.com/maven-central/com.google.cloud.opentelemetry/propagators-gcp