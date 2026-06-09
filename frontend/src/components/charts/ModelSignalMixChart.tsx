import { Bar, BarChart, CartesianGrid, Cell, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { ChartFrame } from '@/components/charts/ChartFrame';
import { axisTick, chartPalette, tooltipStyle } from '@/lib/chartTheme';
import type { ModelLiveMetrics } from '@/types/fraud';

export function ModelSignalMixChart({ metrics }: { metrics: ModelLiveMetrics }) {
  const data = [
    { signal: 'rule score', value: metrics.avgRuleScoreLastHour },
    { signal: 'twin deviation', value: metrics.avgTwinScoreLastHour },
    { signal: 'ml model', value: metrics.avgMlScoreLastHour }
  ];
  const peak = Math.max(10, ...data.map((d) => d.value));

  return (
    <div className="chart-card span-4">
      <div className="chart-title">Live signal mix (last hour)</div>
      <ChartFrame height={260}>
        <ResponsiveContainer width="100%" height="100%">
          <BarChart data={data} layout="vertical" margin={{ left: 8, right: 20, top: 8, bottom: 8 }}>
            <CartesianGrid strokeDasharray="3 3" opacity={0.14} horizontal={false} />
            <XAxis type="number" domain={[0, Math.ceil(peak * 1.15)]} tick={axisTick} />
            <YAxis type="category" dataKey="signal" tick={axisTick} width={110} />
            <Tooltip contentStyle={tooltipStyle} formatter={(value) => [`${Number(value ?? 0).toFixed(1)}`, 'Avg contribution']} />
            <Bar dataKey="value" radius={[0, 10, 10, 0]} barSize={22}>
              {data.map((entry, index) => (
                <Cell key={entry.signal} fill={chartPalette[index % chartPalette.length]} />
              ))}
            </Bar>
          </BarChart>
        </ResponsiveContainer>
      </ChartFrame>
    </div>
  );
}
