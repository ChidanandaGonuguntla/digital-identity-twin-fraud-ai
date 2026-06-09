import type { AuditDecisionDetail, DecisionNarrative, RiskSignalType } from '@/types/fraud';

function signalSource(type: RiskSignalType) {
  if (type === 'RULE') return 'RULE';
  if (type === 'TWIN_DEVIATION') return 'TWIN';
  if (type === 'ML_MODEL') return 'ML';
  return 'POLICY';
}

function headlineFor(decision: string) {
  const normalized = decision.toUpperCase();
  if (normalized === 'BLOCK') return 'Blocked because:';
  if (normalized === 'CHALLENGE') return 'Challenged because:';
  if (normalized === 'ALLOW') return 'Allowed because:';
  return `${decision} because:`;
}

export function buildFallbackNarrative(detail: AuditDecisionDetail): DecisionNarrative {
  const breakdown = detail.scoreBreakdown;
  const mlProbability = detail.championScore ?? breakdown?.mlScore ?? 0;
  const factors = (detail.reasonCodes ?? [])
    .filter((signal) => signal.scoreContribution > 0 || signal.type === 'ML_MODEL')
    .slice(0, 8)
    .map((signal) => ({
      label: signal.message,
      detail: signal.evidence || signal.message,
      points: signal.scoreContribution,
      source: signalSource(signal.type)
    }));

  const bullets = factors.map((factor) => `${factor.label} contributed ${factor.points.toFixed(1)} points`);
  if (mlProbability > 0) {
    bullets.push(`ML model assigned ${mlProbability.toFixed(1)}% fraud probability`);
  }

  return {
    assessmentId: detail.assessmentId,
    decision: detail.decision,
    finalScore: breakdown?.finalScore ?? detail.riskScore,
    headline: headlineFor(detail.decision),
    narrative: bullets.slice(0, 4).join('; '),
    bullets,
    factors,
    scoreAttribution: {
      rulePoints: (breakdown?.ruleScore ?? 0) * 0.35,
      twinPoints: (breakdown?.twinDeviationScore ?? 0) * 0.35,
      mlPoints: (breakdown?.mlScore ?? 0) * 0.3,
      graphPoints: breakdown?.graphScore ?? 0,
      finalScore: breakdown?.finalScore ?? detail.riskScore,
      mlProbability
    },
    shapFeatures: [],
    topFeatures: [],
    mlProbability,
    championScore: detail.championScore ?? null,
    challengerScore: detail.challengerScore ?? null,
    scoreDelta: detail.scoreDelta ?? null,
    modelAgreement: detail.modelAgreement ?? null
  };
}
