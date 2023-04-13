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

## Running locally in a docker container

In case you do not want to spin up your own GKE cluster, but still want telemetry to be published to Google Cloud, you can run the example in a docker container. 

A convenience script has been provided which will run the example in a docker container.

From the root of the repository,
```shell
cd examples/autoinstrument && ./run_in_docker.sh
```
You can now interact with the sample spring example on **localhost:8080**. The metrics and traces from this example can be viewed in Google Cloud Console.
