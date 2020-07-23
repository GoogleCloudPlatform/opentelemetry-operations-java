# Cloud Trace Exporter for OpenTelemetry

  OpenTelemetry Google Cloud Trace Exporter allows the user to send collected traces to Google Cloud. 
  
 [Google Cloud Trace](https://cloud.google.com/trace) is a distributed tracing backend system. It helps developers to gather timing data needed to troubleshoot latency problems in microservice & monolithic architectures. It manages both the collection and lookup of gathered trace data.

## Setup
  Google Cloud Trace is a managed service provided by Google Cloud Platform.

## Installation
  Not currently ready as a package to be imported. [Issue](https://github.com/GoogleCloudPlatform/opentelemetry-operations-java/issues/6) is currently being dealt with.  
  For now, one would have to clone this GitHub repo. To do so, run in the command line:
  ```git
  git clone https://github.com/GoogleCloudPlatform/opentelemetry-operations-java
  ```
## Usage
  If you are running in a GCP environment, the exporter will automatically authenticate using the environment's service account. If not, you will need to follow the instructions in Authentication.  
    
  The TraceExporter constructor takes in three parameters: `projectId: String` for your GCP project ID, `traceServiceClient: TraceServiceClient` for the trace service client to eventually batch write spans, and `fixedAttributes: Map<String, AttributeValue>` for the fixed attributes of a span.  
  So, we need to import the following: 
  ```java
  import com.google.cloud.trace.v2.TraceServiceClient;
  import com.google.devtools.cloudtrace.v2.AttributeValue;
  import com.google.devtools.cloudtrace.v2.ProjectName;
  import com.google.devtools.cloudtrace.v2.Span;
  ```
  Declare and initialize the variables that will be used in the constructor parameters.  
  Then, we can create and register TraceExporter, for example:
  ```java
  TraceExporter javaTraceExporter = new TraceExporter(projectId, traceServiceClient, fixedAttributes);
  OpenTelemetrySdk.getTracerProvider().addSpanProcessor(SimpleSpanProcessor.newBuilder(this.javaTraceExporter).build());
  ```
  Start tracing and collecting SpanData.  
  Spans can be created by importing and using global `opentelemetry-java` API packages, for example:  
  ```java
  String operationName = "receive"; //or whatever other operation
  Span span = this.tracer.spanBuilder(operationName).startSpan();
  ```

## Authentication
  This exporter uses [google-cloud-java](https://github.com/googleapis/google-cloud-java), for details about how to configure the authentication see [here](https://github.com/googleapis/google-cloud-java#authentication).  
    
  In the case that there are problems creating a service account key, make sure that the **constraints/iam.disableServiceAccountKeyCreation** boolean variable is set to false. This can be edited on Google Cloud by clicking on Navigation Menu -> IAM & Admin -> Organization Policies -> Disable Service Account Key Creation -> Edit  
    
  If you are unable to edit this variable due to lack of permission, you can authenticate by running `gcloud auth application-default login` in the command line.
  

## Useful Links
  - For more information on OpenTelemetry, visit: https://opentelemetry.io/  
  - For more about OpenTelemetry Java, visit: https://github.com/open-telemetry/opentelemetry-java  
  - Learn more about Google Cloud Trace at https://cloud.google.com/trace
  
