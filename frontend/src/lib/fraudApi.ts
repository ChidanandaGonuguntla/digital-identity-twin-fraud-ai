import { apiClient } from '@/lib/apiClient';
import { normalizeDecision, normalizeDecisionEvent } from '@/lib/decisionUtils';
import { env } from '@/config/env';
import { getAccessToken } from '@/store/authStore';
import type {
  AuditDecisionDetail,
  AuditPage,
  AuditReasonLeaderboardItem,
  AuditScoreBucket,
  AuditSummary,
  AuditTrendPoint,
  AnalystFeedback,
  BiasFairnessReview,
  ChampionChallengerSummary,
  Customer360Timeline,
  CustomerVelocity,
  DataDriftSummary,
  DecisionEvent,
  DecisionNarrative,
  FeatureStoreEntry,
  FraudCaseDetail,
  FraudCasePage,
  FraudCaseStats,
  FraudCaseBackfillResult,
  FraudDecisionRequest,
  FraudDecisionResponse,
  ModelDriftPoint,
  ModelExplainabilityReport,
  ModelLiveMetrics,
  ModelMetadata,
  ModelQualityMetrics,
  ModelRegistryEntry,
  PlatformOpsSummary,
  StepUpChallengeItem,
  StepUpChallengePage,
  StepUpChallengeResponse,
  SignalContribution,
  TwinExplorerResponse
} from '@/types/fraud';

async function downloadCsv(path: string, filename: string) {
  const token = getAccessToken();
  const response = await fetch(`${env.apiBaseUrl}${path}`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {}
  });
  if (!response.ok) {
    const body = await response.text().catch(() => '');
    throw new Error(body || response.statusText);
  }
  const blob = await response.blob();
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = filename;
  anchor.click();
  URL.revokeObjectURL(url);
}

