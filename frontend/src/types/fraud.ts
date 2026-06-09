export type Decision = 'ALLOW' | 'CHALLENGE' | 'BLOCK';
export type Severity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type RiskSignalType = 'RULE' | 'TWIN_DEVIATION' | 'ML_MODEL' | 'GRAPH' | 'POLICY';

export interface ScoreBreakdown {
  ruleScore: number;
  twinDeviationScore: number;
  mlScore: number;
  graphScore: number;
  finalScore: number;
}

export interface RiskSignal {
  type: RiskSignalType;
  code: string;
  message: string;
  severity: Severity;
  scoreContribution: number;
  evidence: string;
}

export interface FraudDecisionResponse {
  assessmentId: string;
  transactionId: string;
  customerId: string;
  decision: Decision;
  finalScore: number;
  scoreBreakdown: ScoreBreakdown;
  reasonCodes: RiskSignal[];
  modelVersion: string;
  policyVersion: string;
  latencyMs: number;
  assessedAt: string;
}

export interface SignalContribution {
  name: string;
  contribution: number;
}

export interface DecisionEvent {
  transactionId: string;
  customerId: string;
  amount: number;
  merchantCategory: string;
  deviceId: string;
  latitude: number;
  longitude: number;
  eventTimeEpochMs: number;
  riskScore: number;
  decision: Decision;
  coldStart: boolean;
  signals: SignalContribution[];
  reasons: string[];
}

export interface FraudDecisionRequest {
  customerId: string;
  transactionId: string;
  amount: number;
  currency: string;
  merchantCategory: string;
  merchantName?: string;
  merchantId?: string;
  deviceId: string;
  ipAddress?: string;
  userAgent?: string;
  channel: string;
  paymentInstrumentId?: string;
  latitude: number;
  longitude: number;
  countryCode?: string;
  city?: string;
  timestamp: string;
}

export interface ModelMetadata {
  modelName: string;
  modelVersion: string;
  modelType: string;
  trainingDatasetVersion: string;
  featureSchemaVersion: string;
  status: string;
  trainedAt: string;
  loadedAt: string;
  precision: number;
  recall: number;
  auc: number;
  f1Score: number;
  falsePositiveRate: number;
  driftScore: number;
  approvedBy?: string | null;
  approvedAt?: string | null;
}

export interface ModelRegistryEntry {
  modelName: string;
  modelVersion: string;
  trainingDatasetVersion: string;
  featureSchemaVersion: string;
  status: string;
  active: boolean;
  auc: number;
  precision: number;
  recall: number;
  f1Score: number;
  falsePositiveRate: number;
  driftScore: number;
  approvedBy?: string | null;
  approvedAt?: string | null;
  trainedAt?: string | null;
  deployedAt?: string | null;
  rejectionReason?: string | null;
}

export interface FeatureDriftMetric {
  featureName: string;
  baselineMean: number;
  currentMean: number;
  driftIndex: number;
  severity: string;
}

export interface DataDriftSummary {
  modelVersion: string;
  featureSchemaVersion: string;
  modelDriftScore: number;
  driftAlert: boolean;
  evaluatedAt: string;
  features: FeatureDriftMetric[];
}

export interface BiasSegmentReview {
  segment: string;
  sampleSize: number;
  blockRate: number;
  falsePositiveRate: number;
  fairnessStatus: string;
}

export interface BiasFairnessReview {
  reviewStatus: string;
  lastReviewedAt?: string | null;
  modelVersion: string;
  segments: BiasSegmentReview[];
  notes: string;
}

export interface FeatureContribution {
  feature: string;
  value: number;
  contribution: number;
}

export interface ScoreAttribution {
  rulePoints: number;
  twinPoints: number;
  mlPoints: number;
  graphPoints: number;
  finalScore: number;
  mlProbability: number;
}

export interface ShapFeature {
  feature: string;
  displayName: string;
  value: number;
  shapValue: number;
  impact: string;
}

export interface ExplainabilityFactor {
  label: string;
  detail: string;
  points: number;
  source: string;
}

