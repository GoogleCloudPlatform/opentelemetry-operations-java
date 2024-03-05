### Running the example to export traces to Google Cloud

You can run this example to generate sample traces on any Google Cloud project. 

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

#### Run the example

You can run the example application via gradle. From the project root: 

```shell
cd examples/trace/ && gradle run 
```

You should now see the exported traces in your Google Cloud project.