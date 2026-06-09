import type { FraudCaseStats } from '@/types/fraud';

export type CaseStatusFilter =
  | 'ALL'
  | 'OPEN'
  | 'ASSIGNED'
  | 'IN_REVIEW'
  | 'WAITING_CUSTOMER'
  | 'CONFIRMED_FRAUD'
  | 'FALSE_POSITIVE'
  | 'CLOSED';

const FILTER_OPTIONS: {
  value: CaseStatusFilter;
  label: string;
  tone?: 'ok' | 'warn' | 'danger';
  countKey?: keyof FraudCaseStats;
}[] = [
  { value: 'ALL', label: 'All', countKey: 'totalCases' },
  { value: 'OPEN', label: 'Open', tone: 'warn', countKey: 'openCases' },
  { value: 'ASSIGNED', label: 'Assigned', countKey: 'assignedCases' },
  { value: 'IN_REVIEW', label: 'In review', countKey: 'inReviewCases' },
  { value: 'WAITING_CUSTOMER', label: 'Waiting', countKey: 'waitingCustomerCases' },
  { value: 'CONFIRMED_FRAUD', label: 'Confirmed', tone: 'danger', countKey: 'confirmedFraudCases' },
  { value: 'FALSE_POSITIVE', label: 'False positive', tone: 'ok', countKey: 'falsePositiveCases' },
  { value: 'CLOSED', label: 'Closed', countKey: 'closedCases' }
];

export function CaseStatusFilterTabs({
  value,
  onChange,
  stats
}: {
  value: CaseStatusFilter;
  onChange: (value: CaseStatusFilter) => void;
  stats?: FraudCaseStats;
}) {
  return (
    <div className="filter-tabs filter-tabs-scroll" role="tablist" aria-label="Filter by case status">
      {FILTER_OPTIONS.map((option) => {
        const active = value === option.value;
        const count = option.countKey && stats ? stats[option.countKey] : null;
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
