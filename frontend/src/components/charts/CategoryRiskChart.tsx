import { useMemo } from 'react';
import { Bar, CartesianGrid, ComposedChart, Line, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { Store } from 'lucide-react';
import { ChartEmptyState } from '@/components/charts/ChartEmptyState';
import { ChartFrame } from '@/components/charts/ChartFrame';
import { axisTick, decisionColors, riskBarColor, tooltipStyle } from '@/lib/chartTheme';
import { compactCurrency, formatCategoryLabel, riskTier, slugifyChartId } from '@/lib/chartUtils';
import type { DecisionEvent } from '@/types/fraud';

type CategoryRow = {
  id: string;
  category: string;
  label: string;
  avg: number;
  count: number;
  volume: number;
  blocked: number;
  challenged: number;
  allowed: number;
  share: number;
  exposureShare: number;
  blockRate: number;
  interventionRate: number;
};

function CategoryTooltip({
  active,
  payload
}: {
  active?: boolean;
  payload?: Array<{ payload: CategoryRow }>;
}) {
  if (!active || !payload?.length) return null;
  const row = payload[0].payload;
  const tier = riskTier(row.avg);

  return (
    <div className="rich-chart-tooltip wide" style={tooltipStyle}>
      <div className="rich-chart-tooltip-head">
        <strong>{row.label}</strong>
        <span className="rich-chart-tooltip-badge" style={{ color: tier.color, borderColor: `${tier.color}55` }}>
          {tier.label}
        </span>
      </div>
      <div className="rich-chart-tooltip-grid">
        <div>
          <span>Events</span>
          <strong>{row.count.toLocaleString()}</strong>
        </div>
        <div>
          <span>Traffic share</span>
          <strong>{row.share.toFixed(1)}%</strong>
        </div>
        <div>
          <span>Avg risk</span>
          <strong>{row.avg.toFixed(1)}</strong>
        </div>
        <div>
          <span>Exposure</span>
          <strong>{compactCurrency(row.volume)}</strong>
        </div>
        <div>
          <span>Exposure share</span>
          <strong>{row.exposureShare.toFixed(1)}%</strong>
        </div>
        <div>
          <span>Intervention</span>
          <strong>{row.interventionRate.toFixed(1)}%</strong>
        </div>
        <div>
          <span>Blocked</span>
          <strong>{row.blocked.toLocaleString()}</strong>
        </div>
        <div>
          <span>Challenged</span>
          <strong>{row.challenged.toLocaleString()}</strong>
        </div>
        <div>
          <span>Block rate</span>
          <strong>{row.blockRate.toFixed(1)}%</strong>
        </div>
      </div>
    </div>
  );
}

function buildCategoryRows(decisions: DecisionEvent[]): CategoryRow[] {
  const buckets = decisions.reduce<Record<string, CategoryRow>>((acc, decision) => {
    const category = decision.merchantCategory?.trim();
    if (!category) {
      return acc;
    }
    const current =
      acc[category] ??
      ({
        id: slugifyChartId(category),
        category,
        label: formatCategoryLabel(category),
        avg: 0,
        count: 0,
        volume: 0,
        blocked: 0,
        challenged: 0,
        allowed: 0,
        share: 0,
        exposureShare: 0,
        blockRate: 0,
        interventionRate: 0
      } satisfies CategoryRow);

    current.avg = (current.avg * current.count + decision.riskScore) / (current.count + 1);
    current.count += 1;
    current.volume += decision.amount || 0;
    if (decision.decision === 'BLOCK') current.blocked += 1;
    else if (decision.decision === 'CHALLENGE') current.challenged += 1;
    else current.allowed += 1;
    acc[category] = current;
    return acc;
  }, {});

  const rows = Object.values(buckets).sort((a, b) => b.count - a.count || b.avg - a.avg).slice(0, 8);
  const totalEvents = rows.reduce((sum, row) => sum + row.count, 0);
  const totalExposure = rows.reduce((sum, row) => sum + row.volume, 0);
  if (totalEvents === 0) return [];

  return rows.map((row) => ({
    ...row,
    share: (row.count / totalEvents) * 100,
    exposureShare: totalExposure > 0 ? (row.volume / totalExposure) * 100 : 0,
    blockRate: (row.blocked / row.count) * 100,
    interventionRate: ((row.blocked + row.challenged) / row.count) * 100
  }));
}

export function CategoryRiskChart({ decisions }: { decisions: DecisionEvent[] }) {
  const grouped = useMemo(() => buildCategoryRows(decisions), [decisions]);

  const totalEvents = useMemo(() => grouped.reduce((sum, row) => sum + row.count, 0), [grouped]);
  const totalExposure = useMemo(() => grouped.reduce((sum, row) => sum + row.volume, 0), [grouped]);
  const highestRisk = useMemo(() => [...grouped].sort((a, b) => b.avg - a.avg)[0], [grouped]);
  const highestVolume = useMemo(() => [...grouped].sort((a, b) => b.count - a.count)[0], [grouped]);
  const criticalShare = useMemo(
    () => grouped.filter((row) => row.avg >= 70).reduce((sum, row) => sum + row.share, 0),
    [grouped]
  );
  const interventionShare = useMemo(
    () =>
      grouped.reduce((sum, row) => sum + (row.blocked + row.challenged), 0) / Math.max(1, totalEvents) * 100,
    [grouped, totalEvents]
  );

  const eventPeak = Math.max(1, ...grouped.map((row) => row.count));
  const labelWidth = Math.min(176, Math.max(118, ...grouped.map((row) => row.label.length * 7)));
  const chartHeight = Math.max(340, grouped.length * 48 + 72);

  return (
    <div className="chart-card rich-chart span-12">
      <div className="chart-head">
        <div>
          <div className="chart-title">Merchant category exposure</div>
          <div className="chart-subtitle">
            Event volume, dollar exposure, and average risk curve across top merchant categories
          </div>
        </div>
        {grouped.length > 0 && (
          <div className="chart-stats">
            <span className="chart-stat-pill accent">{grouped.length} categories</span>
            {highestVolume && <span className="chart-stat-pill">Top: {highestVolume.label}</span>}
            {highestRisk && <span className="chart-stat-pill warn">Hot: {highestRisk.label}</span>}
            <span className="chart-stat-pill">{compactCurrency(totalExposure)} exposure</span>
          </div>
        )}
      </div>

      <div className="chart-legend-row">
        <span><i style={{ background: 'rgba(52,211,153,.65)' }} /> Allowed</span>
        <span><i style={{ background: decisionColors.CHALLENGE }} /> Challenged</span>
        <span><i style={{ background: decisionColors.BLOCK }} /> Blocked</span>
        <span className="chart-legend-line"><i style={{ background: '#a78bfa' }} /> Avg risk</span>
        <span><i style={{ background: riskBarColor(82) }} /> Critical tier 70+</span>
      </div>

      {grouped.length === 0 ? (
        <ChartEmptyState
          icon={Store}
          title="No merchant exposure yet"
          detail="Category bars appear after audited transactions include merchant category metadata."
        />
      ) : (
        <ChartFrame height={chartHeight}>
          <ResponsiveContainer width="100%" height="100%" minWidth={0}>
            <ComposedChart
              data={grouped}
              layout="vertical"
              margin={{ left: 4, right: 18, top: 18, bottom: 8 }}
              barCategoryGap={14}
            >
              <CartesianGrid strokeDasharray="4 4" opacity={0.12} horizontal={false} />
              <YAxis type="category" dataKey="label" tick={axisTick} width={labelWidth} tickLine={false} axisLine={false} />
              <XAxis
                xAxisId="events"
                type="number"
                domain={[0, Math.ceil(eventPeak * 1.15)]}
                tick={axisTick}
                allowDecimals={false}
                tickLine={false}
                axisLine={false}
              />
              <XAxis
                xAxisId="risk"
                type="number"
                orientation="top"
                domain={[0, 100]}
                tick={axisTick}
                tickLine={false}
                axisLine={false}
                tickFormatter={(value) => `${value}`}
              />
              <Tooltip cursor={{ fill: 'rgba(103,232,249,.06)' }} content={<CategoryTooltip />} />
              <Bar xAxisId="events" dataKey="allowed" stackId="mix" fill="rgba(52,211,153,.42)" barSize={24} animationDuration={520} />
              <Bar xAxisId="events" dataKey="challenged" stackId="mix" fill={decisionColors.CHALLENGE} barSize={24} />
              <Bar xAxisId="events" dataKey="blocked" stackId="mix" fill={decisionColors.BLOCK} radius={[0, 12, 12, 0]} barSize={24} />
              <Line
                xAxisId="risk"
                type="monotone"
                dataKey="avg"
                stroke="#a78bfa"
                strokeWidth={2.5}
                dot={{ r: 3, fill: '#a78bfa', stroke: '#0b1628', strokeWidth: 2 }}
                activeDot={{ r: 5, fill: '#c4b5fd', stroke: '#0b1628', strokeWidth: 2 }}
              />
            </ComposedChart>
          </ResponsiveContainer>
        </ChartFrame>
      )}

      {grouped.length > 0 && (
        <div className="chart-footnote">
          <span>{totalEvents.toLocaleString()} events across top categories</span>
          <span>{criticalShare.toFixed(1)}% traffic in critical-risk categories</span>
          <span>{interventionShare.toFixed(1)}% challenge or block rate</span>
        </div>
      )}
    </div>
  );
}
