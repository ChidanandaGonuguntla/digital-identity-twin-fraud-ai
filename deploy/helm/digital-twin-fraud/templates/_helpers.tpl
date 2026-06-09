{{- define "digital-twin-fraud.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "digital-twin-fraud.fullname" -}}
{{- if .Values.fullnameOverride -}}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- $name := default .Chart.Name .Values.nameOverride -}}
{{- if contains $name .Release.Name -}}
{{- .Release.Name | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}
{{- end -}}

{{- define "digital-twin-fraud.backend.fullname" -}}
{{- printf "%s-backend" (include "digital-twin-fraud.fullname" .) -}}
{{- end -}}

{{- define "digital-twin-fraud.frontend.fullname" -}}
{{- printf "%s-frontend" (include "digital-twin-fraud.fullname" .) -}}
{{- end -}}

{{- define "digital-twin-fraud.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" -}}
{{- end -}}

{{- define "digital-twin-fraud.labels" -}}
helm.sh/chart: {{ include "digital-twin-fraud.chart" . }}
{{ include "digital-twin-fraud.selectorLabels" . }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- range $key, $value := .Values.global.labels }}
{{ $key }}: {{ $value | quote }}
{{- end }}
{{- end -}}

{{- define "digital-twin-fraud.selectorLabels" -}}
app.kubernetes.io/name: {{ include "digital-twin-fraud.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}

{{- define "digital-twin-fraud.backend.selectorLabels" -}}
{{ include "digital-twin-fraud.selectorLabels" . }}
app.kubernetes.io/component: backend
{{- end -}}

{{- define "digital-twin-fraud.frontend.selectorLabels" -}}
{{ include "digital-twin-fraud.selectorLabels" . }}
app.kubernetes.io/component: frontend
{{- end -}}

{{- define "digital-twin-fraud.serviceAccountName" -}}
{{- if .Values.serviceAccount.create -}}
{{- default (include "digital-twin-fraud.fullname" .) .Values.serviceAccount.name -}}
{{- else -}}
{{- default "default" .Values.serviceAccount.name -}}
{{- end -}}
{{- end -}}

{{- define "digital-twin-fraud.secretName" -}}
{{- if .Values.secrets.existingSecret -}}
{{- .Values.secrets.existingSecret -}}
{{- else -}}
{{- include "digital-twin-fraud.fullname" . -}}-secret
{{- end -}}
{{- end -}}

{{- define "digital-twin-fraud.datasourceUrl" -}}
{{- printf "jdbc:postgresql://%s:%d/%s?currentSchema=%s" .Values.postgresql.host (.Values.postgresql.port | int) .Values.postgresql.database .Values.postgresql.schema -}}
{{- end -}}
