# Autoconfiguration Example

An example application that sends custom traces+metric using *only* the OpenTelemetry API.  All SDK configuration is done using the autoconfiguration module.

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

For Windows:
```shell
SET GOOGLE_APPLICATION_CREDENTIALS=%APPDATA%\gcloud\application_default_credentials.json
```

##### Export the Google Cloud Project ID to `GOOGLE_CLOUD_PROJECT` environment variable:

```shell
export GOOGLE_CLOUD_PROJECT="my-awesome-gcp-project-id"
```

## Running in Google Kubernetes Engine

To spin it up on your own GKE cluster, run the following:
```
./gradlew :examples-autoconf:jib --image="gcr.io/$GOOGLE_CLOUD_PROJECT/hello-autoconfigure-java"

sed s/%GOOGLE_CLOUD_PROJECT%/$GOOGLE_CLOUD_PROJECT/g \
 examples/autoconf/job.yaml | kubectl apply -f -
```

This will run a batch job which synthesizes a nested trace, latency metrics and exemplars.

## Running locally on your machine

In case you do not want to spin up your own GKE cluster, but still want telemetry to be published to Google Cloud, you can run the example locally.

From the root of the repository,
```shell
cd examples/autoconf && gradle run
```

This will run the sample app locally on your machine, synthesizing a nested trace, latency metrics and exemplars.