export interface ModelExplainabilityReport {
  assessmentId: string;
  transactionId: string;
  customerId: string;
  decision: string;
  finalScore: number;
  modelVersion: string;
  featureSchemaVersion: string;
  policyVersion: string;
  finalDecisionReason: string;
  scoreBreakdown: ScoreBreakdown;
  reasonCodes: RiskSignal[];
  featureVector: Record<string, number>;
  topFeatures: FeatureContribution[];
  assessedAt: string;
}

export interface TwinSummary {
  customerId: string;
  transactionCount: number;
  knownDevices: number;
  usualCountries: string[];
  amountMean: number;
  amountStdDev: number;
  topCategories: Array<{ category: string; count: number }>;
}

export interface TwinExplorerResponse {
  customerId: string;
  transactionCount: number;
  amountMean: number;
  amountStdDev: number;
  amountTotal: number;
  lastMerchantCategory: string;
  topMerchantCategory: string;
  knownDevices: string[];
  usualCountries: string[];
  merchantCategories: Array<{
    category: string;
    displayCategory: string;
    count: number;
    frequency: number;
    riskOverlay: number;
  }>;
  createdAt: string;
  updatedAt: string;
  coldStart: boolean;
}

export interface AuditSummary {
  totalEvents: number;
  blocked: number;
  reviews: number;
  allowed: number;
  averageRiskScore: number;
  blockRate: number;
  p95LatencyMs: number;
  preventedAmount: number;
}

export interface AuditDecisionItem {
  assessmentId: string;
  transactionId: string;
  customerId: string;
  decision: Decision | string;
  riskScore: number;
  amount: number;
  merchantCategory: string;
  deviceId: string;
  latitude: number;
  longitude: number;
  latencyMs: number;
  assessedAt: string;
  reasons: string[];
  coldStart: boolean;
  modelVersion?: string;
  policyVersion?: string;
  featureVersion?: string;
  finalDecisionReason?: string;
  challenged?: boolean;
  twinUpdated?: boolean;
}

export interface AuditDecisionDetail extends AuditDecisionItem {
  scoreBreakdown: ScoreBreakdown;
  reasonCodes: RiskSignal[];
  eventSnapshot: Record<string, unknown>;
  featureVector: Record<string, number>;
  championScore?: number | null;
  challengerScore?: number | null;
  scoreDelta?: number | null;
  modelAgreement?: boolean | null;
}

