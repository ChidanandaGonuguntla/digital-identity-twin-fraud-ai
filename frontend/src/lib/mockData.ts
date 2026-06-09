import type { Decision, DecisionEvent, FraudDecisionResponse, ModelMetadata, TwinSummary } from '@/types/fraud';

const categories = ['electronics', 'travel', 'grocery', 'fuel', 'jewelry', 'crypto', 'restaurant', 'gift_card'];
const decisions: Decision[] = ['ALLOW', 'CHALLENGE', 'BLOCK'];
const devices = ['ios-17-8a2f', 'android-22-dd1a', 'web-chrome-aa91', 'new-device-x7'];
const cities = [
  { city: 'Charlotte', lat: 35.2271, lng: -80.8431 },
  { city: 'New York', lat: 40.7128, lng: -74.006 },
  { city: 'London', lat: 51.5072, lng: -0.1276 },
  { city: 'Singapore', lat: 1.3521, lng: 103.8198 }
];

export function createMockDecision(i = Date.now()): DecisionEvent {
  const category = categories[Math.floor(Math.random() * categories.length)];
  const base = category === 'crypto' || category === 'jewelry' ? 72 : category === 'electronics' ? 54 : 22;
  const riskScore = Math.min(99, Math.round((base + Math.random() * 28) * 10) / 10);
  const decision: Decision = riskScore >= 75 ? 'BLOCK' : riskScore >= 45 ? 'CHALLENGE' : 'ALLOW';
  const city = cities[Math.floor(Math.random() * cities.length)];
  const signals = [
    { name: 'rule_score', contribution: Math.round(riskScore * 0.35 * 10) / 10 },
    { name: 'twin_deviation', contribution: Math.round(riskScore * 0.35 * 10) / 10 },
    { name: 'ml_model', contribution: Math.round(riskScore * 0.30 * 10) / 10 }
  ];
  return {
    transactionId: `TXN-${i.toString().slice(-7)}`,
    customerId: `CUST-${1000 + Math.floor(Math.random() * 9000)}`,
    amount: Math.round((25 + Math.random() * 2500) * 100) / 100,
    merchantCategory: category,
    deviceId: devices[Math.floor(Math.random() * devices.length)],
    latitude: city.lat,
    longitude: city.lng,
    eventTimeEpochMs: Date.now(),
    riskScore,
    decision,
    coldStart: Math.random() < 0.06,
    signals,
    reasons: decision === 'ALLOW'
      ? ['Transaction consistent with behavioral twin']
      : ['Elevated deviation from customer twin', `Merchant category ${category} requires attention`]
  };
}

export const initialDecisions = Array.from({ length: 24 }, (_, i) => createMockDecision(Date.now() - i * 10000));

export const modelMetadata: ModelMetadata = {
  modelName: 'digital-twin-fraud-risk',
  modelVersion: 'fraud-risk-v1.0.0-embedded',
  modelType: 'heuristic-baseline-replace-with-xgboost-or-onnx',
  trainingDatasetVersion: 'fraud-synthetic-v1.0.0',
  featureSchemaVersion: 'fraud-features-v1.0.0',
  status: 'ACTIVE',
  trainedAt: '2026-01-01T00:00:00Z',
  loadedAt: new Date().toISOString(),
  precision: 0.91,
  recall: 0.87,
  auc: 0.94,
  f1Score: 0.89,
  falsePositiveRate: 0.04,
  driftScore: 0.04,
  approvedBy: 'model-risk-committee@citizens.com',
  approvedAt: '2026-01-01T00:00:00Z'
};

export const twinSummary: TwinSummary = {
  customerId: 'CUST-2198',
  transactionCount: 1842,
  knownDevices: 4,
  usualCountries: ['US', 'CA'],
  amountMean: 68.74,
  amountStdDev: 41.22,
  topCategories: [
    { category: 'grocery', count: 522 },
    { category: 'fuel', count: 311 },
    { category: 'restaurant', count: 286 },
    { category: 'electronics', count: 79 },
    { category: 'travel', count: 22 }
  ]
};

export function toResponse(event: DecisionEvent): FraudDecisionResponse {
  return {
    assessmentId: `ASM-${event.transactionId}`,
    transactionId: event.transactionId,
    customerId: event.customerId,
    decision: event.decision,
    finalScore: event.riskScore,
    scoreBreakdown: {
      ruleScore: event.signals[0]?.contribution ?? 0,
      twinDeviationScore: event.signals[1]?.contribution ?? 0,
      mlScore: event.signals[2]?.contribution ?? 0,
      graphScore: 0,
      finalScore: event.riskScore
    },
    reasonCodes: event.reasons.map((reason, index) => ({
      type: index === 0 ? 'TWIN_DEVIATION' : 'RULE',
      code: reason.toUpperCase().replaceAll(' ', '_'),
      message: reason,
      severity: event.decision === 'BLOCK' ? 'HIGH' : event.decision === 'CHALLENGE' ? 'MEDIUM' : 'LOW',
      scoreContribution: event.signals[index]?.contribution ?? 0,
      evidence: `riskScore=${event.riskScore}`
    })),
    modelVersion: modelMetadata.modelVersion,
    policyVersion: 'fraud-policy-v1.0.0',
    latencyMs: 19 + Math.floor(Math.random() * 80),
    assessedAt: new Date(event.eventTimeEpochMs).toISOString()
  };
}
