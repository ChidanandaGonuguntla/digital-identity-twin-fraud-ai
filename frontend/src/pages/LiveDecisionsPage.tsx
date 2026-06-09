import { format } from 'date-fns';
import { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { DecisionBadge } from '@/components/ui/DecisionBadge';
import { PageHeader } from '@/components/ui/PageHeader';
import { auditItemToDecisionEvent, fraudApi } from '@/lib/fraudApi';
import { formatDecisionAmount, formatDecisionCategory, mergeDecisionFeed } from '@/lib/decisionUtils';
import { env } from '@/config/env';
import { useDecisionStore } from '@/store/decisionStore';

export function LiveDecisionsPage() {
  const liveDecisions = useDecisionStore((s) => s.decisions);
  const status = useDecisionStore((s) => s.connectionStatus);

  const { data: auditPage } = useQuery({
    queryKey: ['audit-live-feed'],
    queryFn: () => fraudApi.auditPage(0, 100),
    enabled: !env.useMock,
    refetchInterval: 30000
  });

  const auditDecisions = useMemo(
    () => (auditPage?.items ?? []).map(auditItemToDecisionEvent),
    [auditPage?.items]
  );

  const decisions = useMemo(
    () => mergeDecisionFeed(liveDecisions, auditDecisions),
    [liveDecisions, auditDecisions]
  );

  return (
    <div className="page-stack">
      <PageHeader
        eyebrow="Stream"
        title="Live Fraud Decisions"
        description={liveDecisions.length > 0
          ? `WebSocket stream active (${status}). Audit snapshots enrich amount and category when needed.`
          : 'Showing latest audit records from backend until WebSocket events arrive.'}
      />
      <div className="table-card">
        <table>
          <thead><tr><th>Time</th><th>Transaction</th><th>Customer</th><th>Amount</th><th>Category</th><th>Risk</th><th>Decision</th><th>Signals</th></tr></thead>
          <tbody>
            {decisions.map((d) => (
              <tr key={`${d.transactionId}-${d.eventTimeEpochMs}`}>
                <td>{format(new Date(d.eventTimeEpochMs), 'HH:mm:ss')}</td>
                <td>{d.transactionId}</td>
                <td>{d.customerId}</td>
                <td>{formatDecisionAmount(d.amount)}</td>
                <td>{formatDecisionCategory(d.merchantCategory)}</td>
                <td><b>{d.riskScore.toFixed(1)}</b></td>
                <td><DecisionBadge decision={d.decision} /></td>
                <td>{d.signals.map((s) => <span className="mini-chip" key={s.name}>{s.name}</span>)}</td>
              </tr>
            ))}
          </tbody>
        </table>
        {decisions.length === 0 && <div className="empty-state">No decisions yet. Ensure backend is running on port 9997.</div>}
      </div>
    </div>
  );
}
