# Exporting OTLP Metrics from Cloud Functions using OpenTelemetry Collector Sidecar

This example shows how to export OpenTelemetry metrics from a Google Cloud Run Function
to Google Managed Prometheus using OpenTelemetry Collector running as a sidecar.

*Google Cloud Functions were renamed to Google Cloud Run Functions. For more information on what this change entails, see [here](https://cloud.google.com/blog/products/serverless/google-cloud-functions-is-now-cloud-run-functions).* 

Additional details on deploying functions on Cloud Run can be viewed [here](https://cloud.google.com/run/docs/deploy-functions).

### Prerequisites

##### Get Google Cloud Credentials on your machine

```shell
gcloud auth application-default login
```

##### Export the Google Cloud Project ID to `GOOGLE_CLOUD_PROJECT` environment variable:

```shell
export GOOGLE_CLOUD_PROJECT="my-awesome-gcp-project-id"
```

### Deploying the function and collector sidecar

#### Prepare a docker image configured to run OpenTelemetry Collector

Create a docker image that runs OpenTelemetry container as a sidecar. This image would be pushed to Google Cloud Artifact Repository.

Follow these steps:

1. Create an artifact repository in your GCP project:
   ```shell
   gcloud artifacts repositories create otlp-cloud-run --repository-format=docker --location=us-central1
   
   gcloud auth configure-docker us-central1-docker.pkg.dev
   ```
2. Build & push the docker image with the collector:
   ```shell
   # From the root of the repository
   cd examples/otlpmetrics-functions/collector-deployment
   docker build . -t otel-collector
   docker push us-central1-docker.pkg.dev/${GOOGLE_CLOUD_PROJECT}/otlp-cloud-run/otel-collector
   ```

#### Deploy & Run the Google Cloud Run Function:

#### Build the JAR to deploy

You first need to build the JAR that will be deployed as a function. To do so, run: 

```shell
# From the examples-otlpmetrics-function directory
gradle clean build shadowJar
```
This command should generate a JAR named `hello-world-function.jar` in `out/deployment` directory.

#### Deploy the function
You can either use `gcloud` or a `terrafrom` script to deploy the function along with the docker container:

##### Using gcloud command

```shell
# From the examples-otlpmetrics-function directory
gcloud beta run deploy cloud-func-helloworld \
 --no-cpu-throttling \
 --container app-function \
 --function com.google.cloud.opentelemetry.examples.otlpmetricsfunction.HelloWorld \
 --source=out/deployment \
 --port=8080 \
 --container otel-collector \
 --image=us-central1-docker.pkg.dev/${GOOGLE_CLOUD_PROJECT}/otlp-cloud-run/otel-collector:latest
```


