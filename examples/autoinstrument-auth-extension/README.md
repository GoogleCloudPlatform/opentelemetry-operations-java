# Autoinstrumentation exmaple with OTLP exporter enabled by GCP Auth Extension

This example showcases a simple Java app exporting Traces to Google Cloud Trace using built-in OTLP exporters, facilitated by the GCP Auth Extension built for Java Agent.

### Prerequisites

##### Get Google Cloud Credentials on your machine

```shell
gcloud auth application-default login
```
Executing this command will save your application credentials to default path which will depend on the type of machine -
- Linux, macOS: `$HOME/.config/gcloud/application_default_credentials.json`
- Windows: `%APPDATA%\gcloud\application_default_credentials.json`

**NOTE: This method of authentication is not recommended for production environments. For production environments, refer to the [Authentication Decision Tree](https://docs.cloud.google.com/docs/authentication#auth-decision-tree?) to identify the right authentication method for your use case.**

Next, export the credentials to `GOOGLE_APPLICATION_CREDENTIALS` environment variable -

For Linux & MacOS:
```shell
export GOOGLE_APPLICATION_CREDENTIALS=$HOME/.config/gcloud/application_default_credentials.json
```

Ensure you have Java 17 set in your environment:

```shell
# This step might look different for your machine. Ensure by running java --version
export PATH="/usr/lib/jvm/java-17-openjdk-amd64/bin:$PATH"
``` 

### Building and Running Locally

To build and run the application locally, you can use the Gradle wrapper from the root of the repository.

### Build the application

Run the following command from the root of the repository:

```shell
./gradlew -p examples/autoinstrument-auth-extension build
```

### Run the application

Run the following command from the root of the repository to start the HTTP server:

```shell
./gradlew -p examples/autoinstrument-auth-extension run
```

The server will start on port `8080`. You can test it using `curl` from another terminal:

```shell
curl http://localhost:8080/
```

### Building and running from a docker image

You can also run the application using Docker.

### Prerequisites

Ensure you have run the `installDist` task to prepare the distribution for the Docker image:

```shell
# Ensure Java 17 is used
export PATH="/usr/lib/jvm/java-17-openjdk-amd64/bin:$PATH"

./gradlew -p examples/autoinstrument-auth-extension installDist
```

### Build the Docker Image

Run the following command from the root of the repository:

```shell
docker build -t autoinstrument-auth-extension examples/autoinstrument-auth-extension
```

### Run the Docker Container

Run the following command to start the application in a docker container:

```shell
docker run \
      --rm \
      -e "GOOGLE_CLOUD_PROJECT=${GOOGLE_CLOUD_PROJECT}" \
      -e "GOOGLE_APPLICATION_CREDENTIALS=${GOOGLE_APPLICATION_CREDENTIALS}" \
      -v "${GOOGLE_APPLICATION_CREDENTIALS}:${GOOGLE_APPLICATION_CREDENTIALS}:ro" \
      -p 8080:8080 \
      --name test-app autoinstrument-auth-extension
```
The OpenTelemetry Java Agent is configured with JVM flags inside the Docker image to use both `logging` and `otlp` exporters. The OTLP exporter points to `https://telemetry.googleapis.com` using the `http/protobuf` protocol.

Authentication to Google Cloud is handled by the GCP Auth Extension. To run this successfully, ensure your environment has Application Default Credentials (ADC) available (e.g., when running on a GCP VM with appropriate scopes, or by passing `GOOGLE_APPLICATION_CREDENTIALS` if running locally).

The server will be accessible at `http://localhost:8080/`. You can test it using `curl`:

```shell
curl http://localhost:8080/
```

### Deploying to Google Cloud Run

You can deploy this application to Google Cloud Run using the provided `run_in_cloud-run.sh` script.

#### Prerequisites

Ensure you have the following environment variables set:
- `GOOGLE_CLOUD_PROJECT`: Your Google Cloud Project ID.
- `GOOGLE_CLOUD_RUN_REGION`: The region to deploy to (e.g., `us-central1`).

#### Deployment

Run the script from the root of the repository, passing the name of the Artifact Registry repository you want to use (it will be created if it doesn't exist):

```shell
./examples/autoinstrument-auth-extension/run_in_cloud-run.sh <artifact-registry-repo-name>
```

The script will:
1. Build the application using `./gradlew installDist`.
2. Build the Docker image using the `Dockerfile`.
3. Push the image to Artifact Registry.
4. Deploy the service to Cloud Run.

#### Authentication on Cloud Run

When running on Cloud Run, the application uses the associated Service Account for authentication. The `gcp-auth-extension` automatically uses these credentials (via Application Default Credentials) to authenticate OTel exports.

> [!IMPORTANT]
> The Service Account used by Cloud Run must have the following permissions:
> - `roles/cloudtrace.agent` (to export traces)
> - `roles/monitoring.metricWriter` (to export metrics)

If you wish to use a custom service account, you can modify the `gcloud run deploy` command in the script to include the `--service-account` flag:
```shell
--service-account="<service-account-email>"
```

### Clean up

To stop and remove the container:

```shell
docker rm -f test-app
```
