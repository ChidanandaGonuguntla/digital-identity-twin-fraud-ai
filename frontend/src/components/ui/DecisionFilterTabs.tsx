import type { AuditSummary } from '@/types/fraud';

type DecisionFilter = 'ALL' | 'ALLOW' | 'CHALLENGE' | 'BLOCK';

const FILTER_OPTIONS: {
  value: DecisionFilter;
  label: string;
  tone?: 'ok' | 'warn' | 'danger';
  countKey?: keyof Pick<AuditSummary, 'totalEvents' | 'allowed' | 'reviews' | 'blocked'>;
}[] = [
  { value: 'ALL', label: 'All', countKey: 'totalEvents' },
  { value: 'ALLOW', label: 'Allow', tone: 'ok', countKey: 'allowed' },
  { value: 'CHALLENGE', label: 'Challenge', tone: 'warn', countKey: 'reviews' },
  { value: 'BLOCK', label: 'Block', tone: 'danger', countKey: 'blocked' }
];

export function DecisionFilterTabs({
  value,
  onChange,
  summary
}: {
  value: DecisionFilter;
  onChange: (value: DecisionFilter) => void;
  summary?: AuditSummary;
}) {
  return (
    <div className="filter-tabs" role="tablist" aria-label="Filter by decision">
      {FILTER_OPTIONS.map((option) => {
        const active = value === option.value;
        const count = option.countKey && summary ? summary[option.countKey] : null;
        return (
          <button
            key={option.value}
            type="button"
            role="tab"
            aria-selected={active}
            className={`filter-tab${active ? ' active' : ''}${option.tone ? ` tone-${option.tone}` : ''}`}
            onClick={() => onChange(option.value)}
          >
            {option.label}
            {count != null && <span className="filter-tab-count">{count.toLocaleString()}</span>}
          </button>
        );
      })}
    </div>
  );
}
