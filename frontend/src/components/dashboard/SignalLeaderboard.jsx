import {SIGNALS} from '../../lib/constants';

export default function SignalLeaderboard({data}) {
    const max = Math.max(1, ...data.map((d) => d.avg));
    if (!data.length) {
        return <div className="py-10 text-center text-sm text-faint">Awaiting flagged transactions…</div>;
    }
    return (
        <div className="space-y-3.5">
            {data.map((s, i) => (
                <div key={s.name}>
                    <div className="mb-1.5 flex items-center justify-between">
                        <span className="text-sm text-ink">{SIGNALS[s.name] || s.name}</span>
                        <span className="mono text-[11px] text-faint">{s.count}× · avg {s.avg.toFixed(1)}</span>
                    </div>
                    <div className="h-2 overflow-hidden rounded-full bg-base">
                        <div
                            className="h-full rounded-full"
                            style={{
                                width: `${(s.avg / max) * 100}%`,
                                background: i === 0 ? '#ff5168' : i === 1 ? '#ffb24d' : '#5aa2ff',
                                transition: 'width 0.6s cubic-bezier(0.22,1,0.36,1)',
                            }}
                        />
                    </div>
                </div>
            ))}
        </div>
    );
}
