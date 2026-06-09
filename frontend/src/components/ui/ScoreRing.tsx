export function ScoreRing({ value, label }: { value: number; label?: string }) {
  const clamped = Math.max(0, Math.min(100, value));
  return (
    <div className="score-ring" style={{ ['--score' as string]: `${clamped * 3.6}deg` }}>
      <div>
        <strong>{clamped.toFixed(0)}</strong>
        <span>{label ?? 'risk'}</span>
      </div>
    </div>
  );
}
