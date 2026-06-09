import type { LucideIcon } from 'lucide-react';

export function ChartEmptyState({
  icon: Icon,
  title,
  detail
}: {
  icon: LucideIcon;
  title: string;
  detail: string;
}) {
  return (
    <div className="chart-empty">
      <div className="chart-empty-icon">
        <Icon size={22} strokeWidth={1.8} />
      </div>
      <strong>{title}</strong>
      <p>{detail}</p>
    </div>
  );
}
