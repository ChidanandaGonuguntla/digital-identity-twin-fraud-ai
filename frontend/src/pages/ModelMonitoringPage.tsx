import { Activity, BrainCircuit, Gauge, Target, TimerReset } from 'lucide-react';
import { useQuery } from '@tanstack/react-query';
import { formatDistanceToNow } from 'date-fns';
import { ModelDriftTrendChart } from '@/components/charts/ModelDriftTrendChart';
import { ModelSignalMixChart } from '@/components/charts/ModelSignalMixChart';
import { KpiCard } from '@/components/ui/KpiCard';
import { PageHeader } from '@/components/ui/PageHeader';
import { fraudApi } from '@/lib/fraudApi';
import { env } from '@/config/env';

const POLL_MS = 10_000;

export function ModelMonitoringPage() {
  const { data: metadata, isLoading: metaLoading } = useQuery({
    queryKey: ['model-metadata'],
    queryFn: () => fraudApi.modelMetadata(),
    enabled: !env.useMock,
    refetchInterval: POLL_MS
  });

  const { data: live, isLoading: liveLoading } = useQuery({
    queryKey: ['model-live-metrics'],
    queryFn: () => fraudApi.modelLiveMetrics(),
    enabled: !env.useMock,
    refetchInterval: POLL_MS
  });

  const { data: quality } = useQuery({
    queryKey: ['model-quality-metrics'],
    queryFn: () => fraudApi.modelQualityMetrics(),
    enabled: !env.useMock,
    refetchInterval: POLL_MS
  });

  const { data: driftTrend } = useQuery({
    queryKey: ['model-drift-trend'],
    queryFn: () => fraudApi.modelDriftTrend(24),
    enabled: !env.useMock,
    refetchInterval: POLL_MS
  });

  const { data: health } = useQuery({
    queryKey: ['model-health'],
    queryFn: () => fraudApi.modelHealth(),
    enabled: !env.useMock,
    refetchInterval: POLL_MS
  });

  const loading = metaLoading || liveLoading;
  const precision = quality?.precision ?? metadata?.precision ?? 0;
  const recall = quality?.recall ?? metadata?.recall ?? 0;
  const auc = quality?.auc ?? metadata?.auc ?? 0;
  const fpr = quality?.falsePositiveRate ?? metadata?.falsePositiveRate ?? 0;

  return (
    <div className="page-stack">
      <PageHeader
        eyebrow="Model Risk"
        title="Model Monitoring"
        description="Live model posture and validation metrics computed from labeled audit decisions."
      />
      {loading && <div className="glass-card">Loading live model metrics…</div>}
      {metadata && live && (
        <>
          <div className="kpi-grid">
            <KpiCard label="Model" value={metadata.modelName} trend={metadata.modelVersion} icon={<BrainCircuit />} />
            <KpiCard label="Precision" value={`${Math.round(precision * 100)}%`} trend={quality ? `${quality.labeledSamples.toLocaleString()} labeled` : 'Audit labels'} icon={<Target />} />
            <KpiCard label="Recall" value={`${Math.round(recall * 100)}%`} trend={quality ? `F1 ${(quality.f1Score * 100).toFixed(1)}%` : 'Fraud capture'} icon={<Gauge />} />
            <KpiCard label="AUC" value={auc.toFixed(3)} trend={`${live.scoredLastHour.toLocaleString()} scored last hour`} icon={<Activity />} />
          </div>
          <div className="kpi-grid">
            <KpiCard label="Live drift" value={live.driftScore.toFixed(3)} trend={`Block rate ${live.blockRateLastHour.toFixed(1)}%`} icon={<Gauge />} />
            <KpiCard label="Avg latency" value={`${live.avgLatencyMs.toFixed(1)}ms`} trend={`Risk ${live.avgRiskScoreLastHour.toFixed(1)}`} icon={<TimerReset />} />
            <KpiCard label="Scored (24h)" value={live.scoredLast24Hours.toLocaleString()} trend="Audit throughput" icon={<Activity />} />
            <KpiCard label="False positive rate" value={`${(fpr * 100).toFixed(2)}%`} trend={`TP ${quality?.truePositives ?? 0} · FP ${quality?.falsePositives ?? 0}`} icon={<Target />} />
          </div>
          <div className="dashboard-grid rich">
            {driftTrend && driftTrend.length > 0 && <ModelDriftTrendChart points={driftTrend} />}
            <ModelSignalMixChart metrics={live} />
            <div className="glass-card large-copy span-4">
              <h3>Active deployment</h3>
              <p>Status: <b>{String(health?.status ?? metadata.status)}</b></p>
              <p>Model type: <b>{metadata.modelType}</b></p>
              <p>Training dataset: <b>{metadata.trainingDatasetVersion}</b></p>
              <p>Feature schema: <b>{metadata.featureSchemaVersion}</b></p>
              <p>Approved by: <b>{metadata.approvedBy ?? '—'}</b></p>
              <p>Trained at: <b>{new Date(metadata.trainedAt).toLocaleString()}</b></p>
              <p>Last scored: <b>{live.lastScoredAt ? formatDistanceToNow(new Date(live.lastScoredAt), { addSuffix: true }) : '—'}</b></p>
              <p>Validation set: <b>{quality?.labeledSamples.toLocaleString() ?? '0'} audit rows</b></p>
              <p>Confusion matrix: <b>TP {quality?.truePositives ?? 0}</b> · <b>FP {quality?.falsePositives ?? 0}</b> · <b>FN {quality?.falseNegatives ?? 0}</b> · <b>TN {quality?.trueNegatives ?? 0}</b></p>
              <p>Metrics source: <b>expectedDecision / riskLabel in audit snapshots vs BLOCK/CHALLENGE outcomes</b></p>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
