export const decisionColors = {
  ALLOW: '#34d399',
  CHALLENGE: '#fbbf24',
  BLOCK: '#fb7185'
} as const;

export const chartPalette = ['#38bdf8', '#a78bfa', '#34d399', '#fbbf24', '#fb7185', '#f472b6', '#22d3ee', '#818cf8'];

export const tooltipStyle = {
  background: '#0b1628',
  border: '1px solid rgba(148,163,184,.25)',
  borderRadius: 12,
  color: '#e8f1ff'
};

export const axisTick = { fill: '#8ea4c8', fontSize: 11 };

export function riskBarColor(score: number) {
  if (score >= 70) return '#fb7185';
  if (score >= 40) return '#fbbf24';
  return '#38bdf8';
}
