import {decisionFromScore, DECISIONS} from '../../lib/constants';

export default function ScoreGauge({score = 0, size = 132}) {
    const decision = decisionFromScore(score);
    const color = DECISIONS[decision].color;
    const stroke = 10;
    const r = (size - stroke) / 2;
    const c = 2 * Math.PI * r;
    const pct = Math.max(0, Math.min(100, score)) / 100;
    const dash = c * pct;

    return (
        <div className="relative inline-flex items-center justify-center" style={{width: size, height: size}}>
            <svg width={size} height={size} className="-rotate-90">
                <circle cx={size / 2} cy={size / 2} r={r} fill="none" stroke="#1e2a35" strokeWidth={stroke}/>
                <circle
                    cx={size / 2} cy={size / 2} r={r} fill="none" stroke={color} strokeWidth={stroke}
                    strokeLinecap="round" strokeDasharray={`${dash} ${c}`}
                    style={{
                        transition: 'stroke-dasharray 0.7s cubic-bezier(0.22,1,0.36,1)',
                        filter: `drop-shadow(0 0 6px ${color}66)`
                    }}
                />
            </svg>
            <div className="absolute inset-0 flex flex-col items-center justify-center">
                <span className="stat-num text-3xl font-bold leading-none" style={{color}}>{score.toFixed(0)}</span>
                <span className="label-kicker mt-1.5">risk</span>
            </div>
        </div>
    );
}
