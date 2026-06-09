import { useMemo } from 'react';
import { Bar, BarChart, CartesianGrid, Cell, LabelList, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { Radar } from 'lucide-react';
import { ChartEmptyState } from '@/components/charts/ChartEmptyState';
import { ChartFrame } from '@/components/charts/ChartFrame';
import { deriveSignals } from '@/lib/fraudApi';
import { axisTick, chartPalette, tooltipStyle } from '@/lib/chartTheme';
import { formatSignalLabel, riskTier } from '@/lib/chartUtils';
import type { AuditReasonLeaderboardItem, DecisionEvent } from '@/types/fraud';

type SignalRow = {
  signal: string;
  contribution: number;
  volume: number;
  rank: number;
};

function SignalTooltip({
  active,
  payload
}: {
  active?: boolean;
  payload?: Array<{ payload: SignalRow }>;
}) {
  if (!active || !payload?.length) return null;
  const row = payload[0].payload;
  const tier = riskTier(row.contribution);

  return (
    <div className="rich-chart-tooltip" style={tooltipStyle}>
      <div className="rich-chart-tooltip-head">
        <strong>{row.signal}</strong>
        <span className="rich-chart-tooltip-badge" style={{ color: tier.color, borderColor: `${tier.color}55` }}>
          {tier.label}
        </span>
      </div>
      <div className="rich-chart-tooltip-grid">
        <div>
          <span>Avg contribution</span>
          <strong>{row.contribution.toFixed(1)}</strong>
        </div>
        <div>
          <span>Audit hits</span>
          <strong>{row.volume.toLocaleString()}</strong>
        </div>
        <div>
          <span>Rank</span>
          <strong>#{row.rank}</strong>
        </div>
      </div>
    </div>
  );
}

export function SignalContributionChart({
  event,
  leaderboard = []
}: {
  event?: DecisionEvent;
  leaderboard?: AuditReasonLeaderboardItem[];
}) {
  const usingLeaderboard = leaderboard.length > 0;

  const data = useMemo<SignalRow[]>(() => {
    const rows = usingLeaderboard
      ? leaderboard.map((item, index) => ({
          signal: formatSignalLabel(item.segment),
          contribution: Number(item.drift) || 0,
          volume: Number(item.count) || 0,
          rank: index + 1
        }))
      : deriveSignals(event).map((signal, index) => ({
          signal: formatSignalLabel(signal.name),
          contribution: Number(signal.contribution) || 0,
          volume: 1,
          rank: index + 1
        }));

    return rows
      .filter((row) => row.contribution > 0 || row.volume > 0)
      .sort((a, b) => b.contribution - a.contribution || b.volume - a.volume)
      .slice(0, 6)
      .map((row, index) => ({ ...row, rank: index + 1 }));
  }, [event, leaderboard, usingLeaderboard]);

  const peak = Math.max(8, ...data.map((row) => row.contribution), 1);
  const labelWidth = Math.min(156, Math.max(108, ...data.map((row) => row.signal.length * 7)));
  const topSignal = data[0];
  const totalHits = data.reduce((sum, row) => sum + row.volume, 0);
  const chartHeight = Math.max(260, data.length * 46 + 48);

  return (
    <div className="chart-card rich-chart span-4">
      <div className="chart-head">
        <div>
          <div className="chart-title">{usingLeaderboard ? 'Top fraud signals' : 'Signal contribution'}</div>
          <div className="chart-subtitle">
            {usingLeaderboard ? '24h audit leaderboard by hit volume and avg score impact' : 'Latest decision signal weights'}
          </div>
        </div>
        {topSignal && (
          <div className="chart-stats">
            <span className="chart-stat-pill accent">Lead: {topSignal.signal}</span>
            <span className="chart-stat-pill">{totalHits.toLocaleString()} hits</span>
          </div>
        )}
      </div>

      {data.length === 0 ? (
        <ChartEmptyState
          icon={Radar}
          title="No signal data yet"
          detail="Live decisions or audit history will populate contribution bars once scoring starts."
        />
      ) : (
        <ChartFrame height={chartHeight}>
          <ResponsiveContainer width="100%" height="100%" minWidth={0}>
            <BarChart data={data} layout="vertical" margin={{ left: 4, right: 28, top: 8, bottom: 8 }} barCategoryGap={14}>
              <CartesianGrid strokeDasharray="4 4" opacity={0.12} horizontal={false} />
              <XAxis type="number" domain={[0, Math.ceil(peak * 1.12)]} tick={axisTick} tickLine={false} axisLine={false} />
              <YAxis
                type="category"
                dataKey="signal"
                tick={axisTick}
                width={labelWidth}
                tickLine={false}
                axisLine={false}
              />
              <Tooltip cursor={{ fill: 'rgba(103,232,249,.06)' }} content={<SignalTooltip />} />
              <Bar dataKey="contribution" radius={[0, 12, 12, 0]} barSize={20} animationDuration={520}>
                {data.map((entry, index) => (
                  <Cell key={`${entry.signal}-${index}`} fill={chartPalette[index % chartPalette.length]} />
                ))}
                <LabelList
                  dataKey="contribution"
                  position="right"
                  formatter={(value) => `${Number(value ?? 0).toFixed(1)}`}
                  fill="#c9d8f2"
                  fontSize={11}
                />
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </ChartFrame>
      )}
    </div>
  );
}
