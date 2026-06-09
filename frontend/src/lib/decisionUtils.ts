import type { Decision, DecisionEvent, SignalContribution } from '@/types/fraud';

function coerceNumber(value: unknown): number {
  if (typeof value === 'number' && Number.isFinite(value)) {
    return value;
  }
  if (typeof value === 'string' && value.trim()) {
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : 0;
  }
  return 0;
}

function coerceString(value: unknown): string {
  return typeof value === 'string' ? value.trim() : '';
}

function coerceSignals(value: unknown): SignalContribution[] {
  if (!Array.isArray(value)) {
    return [];
  }
  return value
    .map((entry) => {
      if (!entry || typeof entry !== 'object') {
        return null;
      }
      const row = entry as Record<string, unknown>;
      const name = coerceString(row.name);
      const contribution = coerceNumber(row.contribution);
      if (!name) {
        return null;
      }
      return { name, contribution };
    })
    .filter((entry): entry is SignalContribution => entry !== null);
}

function coerceReasons(value: unknown): string[] {
  if (!Array.isArray(value)) {
    return [];
  }
  return value.map((entry) => coerceString(entry)).filter(Boolean);
}

export function normalizeDecisionEvent(raw: Record<string, unknown>): DecisionEvent {
  const amount = coerceNumber(raw.amount ?? raw.transactionAmount ?? raw.txnAmount);
  const merchantCategory = coerceString(raw.merchantCategory ?? raw.merchant_category ?? raw.category);
  const signals = coerceSignals(raw.signals);
  const reasons = coerceReasons(raw.reasons);
  const eventTimeEpochMs = coerceNumber(raw.eventTimeEpochMs) || Date.now();

  return {
    transactionId: coerceString(raw.transactionId),
    customerId: coerceString(raw.customerId),
    amount,
    merchantCategory,
    deviceId: coerceString(raw.deviceId ?? raw.device_id),
    latitude: coerceNumber(raw.latitude),
    longitude: coerceNumber(raw.longitude),
    eventTimeEpochMs,
    riskScore: coerceNumber(raw.riskScore ?? raw.finalScore),
    decision: normalizeDecision(coerceString(raw.decision)),
    coldStart: Boolean(raw.coldStart),
    signals,
    reasons
  };
}

export function mergeDecisionFeed(live: DecisionEvent[], audit: DecisionEvent[]): DecisionEvent[] {
  if (live.length === 0) {
    return audit;
  }

  const auditByTransaction = new Map(audit.map((entry) => [entry.transactionId, entry]));

  return live.map((entry) => {
    const snapshot = auditByTransaction.get(entry.transactionId);
    if (!snapshot) {
      return entry;
    }

    return normalizeDecisionEvent({
      ...entry,
      amount: entry.amount > 0 ? entry.amount : snapshot.amount,
      merchantCategory: entry.merchantCategory || snapshot.merchantCategory,
      deviceId: entry.deviceId || snapshot.deviceId,
      latitude: entry.latitude || snapshot.latitude,
      longitude: entry.longitude || snapshot.longitude,
      signals: entry.signals.length > 0 ? entry.signals : snapshot.signals,
      reasons: entry.reasons.length > 0 ? entry.reasons : snapshot.reasons
    });
  });
}

export function formatDecisionAmount(amount: number) {
  if (!Number.isFinite(amount) || amount <= 0) {
    return '—';
  }
  return `$${amount.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
}

export function formatDecisionCategory(category: string) {
  const normalized = category?.trim();
  if (!normalized) {
    return '—';
  }
  return normalized.replaceAll('_', ' ').replace(/\b\w/g, (char) => char.toUpperCase());
}

export function normalizeDecision(value?: string | null): Decision {
  const upper = String(value ?? 'ALLOW').toUpperCase();
  if (upper === 'BLOCK' || upper === 'CHALLENGE' || upper === 'ALLOW') {
    return upper;
  }
  return 'ALLOW';
}

export function normalizeCustomerId(customerId: string): string {
  return customerId.trim().toUpperCase();
}
