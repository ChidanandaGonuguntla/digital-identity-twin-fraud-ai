import type { Decision } from '@/types/fraud';

export function DecisionBadge({ decision }: { decision: Decision }) {
  return <span className={`decision-badge ${decision.toLowerCase()}`}>{decision}</span>;
}
