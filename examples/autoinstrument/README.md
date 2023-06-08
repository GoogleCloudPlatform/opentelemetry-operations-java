# Autoinstrumentation Example

An example spring webapp deployed and instrumented using the OpenTelemetry Java Auto-instrumentation agent.

### Prerequisites

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

## Running in Google Kubernetes Engine

To spin it up on your own GKE cluster, run the following:
```bash
./gradlew :examples-autoinstrument:jib --image="gcr.io/$GOOGLE_CLOUD_PROJECT/hello-autoinstrument-java"

sed s/%GOOGLE_CLOUD_PROJECT%/$GOOGLE_CLOUD_PROJECT/g \
 examples/autoinstrument/deployment.yaml | kubectl apply -f -

kubectl expose deployment  hello-autoinstrument-java --type LoadBalancer --port 80 --target-port 8080
```


This will expose the simple http server at port 80.   You can try out the tracing instrumentation via:

```bash
curl ${cluster_ip}
```

Or, if you'd like to synthesize a parent trace:

```bash
curl -H "traceparent:  00-ff000000000000000000000000000041-ff00000000000041-01" ${cluster_ip}
```

## Running in Google Cloud Run

To run this example in Google Cloud Run, you need to run the convenience script provided. After following the prerequisites, 

First, export the Google Cloud Region for cloud run to `GOOGLE_CLOUD_RUN_REGION` environment variable:

```shell
# This can be any supported Google cloud region
export GOOGLE_CLOUD_RUN_REGION=us-central1
```

Then, from the root of the repository,
```shell
cd examples/autoinstrument && ./run_in_cloud-run.sh
```
This will deploy the containerized application to Cloud Run and you will be presented with a service URL which would look something like - 

```text
Service URL: https://hello-autoinstrument-cloud-run-m43qtxry5q-uc.a.run.app
```

Once you have the service URL to the application, you can make **authenticated** requests to it. Authenticated requests can be made from the command line by passing an auth token in a cURL request - 

```shell
# Make sure to replace the SERVICE_URL with the one that was generated for your deployment

# Making a request to /
curl -H "Authorization: Bearer $(gcloud auth print-identity-token)" ${SERVICE_URL}/

# Making a request to /greeting
curl -H "Authorization: Bearer $(gcloud auth print-identity-token)" ${SERVICE_URL}/greeting
```

You can also allow public access to your cloud-run service, details for which can be found [here](https://cloud.google.com/run/docs/authenticating/public#console-ui).
With public access enabled, you would no longer need to provide the auth token within your requests.

## Running locally in a docker container

In case you do not want to spin up your own GKE cluster, but still want telemetry to be published to Google Cloud, you can run the example in a docker container. 

A convenience script has been provided which will run the example in a docker container.

From the root of the repository,
```shell
cd examples/autoinstrument && ./run_in_docker.sh
```
You can now interact with the sample spring example on **localhost:8080**. The metrics and traces from this example can be viewed in Google Cloud Console.
