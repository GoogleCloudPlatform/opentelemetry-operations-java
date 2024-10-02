# Exporting OTLP Metrics from Cloud Functions using OpenTelemetry Collector Sidecar

This example shows how to export OpenTelemetry metrics from a Google Cloud Run Function
to Google Managed Prometheus using OpenTelemetry Collector running as a sidecar.

*Google Cloud Functions were renamed to Google Cloud Run Functions. For more information on what this change entails, see [here](https://cloud.google.com/blog/products/serverless/google-cloud-functions-is-now-cloud-run-functions).* 

Additional details on deploying functions on Cloud Run can be viewed [here](https://cloud.google.com/run/docs/deploy-functions).

##### Important Note
This example leverages the use of `always-allocated CPU` for Cloud Run Services, which may have different pricing implication compared to the default `CPU only allocated during request` option.
Please see the [pricing table](https://cloud.google.com/run/pricing#tables) for differences and additional details.

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
   cd examples/otlpmetrics-function/collector-deployment
   docker build . -t us-central1-docker.pkg.dev/${GOOGLE_CLOUD_PROJECT}/otlp-cloud-run/otel-collector
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
This example shows how to use the `gcloud` CLI to deploy the function along with the docker container:

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
*Note that even though you are running `gcloud run deploy` instead of `gcloud functions deploy`, the `--function` flags instructs Cloud Run to deploy this service as a function.*

After your Cloud Run Function has finished deploying, depending on your authentication setup, you can create a proxy to the deployed function on your localhost to facilitate testing: 

```shell
# This will allow you to invoke your deployed function from http://localhost:8080
# Press Ctrl+C to interrupt the running proxy
gcloud beta run services proxy cloud-func-helloworld --port=8080
```

### Viewing exported metrics

This example is configured to export metrics via `debug` and `googlemanagedprometheus` exporters in the OpenTelemetry Collector.

 - The output of the debug exporter can be viewed on std out or the logs in Google Cloud Run Function logs, but it is mostly used for debugging any issues with your export.
 - The exported metrics from `googlemanagedprometheus` can be viewed in [metrics explorer](https://cloud.google.com/monitoring/charts/metrics-selector). You can search for the metric named `function_counter_gmp` and it should be listed under the resource `Prometheus Target`.

### Cleanup

After you are done with the example you can follow these steps to clean up any Google Cloud Resources created when running this example:

```shell
# Delete the deployed function
gcloud run services delete cloud-func-helloworld

# Delete the artifact registry and all its contents
gcloud artifacts repositories delete otlp-cloud-run --location=us-central1
```
