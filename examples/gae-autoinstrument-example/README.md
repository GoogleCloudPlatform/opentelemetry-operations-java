Google AppEngine Standard with OTel Auto-instrumentation
============================
**This sample app is a Maven project and builds separately from the rest of the rest of the examples.**

This example Java app runs on GAE standard environment **(Gen2 only)** and is instrumented using OpenTelemetry's [auto-instrumentation](https://github.com/open-telemetry/opentelemetry-java-instrumentation) agent.

See the [Google App Engine standard environment documentation][ae-docs] for more
detailed instructions.

The sample app uses the following - 

[ae-docs]: https://cloud.google.com/appengine/docs/java/


* [Java 11](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
* [Maven](https://maven.apache.org/download.cgi) (at least 3.5)
* [Google Cloud SDK](https://cloud.google.com/sdk/) (aka gcloud)
* [Google Java Logging for GAE](https://github.com/googleapis/java-logging)
* [Google Java Logging - Logback](https://github.com/googleapis/java-logging-logback)
* [Auto-Configuration for OpenTelemetry in Google Cloud](https://github.com/GoogleCloudPlatform/opentelemetry-operations-java/tree/main/exporters/auto)

## Setup

    gcloud init
    gcloud auth login
    gcloud config set project <your-gcp-project-id>

## Maven
### Clean Build 

    mvn clean package

### Running locally

    mvn appengine:run

### Deploying

    mvn appengine:deploy -Dapp.deploy.projectId=<your-gcp-project-id>

## About the Project

The project deploys a GAE standard application to your configured Google Cloud Project. The OpenTelemetry auto-instrumentation agent automatically collects telemetry from the running application which is autoconfigured to be exported to Google Cloud via the [auto-exporter](https://github.com/GoogleCloudPlatform/opentelemetry-operations-java/tree/main/exporters/auto).
The telemetry data from auto-instrumentation contains traces from the application which help gain insight into individual requests made to the application, along with some useful metrics which give an insight into resource utilization and more.

By default, the logging framework used in the example is [Logback](https://logback.qos.ch/), but Google cloud logging also supports [JUL](https://docs.oracle.com/javase/8/docs/api/java/util/logging/package-summary.html) and a sample configuration file for JUL has been provided - [logging.properties](./src/main/webapp/WEB-INF/logging.properties).

*Checkout [Cloud Logging Setup for Java](https://cloud.google.com/logging/docs/setup/java) for more details.* 

Metrics and Traces can be viewed in the Google Cloud Console for your project.

*Note: Metrics exported from the auto-exporter are prefixed with `workload.googleapis.com` - you can use this to search for generated metrics in the metrics explorer.*
 
*Note: Details for Google App Engine Standard runtime for Java11 can be found [here](https://cloud.google.com/appengine/docs/standard/java-gen2/java-differences).*

*Note: Log enhancer currently does not work with JUL, for better log-trace correlation and enhanced logs use Logback. It is already configured for this project and can be used with SLF4J API.*
