import type { ReactNode } from 'react';

export function KpiCard({ label, value, trend, icon }: { label: string; value: string; trend: string; icon?: ReactNode }) {
  return (
    <div className="kpi-card">
      <div className="kpi-top">
        <span>{label}</span>
        {icon}
      </div>
      <strong>{value}</strong>
      <small>{trend}</small>
    </div>
  );
}