export const fraudApi = {
  evaluate: (body: FraudDecisionRequest) => apiClient.post<FraudDecisionResponse>('/fraud/decisions', body),
  modelMetadata: () => apiClient.get<ModelMetadata>('/models/fraud-risk/metadata'),
  modelHealth: () => apiClient.get<Record<string, unknown>>('/models/fraud-risk/health'),
  modelLiveMetrics: () => apiClient.get<ModelLiveMetrics>('/models/fraud-risk/live-metrics'),
  modelDriftTrend: (hours = 24) => apiClient.get<ModelDriftPoint[]>(`/models/fraud-risk/drift-trend?hours=${hours}`),
  modelQualityMetrics: () => apiClient.get<ModelQualityMetrics>('/models/fraud-risk/quality-metrics'),
  modelGovernanceMetadata: () => apiClient.get<ModelMetadata>('/models/governance/metadata'),
  modelRegistry: () => apiClient.get<ModelRegistryEntry[]>('/models/governance/registry'),
  modelDataDrift: (hours = 24) => apiClient.get<DataDriftSummary>(`/models/governance/data-drift?hours=${hours}`),
  modelBiasReview: () => apiClient.get<BiasFairnessReview>('/models/governance/bias-review'),
  modelExplainability: (assessmentId: string) =>
    apiClient.get<ModelExplainabilityReport>(`/models/governance/explainability/${encodeURIComponent(assessmentId)}`),
  approveModel: (modelVersion: string, note?: string) =>
    apiClient.post<ModelRegistryEntry>('/models/governance/approve', { modelVersion, note }),
  rejectModel: (modelVersion: string, reason: string) =>
    apiClient.post<ModelRegistryEntry>('/models/governance/reject', { modelVersion, reason }),
  submitModelForApproval: (modelVersion: string) =>
    apiClient.post<ModelRegistryEntry>('/models/governance/submit', { modelVersion }),
  activateModel: (modelVersion: string) =>
    apiClient.post<Record<string, unknown>>(`/models/governance/activate/${encodeURIComponent(modelVersion)}`, {}),
  rollbackModel: () => apiClient.post<Record<string, unknown>>('/models/admin/rollback', {}),
  platformOpsSummary: () => apiClient.get<PlatformOpsSummary>('/ops/summary'),
  exportGovernanceAuditReport: () => downloadCsv('/models/governance/audit-report', 'model-governance-audit.csv'),
  exportDecisionAudit: (decision?: string) => {
    const params = new URLSearchParams();
    if (decision && decision !== 'ALL') params.set('decision', decision);
    const query = params.toString();
    return downloadCsv(`/audit/decisions/export${query ? `?${query}` : ''}`, 'fraud-decisions-audit.csv');
  },
  auditSummary: () => apiClient.get<AuditSummary>('/audit/decisions/summary'),
  auditPage: (page = 0, size = 50, decision?: string, customerId?: string, transactionId?: string) => {
    const params = new URLSearchParams({ page: String(page), size: String(size) });
    if (decision && decision !== 'ALL') params.set('decision', decision);
    if (customerId) params.set('customerId', customerId);
    if (transactionId) params.set('transactionId', transactionId);
    return apiClient.get<AuditPage>(`/audit/decisions?${params.toString()}`);
  },
  auditDetail: (assessmentId: string) =>
    apiClient.get<AuditDecisionDetail>(`/audit/decisions/${encodeURIComponent(assessmentId)}`),
  auditByCustomer: (customerId: string, page = 0, size = 50) =>
    apiClient.get<AuditPage>(
      `/audit/customers/${encodeURIComponent(customerId)}?page=${page}&size=${size}`
    ),
  auditByTransaction: (transactionId: string, page = 0, size = 50) =>
    apiClient.get<AuditPage>(
      `/audit/transactions/${encodeURIComponent(transactionId)}?page=${page}&size=${size}`
    ),
  twinExplorer: (customerId: string) =>
    apiClient.get<TwinExplorerResponse>(`/twins/${encodeURIComponent(customerId)}/explorer`),
  auditTrends: (hours = 24) => apiClient.get<AuditTrendPoint[]>(`/audit/decisions/trends?hours=${hours}`),
  auditScoreDistribution: () => apiClient.get<AuditScoreBucket[]>('/audit/decisions/score-distribution'),
  auditReasonLeaderboard: () => apiClient.get<AuditReasonLeaderboardItem[]>('/audit/decisions/reason-leaderboard'),
  stepUpQueue: (page = 0, size = 50) =>
    apiClient.get<StepUpChallengePage>(`/challenges/queue?page=${page}&size=${size}`),
  stepUpChallenges: (page = 0, size = 50, status = 'PENDING') =>
    apiClient.get<StepUpChallengePage>(`/challenges?page=${page}&size=${size}&status=${status}`),
  stepUpChallenge: (challengeId: string) =>
    apiClient.get<StepUpChallengeItem>(`/challenges/${encodeURIComponent(challengeId)}`),
  approveStepUpChallenge: (challengeId: string) =>
    apiClient.post<StepUpChallengeResponse>(`/challenges/${encodeURIComponent(challengeId)}/approve`, {}),
  rejectStepUpChallenge: (challengeId: string) =>
    apiClient.post<StepUpChallengeResponse>(`/challenges/${encodeURIComponent(challengeId)}/reject`, {}),
  expireStepUpChallenge: (challengeId: string) =>
    apiClient.post<StepUpChallengeResponse>(`/challenges/${encodeURIComponent(challengeId)}/expire`, {}),
  createStepUpChallenge: (body: {
    assessmentId: string;
    customerId: string;
    transactionId: string;
    channel: string;
    reason: string;
  }) => apiClient.post<StepUpChallengeResponse>('/challenges', body),
  submitFeedback: (assessmentId: string, outcome: string, notes?: string) =>
    apiClient.post<AnalystFeedback>(`/feedback/${encodeURIComponent(assessmentId)}`, { outcome, notes }),
  exportFeedback: () => downloadCsv('/feedback/export', 'analyst-feedback.csv'),
  feedbackList: () => apiClient.get<AnalystFeedback[]>('/feedback'),
  cases: (
    page = 0,
    size = 50,
    status?: string,
    customerId?: string,
    transactionId?: string,
    assignedTo?: string,
    priority?: string
  ) => {
    const params = new URLSearchParams({ page: String(page), size: String(size) });
    if (status && status !== 'ALL') params.set('status', status);
    if (customerId?.trim()) params.set('customerId', customerId.trim());
    if (transactionId?.trim()) params.set('transactionId', transactionId.trim());
    if (assignedTo?.trim()) params.set('assignedTo', assignedTo.trim());
    if (priority && priority !== 'ALL') params.set('priority', priority);
    return apiClient.get<FraudCasePage>(`/cases?${params.toString()}`);
  },
  caseSummary: () => apiClient.get<FraudCaseStats>('/cases/summary'),
  backfillCases: (limit = 500) =>
    apiClient.post<FraudCaseBackfillResult>(`/cases/backfill?limit=${limit}`, {}),
  caseDetail: (caseId: string) => apiClient.get<FraudCaseDetail>(`/cases/${encodeURIComponent(caseId)}`),
  assignCase: (caseId: string, assignedTo: string) =>
    apiClient.post<FraudCaseDetail>(`/cases/${encodeURIComponent(caseId)}/assign`, { assignedTo }),
  updateCaseStatus: (caseId: string, status: string, closureReason?: string, notes?: string) =>
    apiClient.post<FraudCaseDetail>(`/cases/${encodeURIComponent(caseId)}/status`, {
      status,
      closureReason,
      notes
    }),
  addCaseNote: (caseId: string, note: string) =>
    apiClient.post<FraudCaseDetail>(`/cases/${encodeURIComponent(caseId)}/notes`, { note }),
  escalateCase: (caseId: string) =>
    apiClient.post<FraudCaseDetail>(`/cases/${encodeURIComponent(caseId)}/escalate`, {}),
  customerTimeline: (customerId: string) =>
    apiClient.get<Customer360Timeline>(`/customers/${encodeURIComponent(customerId)}/timeline`),
  customerVelocity: (customerId: string) =>
    apiClient.get<CustomerVelocity>(`/customers/${encodeURIComponent(customerId)}/velocity`),
  decisionNarrative: (assessmentId: string) =>
    apiClient.get<DecisionNarrative>(`/audit/decisions/${encodeURIComponent(assessmentId)}/narrative`),
  championChallengerSummary: (hours = 24) =>
    apiClient.get<ChampionChallengerSummary>(`/models/champion-challenger/summary?hours=${hours}`),
  promoteChallenger: () => apiClient.post<Record<string, unknown>>('/models/champion-challenger/promote', {}),
  featureStore: (featureName?: string, customerId?: string) => {
    const params = new URLSearchParams();
    if (featureName) params.set('featureName', featureName);
    if (customerId) params.set('customerId', customerId);
    const query = params.toString();
    return apiClient.get<FeatureStoreEntry[]>(`/feature-store${query ? `?${query}` : ''}`);
  },
  featureStoreCatalog: () => apiClient.get<{ featureNames: string[]; totalEntries: number }>('/feature-store/catalog'),
  authConfig: () => apiClient.get<{ securityEnabled: boolean; provider: string; issuerUri?: string }>('/auth/config')
};

