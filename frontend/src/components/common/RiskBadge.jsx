import {DECISIONS} from '../../lib/constants';

export default function RiskBadge({decision, size = 'sm'}) {
    const meta = DECISIONS[decision] || DECISIONS.ALLOW;
    const pad = size === 'lg' ? 'px-3 py-1.5 text-xs' : 'px-2 py-1 text-[11px]';
    return (
        <span
            className={`chip ${pad} font-semibold uppercase tracking-wider`}
            style={{color: meta.color, background: meta.tint, border: `1px solid ${meta.color}33`}}
        >
      <span className="inline-block h-1.5 w-1.5 rounded-full" style={{background: meta.color}}/>
            {meta.label}
    </span>
    );
}
