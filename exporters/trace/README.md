# Cloud Trace Exporter for OpenTelemetry

  OpenTelemetry Google Cloud Trace Exporter allows the user to send collected traces to Google Cloud. 
  
  <a href="https://cloud.google.com/trace">Google Cloud Trace</a> is a distributed tracing backend system. It helps developers to gather timing data needed to troubleshoot latency problems in microservice & monolithic architectures. It manages both the collection and lookup of gathered trace data.

## Setup
  Google Cloud Trace is a managed service provided by Google Cloud Platform.

## Installation
  Not currently ready as a package to be imported. To use presently, one would have to clone this GitHub repo.
## Usage
  If you are running in a GCP environment, the exporter will automatically authenticate using the environment's service account. If not, you will need to follow the instructions in Authentication.  
    
  The TraceExporter constructor takes in three parameters: String for projectId, TraceServiceClient to create a traceServiceClient instance, and Map<String, AttributeValue> for the fixed attributes of a span.  
  So, we need to import the following: 
  ```java
  import com.google.cloud.trace.v2.TraceServiceClient;
  import com.google.devtools.cloudtrace.v2.AttributeValue;
  import com.google.devtools.cloudtrace.v2.ProjectName;
  import com.google.devtools.cloudtrace.v2.Span;
  ```
  Declare and initialize the variables that will be used in the constructor parameters.  
  Then, we can create a TraceExporter with, for example:
  ```java
  TraceExporter exporter = new TraceExporter(projectId, traceServiceClient, fixedAttributes);
  ```
  Start tracing and collecting SpanData. To export these spans, use the export method, where spanDataList is a Collection of SpanData already collected:
  ```java
  exporter.export(spanDataList)
  ```

## Authentication
  This exporter uses <a href="https://github.com/googleapis/google-cloud-java">google-cloud-java</a>, for details about how to configure the authentication see <a href="https://github.com/googleapis/google-cloud-java#authentication">here</a>.  
    
  In the case that there are problems creating a service account key, make sure that the constraints/iam.disableServiceAccountKeyCreation boolean variable is set to false. This can be edited on Google Cloud by clicking on Navigation Menu -> IAM & Admin -> Organization Policies -> Disable Service Account Key Creation -> Edit  
    
  If you are unable to edit this variable due to lack of permission, you can authenticate by running "gcloud auth application-default login" in the command line.
  

## Useful Links
  - For more information on OpenTelemetry, visit: https://opentelemetry.io/  
  - For more about OpenTelemetry Java, visit: https://github.com/GoogleCloudPlatform/opentelemetry-operations-java  
  - Learn more about Google Cloud Trace at https://cloud.google.com/trace
  
  

