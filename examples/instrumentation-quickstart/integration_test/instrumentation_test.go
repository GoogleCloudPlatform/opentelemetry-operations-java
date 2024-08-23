// Copyright 2024 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

// Tests that docker compose services come up and that metrics, logs, and traces are
// successfully sent from the collector to GCP. The COMPOSE_OVERRIDE_FILE environment variable
// accepts a comma-separated list of paths to additional compose files to include.

package integration_test

import (
	"context"
	"fmt"
	"net/http"
	"os"
	"strings"
	"testing"
	"time"

	dto "github.com/prometheus/client_model/go"
	"github.com/prometheus/common/expfmt"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"github.com/testcontainers/testcontainers-go/modules/compose"
	"github.com/testcontainers/testcontainers-go/wait"
)

const (
	sentItemsThreshold = 100.0
)

type testCase struct {
	metricName string
	exporter   string
}

func TestApp(t *testing.T) {
	ctx := context.Background()
	composeStack := composeUp(ctx, t)

	// Let the docker compose app run until some spans/logs/metrics are sent to GCP
	t.Logf("Compose stack is up, waiting for prometheus metrics indicating successful export")

	// Check the collector's self-observability prometheus metrics to see that exports to GCP were successful.
	for _, tc := range []testCase{
		{metricName: "otelcol_exporter_sent_spans", exporter: "googlecloud"},
		{metricName: "otelcol_exporter_sent_log_records", exporter: "googlecloud"},
		{metricName: "otelcol_exporter_sent_metric_points", exporter: "googlemanagedprometheus"},
	} {
		t.Run(tc.metricName, func(t *testing.T) {
			require.EventuallyWithT(
				t,
				func(collect *assert.CollectT) {
					promMetrics, err := getPromMetrics(ctx, composeStack)
					if !assert.NoError(collect, err) {
						return
					}
					verifyPromMetric(collect, promMetrics, tc)
				},
				time.Minute*2, // wait for up to
				time.Second,   // check at interval
			)
		})
	}
}

func composeUp(ctx context.Context, t *testing.T) compose.ComposeStack {
	composeFiles := []string{"../docker-compose.yaml"}
	if composeOverrideFile := os.Getenv("COMPOSE_OVERRIDE_FILE"); composeOverrideFile != "" {
		composeFiles = append(composeFiles, strings.Split(composeOverrideFile, ",")...)
	}

	var (
		composeStack compose.ComposeStack
		err          error
	)
	composeStack, err = compose.NewDockerCompose(composeFiles...)
	require.NoError(t, err)

	require.NoError(t, err)
	composeStack = composeStack.WithOsEnv().
		WaitForService("app", wait.ForHTTP("/single").WithPort("8080")).
		WaitForService("otelcol", wait.ForHTTP("/metrics").WithPort("8888"))

	t.Cleanup(func() {
		require.NoError(t, composeStack.Down(ctx, compose.RemoveOrphans(true)))
	})
	require.NoError(t, composeStack.Up(ctx))
	return composeStack
}

func getPromMetrics(ctx context.Context, composeStack compose.ComposeStack) (map[string]*dto.MetricFamily, error) {
	promUri, err := getPromEndpoint(ctx, composeStack)
	if err != nil {
		return nil, err
	}

	resp, err := http.Get(promUri)
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close()

	var parser expfmt.TextParser
	parsed, err := parser.TextToMetricFamilies(resp.Body)
	if err != nil {
		return nil, err
	}
	return parsed, nil
}

func getPromEndpoint(ctx context.Context, composeStack compose.ComposeStack) (string, error) {
	collectorContainer, err := composeStack.ServiceContainer(ctx, "otelcol")
	if err != nil {
		return "", err
	}
	collectorHost, err := collectorContainer.Host(ctx)
	if err != nil {
		return "", err
	}
	collectorPort, err := collectorContainer.MappedPort(ctx, "8888")
	if err != nil {
		return "", err
	}
	return fmt.Sprintf("http://%s:%s/metrics", collectorHost, collectorPort.Port()), nil
}

func verifyPromMetric(t assert.TestingT, promMetrics map[string]*dto.MetricFamily, tc testCase) {
	if !assert.Contains(t, promMetrics, tc.metricName, "prometheus metrics do not contain %v:\n%v", tc.metricName, promMetrics) {
		return
	}
	mf := promMetrics[tc.metricName]

	for _, metric := range mf.Metric {
		for _, labelPair := range metric.GetLabel() {
			if labelPair.GetName() == "exporter" && labelPair.GetValue() == tc.exporter {
				value := metric.GetCounter().GetValue()
				assert.Greater(t, value, sentItemsThreshold, "Metric %v was expected to have value > %v, got %v", metric, sentItemsThreshold, value)
				return
			}
		}
	}
	assert.Fail(t, "Could not find a metric sample for exporter=%v, got metrics %v", tc.exporter, mf)
}
