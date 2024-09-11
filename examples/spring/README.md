# Instrumenting Spring Application using OpenTelemetry with Resource Detection

This example shows how to export metrics and traces to GCP generated from a manually instrumented Spring Boot application.
Opentelemetry Autoconfiguration module is used to configure exporters and resource detection. 

#### Prerequisites

##### Get Google Cloud Credentials on your machine

```shell
gcloud auth application-default login
```
Executing this command will save your application credentials to default path which will depend on the type of machine -
- Linux, macOS: `$HOME/.config/gcloud/application_default_credentials.json`
- Windows: `%APPDATA%\gcloud\application_default_credentials.json`

##### Export the Google Cloud Project ID to `GOOGLE_CLOUD_PROJECT` environment variable:

```shell
export GOOGLE_CLOUD_PROJECT="my-awesome-gcp-project-id"
```

#### Run the application locally

You can run the example application via gradle. From the project root:

##### Build and run the executable JAR

```shell
cd examples/spring && gradle bootRun
```

The application is now running. To generate traces, head to `http://localhost:8080` in your browser.
*You can also try hitting other supported endpoints - `http://localhost:8080/greeting` to generate additional traces.*

You should now see the exported traces in your Google Cloud project.

#### Run the application in Cloud Run

To run this example in Google Cloud Run, you need to run the convenience script provided. After following the prerequisites,

First, export the Google Cloud Region for cloud run to `GOOGLE_CLOUD_RUN_REGION` environment variable:

```shell
# This can be any supported Google cloud region
export GOOGLE_CLOUD_RUN_REGION=us-central1
```

Then, from the root of the repository,
```shell
cd examples/spring && ./run_in_cloud-run.sh
```

This will deploy the containerized application to Cloud Run and create a proxy endpoint on your localhost which you can then call to reach your deployed Cloud Run service.
You should be able to hit the following endpoints from your browser: 

```text
http://localhost:8080/
http://localhost:8080/greeting
```

The metrics and traces from this example can be viewed in Google Cloud Console.