import { useQuery } from '@tanstack/react-query';
import { Activity, AlertTriangle, ExternalLink, Gauge, Radio, Server } from 'lucide-react';
import { KpiCard } from '@/components/ui/KpiCard';
import { PageHeader } from '@/components/ui/PageHeader';
import { env } from '@/config/env';
import { fraudApi } from '@/lib/fraudApi';

const POLL_MS = 15_000;

const GRAFANA_DASHBOARDS = [
  { uid: 'dti-fraud-operations', label: 'Fraud Operations' },
  { uid: 'dti-fraud-kafka-model', label: 'Kafka & Model Drift' },
  { uid: 'dti-fraud-slos', label: 'Platform SLOs' }
];

function sloLabel(met: boolean) {
  return met ? 'SLO met' : 'SLO breach';
}

function grafanaDashboardUrl(uid: string) {
  const base = env.grafanaUrl.replace(/\/$/, '');
  return `${base}/d/${uid}`;
}

export function PlatformOpsPage() {
  const { data, isLoading, error } = useQuery({
    queryKey: ['platform-ops-summary'],
    queryFn: () => fraudApi.platformOpsSummary(),
    enabled: !env.useMock,
    refetchInterval: POLL_MS
  });

  return (
    <div className="page-stack">
      <PageHeader
        eyebrow="SRE"
        title="Platform Operations"
        description="Live platform health for model risk and admin roles. Full SLO detail lives in Grafana."
      />

      {isLoading && <div className="glass-card">Loading platform health…</div>}
      {error && <div className="glass-card error-banner">Failed to load ops summary: {(error as Error).message}</div>}

      {data && (
        <>
          <div className="kpi-grid">
            <KpiCard
              label="Decision latency p95"
              value={`${data.p95LatencyMs} ms`}
              trend={`${sloLabel(data.sloLatencyMet)} · target ≤ 300 ms`}
              icon={<Gauge />}
            />
            <KpiCard
              label="Kafka consumer lag"
              value={data.kafkaConsumerLag.toLocaleString()}
              trend={`${sloLabel(data.sloKafkaLagMet)} · target < 1,000`}
              icon={<Radio />}
            />
            <KpiCard
              label="Model drift"
              value={data.driftScore.toFixed(3)}
              trend={`${data.driftAlert ? 'Alert active' : sloLabel(data.sloDriftMet)} · target < 0.35`}
              icon={<AlertTriangle />}
            />
            <KpiCard
              label="Service status"
              value={data.serviceStatus}
              trend={`${data.scoredLastHour.toLocaleString()} scored last hour`}
              icon={<Server />}
            />
          </div>

          <div className="glass-card">
            <div className="audit-toolbar">
              <h3>Grafana dashboards</h3>
              <a
                className="ghost-button"
                href={env.grafanaUrl}
                target="_blank"
                rel="noopener noreferrer"
              >
                <ExternalLink size={16} /> Open Grafana
              </a>
            </div>
            <p>Operational dashboards for latency percentiles, Kafka lag, model drift, and SLO error budget.</p>
            <div className="ops-link-grid">
              {GRAFANA_DASHBOARDS.map((dashboard) => (
                <a
                  key={dashboard.uid}
                  className="ops-dashboard-link"
                  href={grafanaDashboardUrl(dashboard.uid)}
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  <Activity size={16} />
                  <span>{dashboard.label}</span>
                  <ExternalLink size={14} />
                </a>
              ))}
            </div>
          </div>
        </>
      )}
    </div>
  );
}
