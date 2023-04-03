### Running the example to export metrics & traces to Google Cloud

This OTel Autoinstrumentation example runs locally on your machine in a docker container and uses the auto-exporter to export metrics and traces to Google Cloud without any configuration.  

#### Prerequisites

##### Get Google Cloud Credentials on your machine

```shell
gcloud auth application-default login
```
Executing this command will save your application credentials to default path which will depend on the type of machine -
- Linux, macOS: `$HOME/.config/gcloud/application_default_credentials.json`
- Windows: `%APPDATA%\gcloud\application_default_credentials.json`

##### Export the retrieved credentials to `GOOGLE_APPLICATION_CREDENTIALS` environment variable.

```shell
export GOOGLE_APPLICATION_CREDENTIALS=$HOME/.config/gcloud/application_default_credentials.json
```

##### Export the Google Cloud Project ID to `GOOGLE_CLOUD_PROJECT` environment variable:

```shell
export GOOGLE_CLOUD_PROJECT="my-awesome-gcp-project-id"
```

##### Export the file path for auto-exporter to `GOOGLE_AUTO_EXPORTER_PATH` environment variable:

You need the `shaded` variant of the Google Auto Exporter for OpenTelemetry to use with this auto-instrumentation example. 
You can build this by running `./gradlew :exporter-auto:shadowJar` from the root of this repository. 

Export the path of the built JAR to the environment variable:

```shell
export GOOGLE_AUTO_EXPORTER_PATH="$(pwd)/exporters/auto/build/libs/exporter-auto-0.1.0-SNAPSHOT-shaded.jar"
```

*Note: In the future, this will be provided as a downloadable artifact from maven.* 

#### Run the example

You can run the example application from the provided shell script. From the root of the repository:

```shell
cd examples/autoinstrument-docker && ./run_in_docker.sh
```

You should now see the exported metrics & traces in your Google Cloud project.