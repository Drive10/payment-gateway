# Observability

## OpenTelemetry
- Add `opentelemetry-javaagent.jar` as a JVM arg in local runs:
  ```
  -javaagent:./otel/opentelemetry-javaagent.jar -Dotel.service.name=payment-gateway
  ```

## Prometheus
- Services expose `/actuator/prometheus` (if micrometer-registry-prometheus is on the classpath).
- Use `deploy/local/docker-compose.yml` to run Prometheus and Grafana locally.

## Grafana
- Import dashboards from `docs/observability/grafana-dashboards/`.