### Running the example to export metrics to Google Cloud

You can run this example to generate sample metrics on any Google Cloud project.

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

### Running the example

#### Run the example locally

You can run the example application via gradle. From the project root:

```shell
cd examples/metrics/ && gradle run 
```

#### Run the example as a Cloud Run Job

You can run the example application as a Google Cloud Run Job. A convenience script has been provided for this.

First, export your preferred Google cloud region where you want to create and run the job:

```shell
# This can be any valid Google Cloud Region
export GOOGLE_CLOUD_RUN_JOB_REGION="us-central1"
```

Then, from the project root:

```shell
cd examples/metrics/ && ./run_as_cloud-run-job.sh
```

*Note: When using the convenience script, it will create a Google Cloud Artifact Registry named `cloud-run-applications` in your
selected project.*

You should now see the exported metrics in your Google Cloud project.
