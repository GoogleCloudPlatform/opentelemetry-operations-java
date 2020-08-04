# Cloud Trace Exporter for OpenTelemetry

  OpenTelemetry Google Cloud Trace Exporter allows the user to send collected traces to Google Cloud. 
  
 [Google Cloud Trace](https://cloud.google.com/trace) is a distributed tracing backend system. It helps developers to gather timing data needed to troubleshoot latency problems in microservice & monolithic architectures. It manages both the collection and lookup of gathered trace data.

## Setup

### Prerequisites
  Google Cloud Trace is a managed service provided by Google Cloud Platform.
  To use this exporter, you must have an application that you'd like to trace. The app can be on Google Cloud Platform, on-premise, or another cloud platform.
  
  In order to be able to push your traces to Trace, you must:
  
1. [Create a Cloud project](https://support.google.com/cloud/answer/6251787?hl=en).
2. [Enable billing](https://support.google.com/cloud/answer/6288653#new-billing).
3. [Enable the Trace API](https://console.cloud.google.com/apis/api/cloudtrace.googleapis.com/overview).

### Installation
  Not currently ready as a package to be imported. [Issue](https://github.com/GoogleCloudPlatform/opentelemetry-operations-java/issues/6) is currently being dealt with.  
  For now, one would have to clone this GitHub repo and import it as a separate project (see example). To do so, run in the command line:
  ```sh
  git clone git@github.com:GoogleCloudPlatform/opentelemetry-operations-java.git
  ```

### Usage
  If you are running in a GCP environment, the exporter will automatically authenticate using the environment's service account. If not, you will need to follow the instructions in Authentication.  

#### Create the exporter

This uses the default configuration for authentication and project ID.

```java
public class MyMainClass {
  public static void main(String[] args) throws Exception {
    this.traceExporter = TraceExporter.createWithDefaultConfiguration();
    // ...
  }
}
```

Or you can customize it with a TraceConfiguration object
```java
public class MyMainClass {
  public static void main(String[] args) throws Exception {
    this.traceExporter = TraceExporter.createWithConfiguration(
      TraceConfiguration.builder().setProjectId("myCoolGcpProject").build()
    );
    // ...
  }
}
```


  Then, we can connect TraceExporter to OpenTelemetry, for example:
  ```java
  OpenTelemetrySdk.getTracerProvider().addSpanProcessor(SimpleSpanProcessor.newBuilder(this.traceExporter).build());
  ```


#### Specifying a project ID
This exporter uses [google-cloud-java](https://github.com/GoogleCloudPlatform/google-cloud-java),
for details about how to configure the project ID see [here](https://github.com/GoogleCloudPlatform/google-cloud-java#specifying-a-project-id).

If you prefer to manually set the project ID, change it in the TraceConfiguration:
```java
TraceConfiguration.builder().setProjectId("MyProjectId").build();
```
before passing it in to the constructor
#### Authentication
  This exporter uses [google-cloud-java](https://github.com/googleapis/google-cloud-java), for details about how to configure the authentication see [here](https://github.com/googleapis/google-cloud-java#authentication).  


If you prefer to manually set the credentials use:
```java
TraceConfiguration.builder()
    .setCredentials(new GoogleCredentials(new AccessToken(accessToken, expirationTime)))
    .setProjectId( "MyProjectId")
    .build();
```
before passing it into the TraceExporter constructor

    
  In the case that there are problems creating a service account key, make sure that the **constraints/iam.disableServiceAccountKeyCreation** boolean variable is set to false. This can be edited on Google Cloud by clicking on Navigation Menu -> IAM & Admin -> Organization Policies -> Disable Service Account Key Creation -> Edit  
    
  If you are unable to edit this variable due to lack of permission, you can authenticate by running `gcloud auth application-default login` in the command line.

#### Java Versions
Java 8 or above is required for using this exporter.
  

## Useful Links
  - For more information on OpenTelemetry, visit: https://opentelemetry.io/  
  - For more about OpenTelemetry Java, visit: https://github.com/open-telemetry/opentelemetry-java  
  - Learn more about Google Cloud Trace at https://cloud.google.com/trace
  