export interface AuditPage {
  items: AuditDecisionItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface AuditTrendPoint {
  bucket: string;
  allow: number;
  review: number;
  block: number;
}

export interface AuditScoreBucket {
  bucket: string;
  count: number;
}

export interface AuditReasonLeaderboardItem {
  segment: string;
  drift: number;
  count: number;
}

export interface PlatformOpsSummary {
  p95LatencyMs: number;
  kafkaConsumerLag: number;
  driftScore: number;
  driftAlert: boolean;
  serviceStatus: string;
  scoredLastHour: number;
  sloLatencyMet: boolean;
  sloKafkaLagMet: boolean;
  sloDriftMet: boolean;
  evaluatedAt: string;
}

export interface ModelQualityMetrics {
  labeledSamples: number;
  truePositives: number;
  falsePositives: number;
  falseNegatives: number;
  trueNegatives: number;
  precision: number;
  recall: number;
  auc: number;
  f1Score: number;
  falsePositiveRate: number;
}

export interface ModelLiveMetrics {
  scoredLastHour: number;
  scoredLast24Hours: number;
  lastScoredAt: string | null;
  avgLatencyMs: number;
  avgRiskScoreLastHour: number;
  avgMlScoreLastHour: number;
  avgRuleScoreLastHour: number;
  avgTwinScoreLastHour: number;
  blockRateLastHour: number;
  driftScore: number;
}

export interface StepUpChallengeResponse {
  challengeId: string;
  status: string;
  message: string;
  createdAt: string;
}

export interface StepUpChallengeItem {
  challengeId: string;
  assessmentId: string;
  customerId: string;
  transactionId: string;
  status: string;
  deliveryChannel: string;
  reasonDescription: string;
  finalRiskScore: number;
  expiresAt: string;
  createdAt: string;
}

export interface StepUpChallengePage {
  items: StepUpChallengeItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface ModelDriftPoint {
  bucket: string;
  scored: number;
  avgRisk: number;
  riskSpread: number;
  driftScore: number;
  avgLatencyMs: number;
}

export type FeedbackOutcome =
  | 'CONFIRMED_FRAUD'
  | 'FALSE_POSITIVE'
  | 'CUSTOMER_VERIFIED'
  | 'CUSTOMER_DENIED'
  | 'NEEDS_INVESTIGATION';

export interface AnalystFeedback {
  feedbackId: string;
  assessmentId: string;
  transactionId: string;
  customerId: string;
  outcome: FeedbackOutcome | string;
  analystId: string;
  notes?: string | null;
  createdAt: string;
}

export type CaseStatus =
  | 'OPEN'
  | 'ASSIGNED'
  | 'IN_REVIEW'
  | 'WAITING_CUSTOMER'
  | 'CONFIRMED_FRAUD'
  | 'FALSE_POSITIVE'
  | 'CLOSED';

export interface FraudCaseSummary {
  caseId: string;
  assessmentId?: string | null;
  transactionId: string;
  customerId: string;
  status: CaseStatus | string;
  priority: string;
  assignedTo?: string | null;
  slaDueAt?: string | null;
  escalationLevel: number;
  summary?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface FraudCaseEvent {
  eventId: string;
  eventType: string;
  actorId?: string | null;
  payload: Record<string, unknown>;
  createdAt: string;
}

export interface FraudCaseDetail extends FraudCaseSummary {
  closureReason?: string | null;
  closedAt?: string | null;
  events: FraudCaseEvent[];
}

export interface FraudCasePage {
  items: FraudCaseSummary[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface FraudCaseStats {
  totalCases: number;
  activeCases: number;
  openCases: number;
  assignedCases: number;
  inReviewCases: number;
  waitingCustomerCases: number;
  confirmedFraudCases: number;
  falsePositiveCases: number;
  closedCases: number;
  highPriorityCases: number;
  slaBreachedCases: number;
}

export interface FraudCaseBackfillResult {
  created: number;
  skipped: number;
  remaining: number;
}

export interface CustomerVelocity {
  customerId: string;
  txnCount5m: number;
  txnCount1h: number;
  txnCount24h: number;
  amountSum1h: number;
  newDevices24h: number;
  countries24h: number;
  categoryChanges10m: number;
  failedAttempts30m: number;
  updatedAt: string;
}

export interface Customer360TimelineEvent {
  eventType: string;
  eventId: string;
  title: string;
  description?: string | null;
  occurredAt: string;
  metadata: Record<string, unknown>;
}

export interface Customer360Timeline {
  customerId: string;
  profile: TwinExplorerResponse;
  velocity: CustomerVelocity;
  events: Customer360TimelineEvent[];
}

export interface DecisionNarrative {
  assessmentId: string;
  decision: string;
  finalScore: number;
  headline: string;
  narrative: string;
  bullets: string[];
  factors: ExplainabilityFactor[];
  scoreAttribution: ScoreAttribution;
  shapFeatures: ShapFeature[];
  topFeatures: FeatureContribution[];
  mlProbability: number;
  championScore?: number | null;
  challengerScore?: number | null;
  scoreDelta?: number | null;
  modelAgreement?: boolean | null;
}

export interface ChampionChallengerSummary {
  championVersion: string;
  challengerVersion: string;
  scoredEvents: number;
  avgChampionScore: number;
  avgChallengerScore: number;
  avgScoreDelta: number;
  agreementRate: number;
}

export interface FeatureStoreEntry {
  entityKey: string;
  featureName: string;
  featureValue: number;
  updatedAt: string;
}

export interface FeatureStoreCatalog {
  featureNames: string[];
  totalEntries: number;
}
