import { format } from 'date-fns';
import { Area, CartesianGrid, ComposedChart, Line, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { ChartFrame } from '@/components/charts/ChartFrame';
import { axisTick, tooltipStyle } from '@/lib/chartTheme';
import type { ModelDriftPoint } from '@/types/fraud';

export function ModelDriftTrendChart({ points }: { points: ModelDriftPoint[] }) {
  const data = points.map((p) => ({
    time: format(new Date(p.bucket), 'HH:mm'),
    drift: p.driftScore,
    scored: p.scored,
    avgRisk: p.avgRisk,
    latency: p.avgLatencyMs
  }));

  const peakScored = Math.max(1, ...data.map((d) => d.scored));

  return (
    <div className="chart-card tall span-8">
      <div className="chart-title">Live drift & scoring volume (24h)</div>
      <ChartFrame height={320}>
        <ResponsiveContainer width="100%" height="100%">
          <ComposedChart data={data} margin={{ top: 16, right: 20, left: -8, bottom: 0 }}>
            <defs>
              <linearGradient id="driftFill" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stopColor="#a78bfa" stopOpacity={0.5} />
                <stop offset="100%" stopColor="#a78bfa" stopOpacity={0.04} />
              </linearGradient>
            </defs>
            <CartesianGrid strokeDasharray="3 3" opacity={0.14} />
            <XAxis dataKey="time" tick={axisTick} minTickGap={20} />
            <YAxis yAxisId="left" tick={axisTick} domain={[0, peakScored * 1.15]} allowDecimals={false} />
            <YAxis yAxisId="right" orientation="right" domain={[0, 1]} tick={axisTick} />
            <Tooltip
              contentStyle={tooltipStyle}
              formatter={(value, name) => {
                const label = String(name ?? '');
                if (label === 'drift') return [Number(value ?? 0).toFixed(3), 'Drift'];
                if (label === 'scored') return [Number(value ?? 0).toLocaleString(), 'Scored'];
                return [String(value ?? ''), label];
              }}
            />
            <Area yAxisId="left" type="monotone" dataKey="scored" fill="url(#driftFill)" stroke="#38bdf8" strokeWidth={2} />
            <Line yAxisId="right" type="monotone" dataKey="drift" stroke="#f472b6" strokeWidth={3} dot={false} />
          </ComposedChart>
        </ResponsiveContainer>
      </ChartFrame>
    </div>
  );
}
