import { useEffect, useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { format } from 'date-fns';
import { Download, MousePointerClick } from 'lucide-react';
import { useSearchParams } from 'react-router-dom';
import { PageHeader } from '@/components/ui/PageHeader';
import { DecisionBadge } from '@/components/ui/DecisionBadge';
import { DecisionExplainabilityPanel } from '@/components/explainability/DecisionExplainabilityPanel';
import { DecisionFilterTabs } from '@/components/ui/DecisionFilterTabs';
import { TablePagination } from '@/components/ui/TablePagination';
import { KpiCard } from '@/components/ui/KpiCard';
import { fraudApi } from '@/lib/fraudApi';
import { buildFallbackNarrative } from '@/lib/explainabilityUtils';
import { normalizeDecision } from '@/lib/decisionUtils';
import { env } from '@/config/env';

const FEEDBACK_OPTIONS = [
  'CONFIRMED_FRAUD',
  'FALSE_POSITIVE',
  'CUSTOMER_VERIFIED',
  'CUSTOMER_DENIED',
  'NEEDS_INVESTIGATION'
] as const;

export function AuditTrailPage() {
  const queryClient = useQueryClient();
  const [searchParams, setSearchParams] = useSearchParams();
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(50);
  const [filter, setFilter] = useState<'ALL' | 'ALLOW' | 'CHALLENGE' | 'BLOCK'>('ALL');
  const [selectedAssessmentId, setSelectedAssessmentId] = useState<string | null>(null);
  const [feedbackOutcome, setFeedbackOutcome] = useState<string>('FALSE_POSITIVE');
  const [feedbackNotes, setFeedbackNotes] = useState('');
  const deepLinkAssessmentId = searchParams.get('assessmentId');

  const { data: summary } = useQuery({
    queryKey: ['audit-summary'],
    queryFn: () => fraudApi.auditSummary(),
    enabled: !env.useMock
  });

  const { data, isLoading, error, isFetching } = useQuery({
    queryKey: ['audit-page', page, pageSize, filter],
    queryFn: () => fraudApi.auditPage(page, pageSize, filter),
    enabled: !env.useMock,
    placeholderData: (prev) => prev
  });

  const rows = data?.items ?? [];

  useEffect(() => {
    if (deepLinkAssessmentId) {
      setSelectedAssessmentId(deepLinkAssessmentId);
    }
  }, [deepLinkAssessmentId]);

  useEffect(() => {
    if (deepLinkAssessmentId) {
      return;
    }
    if (rows.length === 0) {
      setSelectedAssessmentId(null);
      return;
    }
    setSelectedAssessmentId((current) =>
      current && rows.some((row) => row.assessmentId === current) ? current : rows[0].assessmentId
    );
  }, [rows, deepLinkAssessmentId]);

  const selectAssessment = (assessmentId: string) => {
    setSelectedAssessmentId(assessmentId);
    setSearchParams((params) => {
      const next = new URLSearchParams(params);
      next.set('assessmentId', assessmentId);
      return next;
    });
  };

  const {
    data: narrative,
    isLoading: narrativeLoading,
    isError: narrativeError,
    error: narrativeErrorObj,
    refetch: refetchNarrative
  } = useQuery({
    queryKey: ['decision-narrative', selectedAssessmentId],
    queryFn: () => fraudApi.decisionNarrative(selectedAssessmentId!),
    enabled: !env.useMock && !!selectedAssessmentId,
    retry: 1
  });

  const { data: auditDetail, isLoading: detailLoading } = useQuery({
    queryKey: ['audit-detail', selectedAssessmentId],
    queryFn: () => fraudApi.auditDetail(selectedAssessmentId!),
    enabled: !env.useMock && !!selectedAssessmentId && narrativeError
  });

  const displayNarrative = useMemo(() => {
    if (narrative) return narrative;
    if (auditDetail && narrativeError) return buildFallbackNarrative(auditDetail);
    return null;
  }, [narrative, auditDetail, narrativeError]);

  const panelLoading = !!selectedAssessmentId && (narrativeLoading || (!narrative && detailLoading && !displayNarrative));

  const submitFeedback = useMutation({
    mutationFn: () => fraudApi.submitFeedback(selectedAssessmentId!, feedbackOutcome, feedbackNotes),
    onSuccess: () => {
      setFeedbackNotes('');
      queryClient.invalidateQueries({ queryKey: ['decision-narrative', selectedAssessmentId] });
    }
  });

  const exportFeedback = useMutation({
    mutationFn: () => fraudApi.exportFeedback()
  });

  return (
    <div className="page-stack">
      <PageHeader
        eyebrow="Compliance"
        title="Decision Audit Trail"
        description="Paginated audit store from the Spring backend — 199k+ scored transactions."
      />
      {summary && (
        <div className="kpi-grid">
          <KpiCard label="Total audited" value={summary.totalEvents.toLocaleString()} trend="PostgreSQL audit store" />
          <KpiCard label="Blocked" value={summary.blocked.toLocaleString()} trend={`${summary.blockRate.toFixed(1)}% block rate`} />
          <KpiCard label="Allowed" value={summary.allowed.toLocaleString()} trend="Trusted decisions" />
          <KpiCard label="Avg risk" value={summary.averageRiskScore.toFixed(1)} trend={`P95 ${summary.p95LatencyMs}ms`} />
        </div>
      )}
      <div className="glass-card audit-toolbar">
        <DecisionFilterTabs
          value={filter}
          summary={summary}
          onChange={(next) => {
            setFilter(next);
            setPage(0);
          }}
        />
        <div className="audit-toolbar-actions">
          <button className="ghost-button" onClick={() => exportFeedback.mutate()} disabled={exportFeedback.isPending}><Download size={16} /> Export feedback</button>
          {(isLoading || isFetching) && <span className="audit-status">Loading…</span>}
        </div>
      </div>
      {error && <div className="glass-card error-banner">Failed to load audit trail: {(error as Error).message}</div>}
      <div className="dashboard-grid two">
        <div className="table-card">
          <table>
            <thead>
              <tr>
                <th>Time</th>
                <th>Transaction</th>
                <th>Customer</th>
                <th>Amount</th>
                <th>Category</th>
                <th>Risk</th>
                <th>Decision</th>
                <th>Reasons</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((item) => (
                <tr
                  key={`${item.assessmentId}-${item.transactionId}`}
                  className={selectedAssessmentId === item.assessmentId ? 'row-selected' : ''}
                  onClick={() => selectAssessment(item.assessmentId)}
                  style={{ cursor: 'pointer' }}
                >
                  <td>{format(new Date(item.assessedAt), 'yyyy-MM-dd HH:mm:ss')}</td>
                  <td>{item.transactionId}</td>
                  <td>{item.customerId}</td>
                  <td>${item.amount.toLocaleString()}</td>
                  <td>{item.merchantCategory}</td>
                  <td><b>{item.riskScore.toFixed(1)}</b></td>
                  <td><DecisionBadge decision={normalizeDecision(item.decision)} /></td>
                  <td className="reason-cell">{item.reasons.slice(0, 2).join(' · ')}{item.reasons.length > 2 ? '…' : ''}</td>
                </tr>
              ))}
            </tbody>
          </table>
          {!isLoading && rows.length === 0 && !error && (
            <div className="empty-state">No audit records found for this filter.</div>
          )}
          {data && (
            <TablePagination
              page={page}
              pageSize={pageSize}
              totalElements={data.totalElements}
              totalPages={data.totalPages}
              disabled={isLoading || isFetching}
              onPageChange={setPage}
              onPageSizeChange={(nextSize) => {
                setPageSize(nextSize);
                setPage(0);
              }}
            />
          )}
        </div>

        <div className="glass-card explainability-card">
          {!selectedAssessmentId && (
            <div className="explainability-placeholder">
              <MousePointerClick size={28} />
              <p>Select a decision row to review explainability and submit analyst feedback.</p>
            </div>
          )}
          {selectedAssessmentId && panelLoading && (
            <div className="explainability-placeholder">
              <span className="audit-status">Loading explainability…</span>
            </div>
          )}
          {selectedAssessmentId && !panelLoading && narrativeError && !displayNarrative && (
            <div className="explainability-placeholder error">
              <p>Could not load explainability for this decision.</p>
              <p className="muted-copy">{(narrativeErrorObj as Error)?.message ?? 'Unknown error'}</p>
              <button className="ghost-button" onClick={() => refetchNarrative()}>Retry</button>
            </div>
          )}
          {selectedAssessmentId && displayNarrative && (
            <>
              {narrativeError && (
                <div className="explainability-banner warn">
                  Full narrative API unavailable — showing audit detail summary. Restart backend to enable SHAP attribution.
                </div>
              )}
              <DecisionExplainabilityPanel narrative={displayNarrative} />
              <h4>Analyst feedback</h4>
              <div className="audit-toolbar">
                <select className="field-input" value={feedbackOutcome} onChange={(e) => setFeedbackOutcome(e.target.value)}>
                  {FEEDBACK_OPTIONS.map((option) => (
                    <option key={option} value={option}>{option.replace(/_/g, ' ')}</option>
                  ))}
                </select>
                <input className="field-input" placeholder="Notes for retraining labels" value={feedbackNotes} onChange={(e) => setFeedbackNotes(e.target.value)} />
                <button className="primary-button" disabled={submitFeedback.isPending} onClick={() => submitFeedback.mutate()}>Save feedback</button>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
