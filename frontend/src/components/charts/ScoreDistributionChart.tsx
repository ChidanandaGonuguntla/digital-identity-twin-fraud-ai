import { useMemo } from 'react';
import {
  Bar,
  CartesianGrid,
  Cell,
  ComposedChart,
  Line,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis
} from 'recharts';
import { BarChart3 } from 'lucide-react';
import { ChartEmptyState } from '@/components/charts/ChartEmptyState';
import { ChartFrame } from '@/components/charts/ChartFrame';
import { axisTick, riskBarColor, tooltipStyle } from '@/lib/chartTheme';
import { riskTier, scoreBucketLabel, slugifyChartId } from '@/lib/chartUtils';
import type { AuditScoreBucket } from '@/types/fraud';

type BucketRow = {
  id: string;
  bucket: string;
  count: number;
  midpoint: number;
  share: number;
  cumulative: number;
};

function ScoreTooltip({
  active,
  payload
}: {
  active?: boolean;
  payload?: Array<{ payload: BucketRow }>;
}) {
  if (!active || !payload?.length) return null;
  const row = payload[0].payload;
  const tier = riskTier(row.midpoint);

  return (
    <div className="rich-chart-tooltip" style={tooltipStyle}>
      <div className="rich-chart-tooltip-head">
        <strong>Score {row.bucket}</strong>
        <span className="rich-chart-tooltip-badge" style={{ color: tier.color, borderColor: `${tier.color}55` }}>
          {tier.label}
        </span>
      </div>
      <div className="rich-chart-tooltip-grid">
        <div>
          <span>Decisions</span>
          <strong>{row.count.toLocaleString()}</strong>
        </div>
        <div>
          <span>Share</span>
          <strong>{row.share.toFixed(1)}%</strong>
        </div>
        <div>
          <span>Cumulative</span>
          <strong>{row.cumulative.toFixed(1)}%</strong>
        </div>
      </div>
    </div>
  );
}

function buildBucketRows(buckets: AuditScoreBucket[]): BucketRow[] {
  const lookup = new Map(buckets.map((bucket) => [bucket.bucket, bucket.count]));
  const slots = Array.from({ length: 10 }, (_, index) => index * 10).map((start) => {
    const label = scoreBucketLabel(start);
    return {
      id: slugifyChartId(label),
      bucket: label,
      count: lookup.get(label) ?? 0,
      midpoint: start + 5,
      share: 0,
      cumulative: 0
    };
  });

  const total = slots.reduce((sum, row) => sum + row.count, 0);
  if (total === 0) return [];

  let running = 0;
  return slots.map((row) => {
    const share = (row.count / total) * 100;
    running += share;
    return { ...row, share, cumulative: running };
  });
}

export function ScoreDistributionChart({ buckets }: { buckets: AuditScoreBucket[] }) {
  const data = useMemo(() => buildBucketRows(buckets), [buckets]);

  const total = useMemo(() => data.reduce((sum, row) => sum + row.count, 0), [data]);
  const peak = useMemo(() => [...data].sort((a, b) => b.count - a.count)[0], [data]);
  const highRiskShare = useMemo(
    () => data.filter((row) => row.midpoint >= 70).reduce((sum, row) => sum + row.share, 0),
    [data]
  );
  const elevatedShare = useMemo(
    () => data.filter((row) => row.midpoint >= 40 && row.midpoint < 70).reduce((sum, row) => sum + row.share, 0),
    [data]
  );
  const volumePeak = Math.max(1, ...data.map((row) => row.count));

  return (
    <div className="chart-card rich-chart span-4">
      <div className="chart-head">
        <div>
          <div className="chart-title">Risk score distribution</div>
          <div className="chart-subtitle">Historical audit histogram with cumulative risk curve across score bands</div>
        </div>
        {data.length > 0 && (
          <div className="chart-stats">
            <span className="chart-stat-pill accent">{total.toLocaleString()} scored</span>
            {peak && peak.count > 0 && <span className="chart-stat-pill">Peak: {peak.bucket}</span>}
            <span className="chart-stat-pill warn">{highRiskShare.toFixed(1)}% critical</span>
          </div>
        )}
      </div>

      <div className="chart-legend-row">
        <span><i style={{ background: riskBarColor(15) }} /> Low 0-39</span>
        <span><i style={{ background: riskBarColor(55) }} /> Elevated 40-69</span>
        <span><i style={{ background: riskBarColor(85) }} /> Critical 70+</span>
        <span className="chart-legend-line"><i /> Cumulative %</span>
      </div>

      {data.length === 0 ? (
        <ChartEmptyState
          icon={BarChart3}
          title="No score distribution yet"
          detail="Score buckets populate once audited fraud decisions are stored in the platform."
        />
      ) : (
        <ChartFrame height={300}>
          <ResponsiveContainer width="100%" height="100%" minWidth={0}>
            <ComposedChart data={data} margin={{ top: 14, right: 18, left: -6, bottom: 4 }} barCategoryGap="18%">
              <defs>
                {data.map((entry) => (
                  <linearGradient id={`score-${entry.id}`} key={entry.id} x1="0" y1="1" x2="0" y2="0">
                    <stop offset="0%" stopColor={riskBarColor(entry.midpoint)} stopOpacity={0.3} />
                    <stop offset="100%" stopColor={riskBarColor(entry.midpoint)} stopOpacity={0.98} />
                  </linearGradient>
                ))}
              </defs>
              <CartesianGrid strokeDasharray="4 4" opacity={0.12} vertical={false} />
              <XAxis dataKey="bucket" tick={axisTick} tickLine={false} axisLine={false} interval={0} />
              <YAxis
                yAxisId="volume"
                tick={axisTick}
                allowDecimals={false}
                domain={[0, Math.ceil(volumePeak * 1.15)]}
                tickLine={false}
                axisLine={false}
              />
              <YAxis
                yAxisId="cumulative"
                orientation="right"
                tick={axisTick}
                domain={[0, 100]}
                tickFormatter={(value) => `${value}%`}
                tickLine={false}
                axisLine={false}
                width={42}
              />
              <Tooltip cursor={{ fill: 'rgba(103,232,249,.06)' }} content={<ScoreTooltip />} />
              <Bar yAxisId="volume" dataKey="count" radius={[10, 10, 0, 0]} barSize={26} animationDuration={520}>
                {data.map((entry) => (
                  <Cell key={entry.id} fill={`url(#score-${entry.id})`} />
                ))}
              </Bar>
              <Line
                yAxisId="cumulative"
                type="monotone"
                dataKey="cumulative"
                stroke="#a78bfa"
                strokeWidth={2.5}
                dot={{ r: 3, fill: '#a78bfa', stroke: '#0b1628', strokeWidth: 2 }}
                activeDot={{ r: 5, fill: '#c4b5fd', stroke: '#0b1628', strokeWidth: 2 }}
              />
            </ComposedChart>
          </ResponsiveContainer>
        </ChartFrame>
      )}

      {data.length > 0 && (
        <div className="chart-footnote">
          <span>{elevatedShare.toFixed(1)}% in challenge band</span>
          <span>{highRiskShare.toFixed(1)}% in block band</span>
        </div>
      )}
    </div>
  );
}
