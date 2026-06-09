export function slugifyChartId(value: string) {
  return value
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '') || 'item';
}

export function formatSignalLabel(value: string) {
  return value
    .replaceAll('_', ' ')
    .replace(/\s+/g, ' ')
    .trim()
    .replace(/\b\w/g, (char) => char.toUpperCase());
}

export function formatCategoryLabel(value: string) {
  const normalized = value?.trim();
  if (!normalized) return 'Unknown';
  return formatSignalLabel(normalized);
}

export function riskTier(score: number) {
  if (score >= 70) return { label: 'Critical', color: '#fb7185' };
  if (score >= 40) return { label: 'Elevated', color: '#fbbf24' };
  return { label: 'Low', color: '#38bdf8' };
}

export function compactCurrency(value: number) {
  if (value >= 1_000_000) return `$${(value / 1_000_000).toFixed(1)}M`;
  if (value >= 1_000) return `$${(value / 1_000).toFixed(1)}K`;
  return `$${Math.round(value).toLocaleString()}`;
}

export function scoreBucketMidpoint(bucket: string) {
  const start = Number.parseInt(bucket.split('-')[0] ?? '', 10);
  return Number.isFinite(start) ? start + 5 : 0;
}

export function scoreBucketLabel(start: number) {
  return `${start}-${start + 9}`;
}