export function deriveSignals(event?: DecisionEvent): SignalContribution[] {
  if (!event) return [];
  if (event.signals.length > 0) return event.signals;
  if (event.reasons.length === 0) {
    return event.riskScore > 0 ? [{ name: 'composite_risk', contribution: Math.round(event.riskScore * 10) / 10 }] : [];
  }
  const share = event.riskScore > 0 ? event.riskScore / event.reasons.length : 24 / event.reasons.length;
  return event.reasons.slice(0, 6).map((reason, index) => ({
    name: reason.split(' ').slice(0, 3).join('_').toLowerCase() || `signal_${index + 1}`,
    contribution: Math.round(share * 10) / 10
  }));
}

export function auditItemToDecisionEvent(item: AuditPage['items'][number]): DecisionEvent {
  return normalizeDecisionEvent({
    transactionId: item.transactionId,
    customerId: item.customerId,
    amount: item.amount,
    merchantCategory: item.merchantCategory,
    deviceId: item.deviceId,
    latitude: item.latitude,
    longitude: item.longitude,
    eventTimeEpochMs: new Date(item.assessedAt).getTime(),
    riskScore: item.riskScore,
    decision: item.decision,
    coldStart: item.coldStart,
    signals: deriveSignals({
      transactionId: item.transactionId,
      customerId: item.customerId,
      amount: item.amount,
      merchantCategory: item.merchantCategory,
      deviceId: item.deviceId,
      latitude: item.latitude,
      longitude: item.longitude,
      eventTimeEpochMs: new Date(item.assessedAt).getTime(),
      riskScore: item.riskScore,
      decision: normalizeDecision(item.decision),
      coldStart: item.coldStart,
      signals: [],
      reasons: item.reasons
    }),
    reasons: item.reasons
  });
}
