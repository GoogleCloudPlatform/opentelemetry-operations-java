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
if [[ -z "${GOOGLE_CLOUD_PROJECT}" ]]; then
  echo "GOOGLE_CLOUD_PROJECT environment variable not set"
  exit 1
fi

if [[ -z "${GOOGLE_APPLICATION_CREDENTIALS}" ]]; then
  echo "GOOGLE_APPLICATION_CREDENTIALS environment variable not set"
  exit 1
fi

if [[ -z "${GOOGLE_CLOUD_RUN_JOB_REGION}" ]]; then
  echo "GOOGLE_CLOUD_RUN_JOB_REGION environment variable not set"
  exit 1
fi

echo "ENVIRONMENT VARIABLES VERIFIED"

echo "BUILDING SAMPLE APP IMAGE"
gradle clean jib --image "gcr.io/${GOOGLE_CLOUD_PROJECT}/metrics-export-java"


echo "CREATING A CLOUD RUN JOB TO RUN THE CONTAINER"
gcloud run jobs create job-metrics-export \
    --image "gcr.io/${GOOGLE_CLOUD_PROJECT}/metrics-export-java" \
    --max-retries 5 \
    --region ${GOOGLE_CLOUD_RUN_JOB_REGION} \
    --project="${GOOGLE_CLOUD_PROJECT}"

echo "SETTING CLOUD RUN JOB REGION"
gcloud config set run/region "${GOOGLE_CLOUD_RUN_JOB_REGION}"

echo "RUNNING THE CREATED JOB"
gcloud run jobs execute job-metrics-export
