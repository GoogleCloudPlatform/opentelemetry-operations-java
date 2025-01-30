# OTLP Trace with Google Auth Example

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

Finally, to run the sample from the project root:

```
cd examples/otlptrace && gradle run
```
