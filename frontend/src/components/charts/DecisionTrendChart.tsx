import { Area, AreaChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { ChartFrame } from '@/components/charts/ChartFrame';
import { axisTick, tooltipStyle } from '@/lib/chartTheme';
import type { DecisionEvent } from '@/types/fraud';
import { format } from 'date-fns';

export function DecisionTrendChart({ decisions }: { decisions: DecisionEvent[] }) {
  const data = decisions.slice(0, 40).reverse().map((d) => ({
    time: format(new Date(d.eventTimeEpochMs), 'HH:mm:ss'),
    risk: d.riskScore
  }));
  const peak = Math.max(10, ...data.map((d) => d.risk), 1);

  return (
    <div className="chart-card tall span-8">
      <div className="chart-title">Real-time risk trend</div>
      <ChartFrame height={320}>
        <ResponsiveContainer width="100%" height="100%">
          <AreaChart data={data} margin={{ top: 16, right: 20, left: -8, bottom: 0 }}>
            <defs>
              <linearGradient id="riskGradient" x1="0" y1="0" x2="0" y2="1">
                <stop offset="5%" stopColor="#38bdf8" stopOpacity={0.55} />
                <stop offset="95%" stopColor="#a78bfa" stopOpacity={0.04} />
              </linearGradient>
            </defs>
            <CartesianGrid strokeDasharray="3 3" opacity={0.14} />
            <XAxis dataKey="time" tick={axisTick} minTickGap={18} />
            <YAxis domain={[0, Math.ceil(peak * 1.2)]} tick={axisTick} />
            <Tooltip contentStyle={tooltipStyle} formatter={(value) => [`${Number(value ?? 0).toFixed(1)}`, 'Risk score']} />
            <Area type="monotone" dataKey="risk" stroke="#67e8f9" fill="url(#riskGradient)" strokeWidth={3} dot={{ r: 2, fill: '#a78bfa' }} />
          </AreaChart>
        </ResponsiveContainer>
      </ChartFrame>
    </div>
  );
}
