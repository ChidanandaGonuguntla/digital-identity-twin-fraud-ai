import { Activity, Ban, ShieldAlert, TimerReset } from 'lucide-react';
import { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { AuditVolumeTrendChart } from '@/components/charts/AuditVolumeTrendChart';
import { CategoryRiskChart } from '@/components/charts/CategoryRiskChart';
import { DecisionMixChart } from '@/components/charts/DecisionMixChart';
import { DecisionTrendChart } from '@/components/charts/DecisionTrendChart';
import { ScoreDistributionChart } from '@/components/charts/ScoreDistributionChart';
import { SignalContributionChart } from '@/components/charts/SignalContributionChart';
import { DecisionBadge } from '@/components/ui/DecisionBadge';
import { KpiCard } from '@/components/ui/KpiCard';
import { PageHeader } from '@/components/ui/PageHeader';
import { ScoreRing } from '@/components/ui/ScoreRing';
import { auditItemToDecisionEvent, fraudApi } from '@/lib/fraudApi';
import { env } from '@/config/env';
import { useDecisionStore } from '@/store/decisionStore';

export function CommandCenterPage() {
  const liveDecisions = useDecisionStore((s) => s.decisions);

  const { data: summary } = useQuery({
    queryKey: ['audit-summary'],
    queryFn: () => fraudApi.auditSummary(),
    enabled: !env.useMock,
    refetchInterval: 60000
  });

  const { data: auditSample } = useQuery({
    queryKey: ['audit-sample-charts'],
    queryFn: () => fraudApi.auditPage(0, 200),
    enabled: !env.useMock
  });

  const { data: trends } = useQuery({
    queryKey: ['audit-trends'],
    queryFn: () => fraudApi.auditTrends(24),
    enabled: !env.useMock,
    refetchInterval: 60000
  });

  const { data: scoreBuckets } = useQuery({
    queryKey: ['audit-score-distribution'],
    queryFn: () => fraudApi.auditScoreDistribution(),
    enabled: !env.useMock
  });

  const { data: reasonLeaderboard } = useQuery({
    queryKey: ['audit-reason-leaderboard'],
    queryFn: () => fraudApi.auditReasonLeaderboard(),
    enabled: !env.useMock
  });

  const sampleDecisions = useMemo(
    () => (auditSample?.items ?? []).map(auditItemToDecisionEvent),
    [auditSample?.items]
  );

  const decisions = liveDecisions.length > 0 ? liveDecisions : sampleDecisions;
  const latest = decisions[0];

  const blocked = env.useMock ? decisions.filter((d) => d.decision === 'BLOCK').length : summary?.blocked ?? 0;
  const challenged = env.useMock ? decisions.filter((d) => d.decision === 'CHALLENGE').length : summary?.reviews ?? 0;
  const eventCount = env.useMock ? decisions.length : summary?.totalEvents ?? 0;
  const avgRisk = env.useMock
    ? decisions.reduce((sum, d) => sum + d.riskScore, 0) / Math.max(1, decisions.length)
    : summary?.averageRiskScore ?? 0;

  const mix = summary
    ? { allow: summary.allowed, challenge: summary.reviews, block: summary.blocked }
    : undefined;

  return (
    <div className="page-stack">
      <PageHeader
        eyebrow="Operations"
        title="Fraud Command Center"
        description="Real-time decision intelligence across rule, twin deviation, and ML scoring engines."
      />
      <div className="kpi-grid">
        <KpiCard label="Events monitored" value={eventCount.toLocaleString()} trend={env.useMock ? 'Last rolling window' : 'Historical audit store'} icon={<Activity />} />
        <KpiCard label="Blocked" value={blocked.toLocaleString()} trend={`${summary?.blockRate?.toFixed(1) ?? 0}% block rate`} icon={<Ban />} />
        <KpiCard label="Challenged" value={challenged.toLocaleString()} trend="Step-up required" icon={<ShieldAlert />} />
        <KpiCard label="Average risk" value={avgRisk.toFixed(1)} trend={summary ? `P95 latency ${summary.p95LatencyMs}ms` : 'Composite score'} icon={<TimerReset />} />
      </div>
      <div className="dashboard-grid rich">
        {trends && trends.length > 0 ? (
          <AuditVolumeTrendChart points={trends} />
        ) : (
          <DecisionTrendChart decisions={decisions} />
        )}
        <div className="latest-card span-4">
          <div className="chart-title">Latest decision</div>
          {latest ? (
            <>
              <div className="latest-top"><ScoreRing value={latest.riskScore} /><DecisionBadge decision={latest.decision} /></div>
              <h3>{latest.transactionId}</h3>
              <p>{latest.customerId} · ${latest.amount.toLocaleString()} · {latest.merchantCategory}</p>
              <ul>{latest.reasons.map((r) => <li key={r}>{r}</li>)}</ul>
            </>
          ) : (
            <p>Waiting for live WebSocket decisions…</p>
          )}
        </div>
        <DecisionMixChart decisions={decisions} mix={mix} />
        <SignalContributionChart event={latest} leaderboard={reasonLeaderboard ?? []} />
        <ScoreDistributionChart buckets={scoreBuckets ?? []} />
        <CategoryRiskChart decisions={sampleDecisions.length ? sampleDecisions : decisions} />
      </div>
    </div>
  );
}
