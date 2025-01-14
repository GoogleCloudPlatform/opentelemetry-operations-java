# OTLP Metric with Google Auth Example

Run this sample to connect to an endpoint that is protected by GCP authentication.

First, get GCP credentials on your machine:

```shell
gcloud auth application-default login
```
Executing this command will save your application credentials to default path which will depend on the type of machine -
 - Linux, macOS: `$HOME/.config/gcloud/application_default_credentials.json`
 - Windows: `%APPDATA%\gcloud\application_default_credentials.json`

Next, set your endpoint with the `OTEL_EXPORTER_OTLP_ENDPOINT` environment variable:

```shell
export OTEL_EXPORTER_OTLP_ENDPOINT="http://your-endpoint:port"
```

Next, update [`build.gradle`](build.grade) to set the following:

```
	'-Dotel.resource.attributes=gcp.project_id=<YOUR_PROJECT_ID>,
	'-Dotel.exporter.otlp.headers=X-Goog-User-Project=<YOUR_QUOTA_PROJECT>',
	# Optional - if you want to export using gRPC protocol
	'-Dotel.exporter.otlp.protocol=grpc',
```

Finally, to run the sample from the project root:

```
cd examples/otlpmetric && gradle run
```
