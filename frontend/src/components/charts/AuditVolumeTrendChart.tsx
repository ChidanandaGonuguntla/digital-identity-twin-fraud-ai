import { format } from 'date-fns';
import { Area, AreaChart, CartesianGrid, Legend, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { ChartFrame } from '@/components/charts/ChartFrame';
import { axisTick, decisionColors, tooltipStyle } from '@/lib/chartTheme';
import type { AuditTrendPoint } from '@/types/fraud';

export function AuditVolumeTrendChart({ points }: { points: AuditTrendPoint[] }) {
  const data = points.map((p) => ({
    time: format(new Date(p.bucket), 'HH:mm'),
    allow: p.allow,
    review: p.review,
    block: p.block,
    total: p.allow + p.review + p.block
  }));

  return (
    <div className="chart-card tall span-8">
      <div className="chart-title">Decision volume trend (24h)</div>
      <ChartFrame height={320}>
        <ResponsiveContainer width="100%" height="100%">
          <AreaChart data={data} margin={{ top: 16, right: 20, left: -8, bottom: 0 }}>
            <defs>
              <linearGradient id="allowFill" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stopColor={decisionColors.ALLOW} stopOpacity={0.55} />
                <stop offset="100%" stopColor={decisionColors.ALLOW} stopOpacity={0.05} />
              </linearGradient>
              <linearGradient id="reviewFill" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stopColor={decisionColors.CHALLENGE} stopOpacity={0.55} />
                <stop offset="100%" stopColor={decisionColors.CHALLENGE} stopOpacity={0.05} />
              </linearGradient>
              <linearGradient id="blockFill" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stopColor={decisionColors.BLOCK} stopOpacity={0.65} />
                <stop offset="100%" stopColor={decisionColors.BLOCK} stopOpacity={0.08} />
              </linearGradient>
            </defs>
            <CartesianGrid strokeDasharray="3 3" opacity={0.14} />
            <XAxis dataKey="time" tick={axisTick} minTickGap={24} />
            <YAxis tick={axisTick} allowDecimals={false} />
            <Tooltip contentStyle={tooltipStyle} />
            <Legend wrapperStyle={{ color: '#8ea4c8', fontSize: 12 }} />
            <Area type="monotone" dataKey="allow" stackId="1" stroke={decisionColors.ALLOW} fill="url(#allowFill)" strokeWidth={2} />
            <Area type="monotone" dataKey="review" stackId="1" stroke={decisionColors.CHALLENGE} fill="url(#reviewFill)" strokeWidth={2} />
            <Area type="monotone" dataKey="block" stackId="1" stroke={decisionColors.BLOCK} fill="url(#blockFill)" strokeWidth={2} />
          </AreaChart>
        </ResponsiveContainer>
      </ChartFrame>
    </div>
  );
}
