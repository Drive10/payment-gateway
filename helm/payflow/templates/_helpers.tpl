{{- define "payflow.labels" -}}
app.kubernetes.io/name: payflow
app.kubernetes.io/version: {{ .Chart.AppVersion }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}