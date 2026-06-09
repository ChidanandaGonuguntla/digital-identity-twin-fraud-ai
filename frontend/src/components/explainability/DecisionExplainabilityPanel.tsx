import { useMemo } from 'react';
import { Bar, BarChart, CartesianGrid, Cell, LabelList, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { BrainCircuit, Gauge, Scale, ShieldAlert } from 'lucide-react';
import { ChartFrame } from '@/components/charts/ChartFrame';
import { DecisionBadge } from '@/components/ui/DecisionBadge';
import { axisTick, chartPalette, tooltipStyle } from '@/lib/chartTheme';
import { normalizeDecision } from '@/lib/decisionUtils';
import type { DecisionNarrative } from '@/types/fraud';

function sourceClass(source: string) {
  if (source === 'RULE') return 'status-pill danger';
  if (source === 'TWIN') return 'status-pill warn';
  if (source === 'ML') return 'status-pill';
  return 'status-pill ok';
}

export function DecisionExplainabilityPanel({ narrative }: { narrative: DecisionNarrative }) {
  const shapData = useMemo(
    () =>
      (narrative.shapFeatures ?? [])
        .filter((item) => Math.abs(item.shapValue) > 0.01)
        .slice(0, 8)
        .map((item) => ({
          feature: item.displayName,
          shapValue: item.shapValue,
          value: item.value
        })),
    [narrative.shapFeatures]
  );

  const attribution = narrative.scoreAttribution;
  const attributionRows = attribution
    ? [
        { label: 'Rules', value: attribution.rulePoints, color: '#fb7185' },
        { label: 'Twin deviation', value: attribution.twinPoints, color: '#fbbf24' },
        { label: 'ML model', value: attribution.mlPoints, color: '#38bdf8' }
      ]
    : [];

  return (
    <div className="explainability-panel">
      <div className="explainability-head">
        <div>
          <div className="eyebrow">Explainability</div>
          <h3>{narrative.headline ?? `${narrative.decision} because:`}</h3>
        </div>
        <DecisionBadge decision={normalizeDecision(narrative.decision)} />
      </div>

      <div className="kpi-grid compact">
        <div className="mini-kpi"><Scale size={16} /> Final score <strong>{(narrative.finalScore ?? 0).toFixed(1)}</strong></div>
        <div className="mini-kpi"><BrainCircuit size={16} /> ML probability <strong>{(narrative.mlProbability ?? 0).toFixed(1)}%</strong></div>
        {narrative.championScore != null && (
          <div className="mini-kpi"><Gauge size={16} /> Champion/Challenger <strong>{narrative.championScore.toFixed(1)} / {narrative.challengerScore?.toFixed(1) ?? '—'}</strong></div>
        )}
      </div>

      <div className="explainability-section">
        <div className="section-title"><ShieldAlert size={16} /> Decision factors</div>
        <ul className="explainability-factor-list">
          {(narrative.factors ?? []).length === 0 && (
            <li className="factor-empty">No scored risk factors recorded for this decision.</li>
          )}
          {(narrative.factors ?? []).map((factor) => (
            <li key={`${factor.source}-${factor.label}-${factor.points}`}>
              <div className="factor-main">
                <strong>{factor.label}</strong>
                <span className={sourceClass(factor.source)}>{factor.source}</span>
              </div>
              <div className="factor-detail">{factor.detail}</div>
              <div className="factor-points">{factor.points.toFixed(1)} pts</div>
            </li>
          ))}
        </ul>
      </div>

      {attributionRows.length > 0 && (
        <div className="explainability-section">
          <div className="section-title">Rule / twin / ML score attribution</div>
          <div className="attribution-bar">
            {attributionRows.map((row) => (
              <div
                key={row.label}
                className="attribution-segment"
                style={{
                  width: `${Math.max(8, (row.value / Math.max(narrative.finalScore, 1)) * 100)}%`,
                  background: row.color
                }}
                title={`${row.label}: ${row.value.toFixed(1)} pts`}
              />
            ))}
          </div>
          <div className="attribution-legend">
            {attributionRows.map((row) => (
              <span key={row.label}>{row.label} {row.value.toFixed(1)}</span>
            ))}
          </div>
        </div>
      )}

      {shapData.length > 0 && (
        <div className="explainability-section">
          <div className="section-title">Top ML features (SHAP-style attribution)</div>
          <ChartFrame height={Math.max(220, shapData.length * 42)}>
            <ResponsiveContainer width="100%" height="100%" minWidth={0}>
              <BarChart data={shapData} layout="vertical" margin={{ left: 4, right: 24, top: 8, bottom: 8 }}>
                <CartesianGrid strokeDasharray="4 4" opacity={0.12} horizontal={false} />
                <XAxis type="number" tick={axisTick} tickLine={false} axisLine={false} />
                <YAxis type="category" dataKey="feature" width={150} tick={axisTick} tickLine={false} axisLine={false} />
                <Tooltip
                  cursor={{ fill: 'rgba(103,232,249,.06)' }}
                  contentStyle={tooltipStyle}
                  formatter={(value, _name, item) => [
                    `${Number(value).toFixed(2)} SHAP · value ${Number(item.payload.value).toFixed(3)}`,
                    'Impact'
                  ]}
                />
                <Bar dataKey="shapValue" radius={[0, 10, 10, 0]} barSize={18}>
                  {shapData.map((entry, index) => (
                    <Cell key={entry.feature} fill={chartPalette[index % chartPalette.length]} />
                  ))}
                  <LabelList dataKey="shapValue" position="right" formatter={(value) => Number(value).toFixed(1)} fill="#c9d8f2" fontSize={11} />
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </ChartFrame>
        </div>
      )}
    </div>
  );
}
