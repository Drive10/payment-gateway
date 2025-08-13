# Runbook

Common operational tasks, playbooks, and on-call hints.

- Rollbacks via Helm: `helm rollback payment-gateway <REV>`
- Inspect logs: `kubectl logs -n payments deploy/payment-service -f`
- Kafka topic lag: use Kafka UI or `kafka-consumer-groups.sh`
