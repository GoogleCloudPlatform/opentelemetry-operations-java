# OTLP Metric with Google Auth Example

Run this sample to connect to an endpoint that is protected by GCP authentication.

First, get GCP credentials on your machine:

```shell
gcloud auth application-default login
```
Executing this command will save your application credentials to default path which will depend on the type of machine -
 - Linux, macOS: `$HOME/.config/gcloud/application_default_credentials.json`
 - Windows: `%APPDATA%\gcloud\application_default_credentials.json`

Next, update [`build.gradle`](build.grade) to set the following:

```
	'-Dotel.resource.attributes=gcp.project_id=<YOUR_PROJECT_ID>,
	# Optional - if you want to export using gRPC protocol
	'-Dotel.exporter.otlp.protocol=grpc',
```

Finally, to run the sample from the project root:
```
./gradlew :examples-otlpmetric:run
```
