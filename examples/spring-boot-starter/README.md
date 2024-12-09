# Getting Started

This example shows a Java Spring Boot application running as GraalVM Native Image, auto-instrumented using OpenTelemetry Spring Boot Starter agent.
The agent has been configured to export the telemetry to Google Cloud Platform.

There are two ways to build and run this example:
 - Lightweight Container with Cloud Native Buildpacks.
 - Executable with Native Build Tools

### Reference Documentation

For further reference, please consider the following sections:

* [OpenTelemetry Spring Boot Starter Agent](https://opentelemetry.io/docs/zero-code/java/spring-boot-starter/)
* [Official Gradle documentation](https://docs.gradle.org)
* [Spring Boot Gradle Plugin Reference Guide](https://docs.spring.io/spring-boot/3.3.5/gradle-plugin)
* [Create an OCI image](https://docs.spring.io/spring-boot/3.3.5/gradle-plugin/packaging-oci-image.html)
* [GraalVM Native Image Support](https://docs.spring.io/spring-boot/3.3.5/reference/packaging/native-image/introducing-graalvm-native-images.html)

### Additional Links

These additional references should also help you:

* [Gradle Build Scans – insights for your project's build](https://scans.gradle.com#gradle)
* [Configure AOT settings in Build Plugin](https://docs.spring.io/spring-boot/3.3.5/how-to/aot.html)

### Prerequisites

> [!IMPORTANT]
> This example requires Java 17 to build and run.

##### Get Google Cloud Credentials on your machine

```shell
gcloud auth application-default login
```
Executing this command will save your application credentials to default path which will depend on the type of machine -
- Linux, macOS: `$HOME/.config/gcloud/application_default_credentials.json`
- Windows: `%APPDATA%\gcloud\application_default_credentials.json`

**NOTE: This method of authentication is not recommended for production environments.**

Next, export the credentials to `GOOGLE_APPLICATION_CREDENTIALS` environment variable -

For Linux & MacOS:
```shell
export GOOGLE_APPLICATION_CREDENTIALS=$HOME/.config/gcloud/application_default_credentials.json
```

##### Export the Google Cloud Project ID to `GOOGLE_CLOUD_PROJECT` environment variable:

```shell
export GOOGLE_CLOUD_PROJECT="my-awesome-gcp-project-id"
```

### Lightweight Container with Cloud Native Buildpacks

If you're already familiar with Spring Boot container images support, this is
the easiest way to get started.
Docker should be installed and configured on your machine prior to creating the
image.

To create the image, run the following goal:

```shell
gradle bootBuildImage
```

Then, you can run the app like any other container:

```shell
docker run \
      --rm \
      -e "GOOGLE_CLOUD_PROJECT=${GOOGLE_CLOUD_PROJECT}" \
      -e "GOOGLE_APPLICATION_CREDENTIALS=${GOOGLE_APPLICATION_CREDENTIALS}" \
      -v "${GOOGLE_APPLICATION_CREDENTIALS}:${GOOGLE_APPLICATION_CREDENTIALS}:ro" \
      -u $(id -u ${USER}) \
      -p 8080:8080 \
      "examples-spring-boot-starter:0.1.0"
```

### Executable with Native Build Tools

Use this option if you want to explore more options such as running your tests
in a native image.
The GraalVM `native-image` compiler should be installed and configured on your
machine.

To create the executable, run the following goal:

```shell
gradle nativeCompile
```

Then, you can run the app as follows:

```shell
./build/native/nativeCompile/spring-native-example
```

### Interacting with the running Spring Applications

You can interact with the running Spring application by making cURL requests at
localhost:8080 to interact with the deployed Spring application.

```shell
# Call the home endpoint
curl http://localhost:8080/
# Call the ping endpoint
curl http://localhost:8080/ping
```
Make a few requests on these endpoints to generate some traces and metrics.

### Viewing the generated telemetry

After making a few requests to the deployed Spring application, the metrics and
traces generated using the OpenTelemetry Spring Boot Starter agent should be
visible in the configured project in Google Cloud Platform UI. 

* To view the generated traces, use the [Trace Explorer](https://cloud.google.com/trace/docs/finding-traces)
* To view the generated metrics, select the metrics in the [Metrics Selector](https://cloud.google.com/monitoring/charts/metrics-selector)

Some known metrics produced by Spring Boot Starter Agent include 
`processedSpans`, `http.server.request.duration` and `queueSize`.

For more details on out-of-the-box instrumentation provided by the OpenTelemetry
Spring Boot Starter Agent, see [here](https://opentelemetry.io/docs/zero-code/java/spring-boot-starter/out-of-the-box-instrumentation/).
