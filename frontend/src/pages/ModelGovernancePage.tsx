import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { format } from 'date-fns';
import {
  AlertTriangle,
  CheckCircle2,
  Download,
  FileSearch,
  GitBranch,
  RotateCcw,
  Scale,
  ShieldCheck,
  XCircle
} from 'lucide-react';
import { KpiCard } from '@/components/ui/KpiCard';
import { PageHeader } from '@/components/ui/PageHeader';
import { DecisionExplainabilityPanel } from '@/components/explainability/DecisionExplainabilityPanel';
import { fraudApi } from '@/lib/fraudApi';
import { env } from '@/config/env';

const POLL_MS = 15_000;

function statusClass(status: string) {
  if (status === 'ACTIVE' || status === 'APPROVED') return 'status-pill ok';
  if (status === 'PENDING_APPROVAL') return 'status-pill warn';
  if (status === 'REJECTED' || status === 'ROLLED_BACK') return 'status-pill danger';
  return 'status-pill';
}

export function ModelGovernancePage() {
  const queryClient = useQueryClient();
  const [assessmentId, setAssessmentId] = useState('');
  const [loadedAssessmentId, setLoadedAssessmentId] = useState('');
  const [rejectReason, setRejectReason] = useState('');
  const [selectedVersion, setSelectedVersion] = useState<string | null>(null);

  const { data: metadata } = useQuery({
    queryKey: ['governance-metadata'],
    queryFn: () => fraudApi.modelGovernanceMetadata(),
    enabled: !env.useMock,
    refetchInterval: POLL_MS
  });

  const { data: registry } = useQuery({
    queryKey: ['model-registry'],
    queryFn: () => fraudApi.modelRegistry(),
    enabled: !env.useMock,
    refetchInterval: POLL_MS
  });

  const { data: dataDrift } = useQuery({
    queryKey: ['model-data-drift'],
    queryFn: () => fraudApi.modelDataDrift(24),
    enabled: !env.useMock,
    refetchInterval: POLL_MS
  });

  const { data: biasReview } = useQuery({
    queryKey: ['model-bias-review'],
    queryFn: () => fraudApi.modelBiasReview(),
    enabled: !env.useMock,
    refetchInterval: POLL_MS
  });

  const { data: championChallenger } = useQuery({
    queryKey: ['champion-challenger-summary'],
    queryFn: () => fraudApi.championChallengerSummary(24),
    enabled: !env.useMock,
    refetchInterval: POLL_MS
  });

  const { data: explainability, isFetching: explainabilityLoading } = useQuery({
    queryKey: ['model-explainability', loadedAssessmentId],
    queryFn: () => fraudApi.modelExplainability(loadedAssessmentId),
    enabled: !!loadedAssessmentId.trim() && !env.useMock
  });

  const { data: narrative, isFetching: narrativeLoading } = useQuery({
    queryKey: ['decision-narrative', loadedAssessmentId],
    queryFn: () => fraudApi.decisionNarrative(loadedAssessmentId),
    enabled: !!loadedAssessmentId.trim() && !env.useMock
  });

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ['governance-metadata'] });
    queryClient.invalidateQueries({ queryKey: ['model-registry'] });
    queryClient.invalidateQueries({ queryKey: ['model-data-drift'] });
    queryClient.invalidateQueries({ queryKey: ['model-metadata'] });
  };

  const approve = useMutation({
    mutationFn: (version: string) => fraudApi.approveModel(version),
    onSuccess: invalidate
  });

  const reject = useMutation({
    mutationFn: ({ version, reason }: { version: string; reason: string }) =>
      fraudApi.rejectModel(version, reason),
    onSuccess: () => {
      setRejectReason('');
      invalidate();
    }
  });

  const submit = useMutation({
    mutationFn: (version: string) => fraudApi.submitModelForApproval(version),
    onSuccess: invalidate
  });

  const activate = useMutation({
    mutationFn: (version: string) => fraudApi.activateModel(version),
    onSuccess: invalidate
  });

  const rollback = useMutation({
    mutationFn: () => fraudApi.rollbackModel(),
    onSuccess: invalidate
  });

  const promoteChallenger = useMutation({
    mutationFn: () => fraudApi.promoteChallenger(),
    onSuccess: invalidate
  });

  const exportGovernance = useMutation({
    mutationFn: () => fraudApi.exportGovernanceAuditReport()
  });

  const exportDecisions = useMutation({
    mutationFn: () => fraudApi.exportDecisionAudit()
  });

  const selected = registry?.find((entry) => entry.modelVersion === selectedVersion) ?? registry?.[0];

  return (
    <div className="page-stack">
      <PageHeader
        eyebrow="Model Risk"
        title="Data & Model Governance"
        description="Registry, approval workflow, drift monitoring, bias review, explainability, and audit exports."
      />

      {metadata && (
        <div className="kpi-grid">
          <KpiCard label="Active model" value={metadata.modelVersion} trend={metadata.status} icon={<GitBranch />} />
          <KpiCard label="Dataset version" value={metadata.trainingDatasetVersion} trend="Training lineage" icon={<ShieldCheck />} />
          <KpiCard label="Feature schema" value={metadata.featureSchemaVersion} trend="Feature versioning" icon={<FileSearch />} />
          <KpiCard label="Drift score" value={metadata.driftScore.toFixed(3)} trend={dataDrift?.driftAlert ? 'Alert threshold breached' : 'Within tolerance'} icon={<AlertTriangle />} />
        </div>
      )}

      {metadata && (
        <div className="kpi-grid">
          <KpiCard label="AUC" value={metadata.auc.toFixed(3)} trend={`Precision ${(metadata.precision * 100).toFixed(1)}%`} icon={<Scale />} />
          <KpiCard label="Recall" value={`${(metadata.recall * 100).toFixed(1)}%`} trend={`F1 ${(metadata.f1Score * 100).toFixed(1)}%`} icon={<CheckCircle2 />} />
          <KpiCard label="False positive rate" value={`${(metadata.falsePositiveRate * 100).toFixed(2)}%`} trend="Model risk KPI" icon={<XCircle />} />
          <KpiCard
            label="Approval"
            value={metadata.approvedBy ?? 'Pending'}
            trend={metadata.approvedAt ? format(new Date(metadata.approvedAt), 'yyyy-MM-dd HH:mm') : 'Awaiting committee'}
            icon={<ShieldCheck />}
          />
        </div>
      )}

      <div className="glass-card audit-toolbar">
        <button className="ghost-button" onClick={() => exportGovernance.mutate()} disabled={exportGovernance.isPending}>
          <Download size={16} /> Governance audit CSV
        </button>
        <button className="ghost-button" onClick={() => exportDecisions.mutate()} disabled={exportDecisions.isPending}>
          <Download size={16} /> Decision audit CSV
        </button>
        <button className="ghost-button" onClick={() => rollback.mutate()} disabled={rollback.isPending}>
          <RotateCcw size={16} /> Rollback active model
        </button>
        <button className="ghost-button" onClick={() => promoteChallenger.mutate()} disabled={promoteChallenger.isPending}>
          <GitBranch size={16} /> Promote challenger
        </button>
      </div>

      {championChallenger && (
        <div className="kpi-grid">
          <KpiCard label="Champion" value={championChallenger.championVersion} trend="Production decision model" icon={<ShieldCheck />} />
          <KpiCard label="Challenger" value={championChallenger.challengerVersion} trend="Shadow scoring only" icon={<GitBranch />} />
          <KpiCard label="Shadow scored" value={championChallenger.scoredEvents.toLocaleString()} trend={`Agreement ${(championChallenger.agreementRate * 100).toFixed(1)}%`} icon={<Scale />} />
          <KpiCard label="Avg score delta" value={championChallenger.avgScoreDelta.toFixed(2)} trend={`Champion ${championChallenger.avgChampionScore.toFixed(1)} vs ${championChallenger.avgChallengerScore.toFixed(1)}`} icon={<AlertTriangle />} />
        </div>
      )}

      <div className="table-card">
        <h3>Model registry</h3>
        <table>
          <thead>
            <tr>
              <th>Version</th>
              <th>Status</th>
              <th>Dataset</th>
              <th>Schema</th>
              <th>AUC</th>
              <th>FPR</th>
              <th>Approved</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {(registry ?? []).map((entry) => (
              <tr
                key={entry.modelVersion}
                className={selectedVersion === entry.modelVersion || (!selectedVersion && entry.active) ? 'row-selected' : ''}
                onClick={() => setSelectedVersion(entry.modelVersion)}
              >
                <td>
                  <strong>{entry.modelVersion}</strong>
                  {entry.active && <span className="status-pill ok">LIVE</span>}
                </td>
                <td><span className={statusClass(entry.status)}>{entry.status}</span></td>
                <td>{entry.trainingDatasetVersion}</td>
                <td>{entry.featureSchemaVersion}</td>
                <td>{entry.auc.toFixed(3)}</td>
                <td>{(entry.falsePositiveRate * 100).toFixed(2)}%</td>
                <td>{entry.approvedBy ?? '—'}</td>
                <td className="action-cell">
                  {entry.status === 'PENDING_APPROVAL' && (
                    <button className="ghost-button" onClick={(e) => { e.stopPropagation(); approve.mutate(entry.modelVersion); }}>
                      Approve
                    </button>
                  )}
                  {!entry.active && entry.status !== 'REJECTED' && (
                    <button className="ghost-button" onClick={(e) => { e.stopPropagation(); submit.mutate(entry.modelVersion); }}>
                      Submit
                    </button>
                  )}
                  {(entry.status === 'APPROVED' || entry.status === 'ACTIVE') && !entry.active && (
                    <button className="ghost-button" onClick={(e) => { e.stopPropagation(); activate.mutate(entry.modelVersion); }}>
                      Activate
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {selected && (
        <div className="dashboard-grid rich">
          <div className="glass-card">
            <h3>Selected version</h3>
            <p>Version: <b>{selected.modelVersion}</b></p>
            <p>Status: <b>{selected.status}</b></p>
            <p>Rejection reason: <b>{selected.rejectionReason ?? '—'}</b></p>
            <div className="audit-toolbar">
              <input
                className="field-input"
                placeholder="Rejection reason"
                value={rejectReason}
                onChange={(e) => setRejectReason(e.target.value)}
              />
              <button
                className="ghost-button"
                disabled={!rejectReason.trim() || selected.active}
                onClick={() => reject.mutate({ version: selected.modelVersion, reason: rejectReason.trim() })}
              >
                Reject
              </button>
            </div>
          </div>

          {dataDrift && (
            <div className="glass-card span-2">
              <h3>Data drift monitoring</h3>
              <p>Model drift score: <b>{dataDrift.modelDriftScore.toFixed(3)}</b> · Alert: <b>{dataDrift.driftAlert ? 'YES' : 'NO'}</b></p>
              <table>
                <thead>
                  <tr>
                    <th>Feature</th>
                    <th>Baseline</th>
                    <th>Current</th>
                    <th>Drift</th>
                    <th>Severity</th>
                  </tr>
                </thead>
                <tbody>
                  {dataDrift.features.map((feature) => (
                    <tr key={feature.featureName}>
                      <td>{feature.featureName}</td>
                      <td>{feature.baselineMean.toFixed(3)}</td>
                      <td>{feature.currentMean.toFixed(3)}</td>
                      <td>{feature.driftIndex.toFixed(3)}</td>
                      <td><span className={statusClass(feature.severity === 'HIGH' ? 'REJECTED' : feature.severity === 'MEDIUM' ? 'PENDING_APPROVAL' : 'APPROVED')}>{feature.severity}</span></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          {biasReview && (
            <div className="glass-card span-2">
              <h3>Bias / fairness review</h3>
              <p>Status: <b>{biasReview.reviewStatus}</b></p>
              <p>{biasReview.notes}</p>
              <table>
                <thead>
                  <tr>
                    <th>Segment</th>
                    <th>Samples</th>
                    <th>Block rate</th>
                    <th>FPR</th>
                    <th>Status</th>
                  </tr>
                </thead>
                <tbody>
                  {biasReview.segments.map((segment) => (
                    <tr key={segment.segment}>
                      <td>{segment.segment}</td>
                      <td>{segment.sampleSize.toLocaleString()}</td>
                      <td>{segment.blockRate.toFixed(1)}%</td>
                      <td>{(segment.falsePositiveRate * 100).toFixed(2)}%</td>
                      <td>{segment.fairnessStatus}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      <div className="glass-card">
        <h3>Explainability report</h3>
        <div className="audit-toolbar">
          <input
            className="field-input"
            placeholder="Assessment ID"
            value={assessmentId}
            onChange={(e) => setAssessmentId(e.target.value)}
          />
          <button
            className="ghost-button"
            disabled={!assessmentId.trim() || explainabilityLoading || narrativeLoading}
            onClick={() => setLoadedAssessmentId(assessmentId.trim())}
          >
            Load report
          </button>
        </div>
        {narrative && <DecisionExplainabilityPanel narrative={narrative} />}
        {explainability && !narrative && (
          <div className="large-copy">
            <p>Transaction: <b>{explainability.transactionId}</b> · Decision: <b>{explainability.decision}</b> · Score: <b>{explainability.finalScore.toFixed(1)}</b></p>
            <p>Reason: <b>{explainability.finalDecisionReason}</b></p>
            <table>
              <thead>
                <tr>
                  <th>Feature</th>
                  <th>Value</th>
                  <th>Contribution</th>
                </tr>
              </thead>
              <tbody>
                {explainability.topFeatures.map((feature) => (
                  <tr key={feature.feature}>
                    <td>{feature.feature}</td>
                    <td>{feature.value.toFixed(3)}</td>
                    <td>{feature.contribution.toFixed(3)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
