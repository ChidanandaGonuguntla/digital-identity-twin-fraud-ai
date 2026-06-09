import { Cell, Pie, PieChart, ResponsiveContainer, Tooltip } from 'recharts';
import { ChartFrame } from '@/components/charts/ChartFrame';
import { decisionColors, tooltipStyle } from '@/lib/chartTheme';
import type { DecisionEvent } from '@/types/fraud';

type MixInput = {
  allow: number;
  challenge: number;
  block: number;
};

export function DecisionMixChart({
  decisions = [],
  mix
}: {
  decisions?: DecisionEvent[];
  mix?: MixInput;
}) {
  const counts = mix ?? decisions.reduce<MixInput>(
    (acc, d) => {
      if (d.decision === 'ALLOW') acc.allow += 1;
      else if (d.decision === 'CHALLENGE') acc.challenge += 1;
      else acc.block += 1;
      return acc;
    },
    { allow: 0, challenge: 0, block: 0 }
  );

  const data = [
    { name: 'ALLOW', value: counts.allow, color: decisionColors.ALLOW },
    { name: 'CHALLENGE', value: counts.challenge, color: decisionColors.CHALLENGE },
    { name: 'BLOCK', value: counts.block, color: decisionColors.BLOCK }
  ].filter((d) => d.value > 0);

  const total = counts.allow + counts.challenge + counts.block;

  return (
    <div className="chart-card span-4">
      <div className="chart-title">Decision mix</div>
      <ChartFrame height={240}>
        <ResponsiveContainer width="100%" height="100%">
          <PieChart>
            <defs>
              {data.map((entry) => (
                <linearGradient id={`mix-${entry.name}`} key={entry.name} x1="0" y1="0" x2="1" y2="1">
                  <stop offset="0%" stopColor={entry.color} stopOpacity={1} />
                  <stop offset="100%" stopColor={entry.color} stopOpacity={0.55} />
                </linearGradient>
              ))}
            </defs>
            <Pie
              data={data.length ? data : [{ name: 'EMPTY', value: 1, color: '#334155' }]}
              dataKey="value"
              nameKey="name"
              innerRadius={62}
              outerRadius={92}
              paddingAngle={3}
              stroke="#0b1628"
              strokeWidth={3}
            >
              {(data.length ? data : [{ name: 'EMPTY', value: 1, color: '#334155' }]).map((entry) => (
                <Cell key={entry.name} fill={`url(#mix-${entry.name})`} />
              ))}
            </Pie>
            <Tooltip
              contentStyle={tooltipStyle}
              formatter={(value, name) => [
                `${Number(value ?? 0).toLocaleString()} (${total ? ((Number(value ?? 0) / total) * 100).toFixed(1) : 0}%)`,
                String(name ?? '')
              ]}
            />
          </PieChart>
        </ResponsiveContainer>
      </ChartFrame>
      <div className="legend-row rich">
        {['ALLOW', 'CHALLENGE', 'BLOCK'].map((name) => {
          const value = name === 'ALLOW' ? counts.allow : name === 'CHALLENGE' ? counts.challenge : counts.block;
          return (
            <span key={name} className="legend-chip" style={{ borderColor: decisionColors[name as keyof typeof decisionColors] }}>
              <i style={{ background: decisionColors[name as keyof typeof decisionColors] }} />
              {name}: <b>{value.toLocaleString()}</b>
            </span>
          );
        })}
      </div>
    </div>
  );
}
