import {useState} from 'react';
import {AnimatePresence, motion} from 'framer-motion';
import {useStore} from '../store/useStore';
import RiskBadge from '../components/common/RiskBadge';
import TxnDetailDrawer from '../components/feed/TxnDetailDrawer';
import {cityFromCoords, fmtCurrency2, fmtTime} from '../lib/format';

const FILTERS = ['ALL', 'ALLOW', 'CHALLENGE', 'BLOCK'];

export default function LiveFeed() {
    const txns = useStore((s) => s.transactions);
    const [filter, setFilter] = useState('ALL');
    const [selected, setSelected] = useState(null);

    const rows = filter === 'ALL' ? txns : txns.filter((t) => t.decision === filter);

    return (
        <div className="space-y-5">
            <div className="flex items-end justify-between">
                <div>
                    <h1 className="text-2xl font-extrabold tracking-tight text-ink">Live Feed</h1>
                    <p className="mono mt-1 text-xs text-muted">Every scored transaction · click a row for full
                        assessment</p>
                </div>
                <div className="flex gap-1.5 rounded-lg border border-line bg-surface p-1">
                    {FILTERS.map((f) => (
                        <button key={f} onClick={() => setFilter(f)}
                                className={`mono rounded-md px-3 py-1.5 text-xs font-medium transition ${
                                    filter === f ? 'bg-surface2 text-ink' : 'text-faint hover:text-muted'}`}>
                            {f}
                        </button>
                    ))}
                </div>
            </div>

            <div className="panel overflow-hidden">
                <div
                    className="grid grid-cols-[110px_140px_110px_1fr_140px_90px] gap-4 border-b border-line px-5 py-3 label-kicker">
                    <span>Decision</span><span>Customer</span><span className="text-right">Amount</span>
                    <span>Merchant · Location</span><span>Device</span><span className="text-right">Time</span>
                </div>
                <div className="max-h-[calc(100vh-260px)] overflow-y-auto">
                    {rows.length === 0 &&
                        <div className="py-16 text-center text-sm text-faint">No transactions match this filter
                            yet.</div>}
                    <AnimatePresence initial={false}>
                        {rows.map((t) => (
                            <motion.button
                                key={t.transactionId}
                                layout
                                initial={{opacity: 0, backgroundColor: tintFor(t.decision)}}
                                animate={{opacity: 1, backgroundColor: 'rgba(0,0,0,0)'}}
                                transition={{duration: 0.8}}
                                onClick={() => setSelected(t)}
                                className="grid w-full grid-cols-[110px_140px_110px_1fr_140px_90px] items-center gap-4 border-b border-line/50 px-5 py-3 text-left text-sm hover:bg-surface2/60"
                            >
                                <span><RiskBadge decision={t.decision}/></span>
                                <span className="mono truncate text-muted">{t.customerId}</span>
                                <span className="mono text-right font-semibold text-ink">{fmtCurrency2(t.amount)}</span>
                                <span
                                    className="mono truncate text-faint">{t.merchantCategory} · {cityFromCoords(t.latitude, t.longitude)}</span>
                                <span className="mono truncate text-faint">{t.deviceId}</span>
                                <span className="mono text-right text-[11px] text-faint">{fmtTime(t.timestamp)}</span>
                            </motion.button>
                        ))}
                    </AnimatePresence>
                </div>
            </div>

            <TxnDetailDrawer txn={selected} onClose={() => setSelected(null)}/>
        </div>
    );
}

function tintFor(decision) {
    return decision === 'BLOCK' ? 'rgba(255,81,104,0.16)'
        : decision === 'CHALLENGE' ? 'rgba(255,178,77,0.14)'
            : 'rgba(52,224,161,0.10)';
}
