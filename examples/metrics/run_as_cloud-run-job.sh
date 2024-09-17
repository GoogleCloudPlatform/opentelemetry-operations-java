#!/bin/bash
#
# Copyright 2023 Google LLC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
ARTIFACT_REPOSITORY=cloud-run-applications
JOB_NAME=job-metrics-export

# Verify necessary environment variables are set
echo "${GOOGLE_CLOUD_PROJECT:?${UNSET_WARNING}}"
echo "${GOOGLE_APPLICATION_CREDENTIALS:?${UNSET_WARNING}}"
echo "${GOOGLE_CLOUD_RUN_REGION:?${UNSET_WARNING}}"

echo "ENVIRONMENT VARIABLES VERIFIED"

# Safety check to verify if repository already exists.
if gcloud artifacts repositories describe ${ARTIFACT_REPOSITORY} \
  --location="${GOOGLE_CLOUD_RUN_REGION}"
then
  echo "Repository ${ARTIFACT_REPOSITORY} already exists."
else
  echo "CREATING CLOUD ARTIFACT REPOSITORY"
  gcloud artifacts repositories create ${ARTIFACT_REPOSITORY} --repository-format=docker --location=${GOOGLE_CLOUD_RUN_REGION} --description="Sample applications to run on Google Cloud Run"
  echo "CREATED ${ARTIFACT_REPOSITORY} in ${GOOGLE_CLOUD_RUN_REGION}"
fi

echo "BUILDING SAMPLE APP IMAGE"
gradle clean jib --image "${GOOGLE_CLOUD_RUN_REGION}-docker.pkg.dev/${GOOGLE_CLOUD_PROJECT}/${ARTIFACT_REPOSITORY}/metrics-export-java"

# Safety check to verify if the job already exists
if gcloud run jobs describe ${JOB_NAME} --region="${GOOGLE_CLOUD_RUN_REGION}"
then
  echo "Job ${JOB_NAME} already exists"
else
  echo "CREATING A CLOUD RUN JOB TO RUN THE CONTAINER"
  gcloud run jobs create job-metrics-export \
      --image="${GOOGLE_CLOUD_RUN_REGION}-docker.pkg.dev/${GOOGLE_CLOUD_PROJECT}/${ARTIFACT_REPOSITORY}/metrics-export-java" \
      --max-retries=5 \
      --region="${GOOGLE_CLOUD_RUN_REGION}" \
      --project="${GOOGLE_CLOUD_PROJECT}"
fi

echo "SETTING CLOUD RUN JOB REGION"
gcloud config set run/region "${GOOGLE_CLOUD_RUN_REGION}"

echo "RUNNING THE CREATED JOB"
gcloud run jobs execute job-metrics-export
