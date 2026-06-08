import {useEffect, useRef, useState} from 'react';
import {AnimatePresence, motion} from 'framer-motion';
import {ShieldAlert, X} from 'lucide-react';
import {useStore} from '../../store/useStore';
import {cityFromCoords, fmtCurrency2} from '../../lib/format';

// Watches the notifications buffer and surfaces new BLOCK alerts as auto-dismissing toasts.
export default function ToastHost() {
    const notifications = useStore((s) => s.notifications);
    const [toasts, setToasts] = useState([]);
    const seen = useRef(new Set());

    useEffect(() => {
        const latest = notifications[0];
        if (!latest || latest.decision !== 'BLOCK' || seen.current.has(latest._nid)) return;
        seen.current.add(latest._nid);
        setToasts((t) => [{...latest}, ...t].slice(0, 4));
        const id = setTimeout(() => {
            setToasts((t) => t.filter((x) => x._nid !== latest._nid));
        }, 6000);
        return () => clearTimeout(id);
    }, [notifications]);

    const dismiss = (nid) => setToasts((t) => t.filter((x) => x._nid !== nid));

    return (
        <div className="pointer-events-none fixed bottom-6 right-6 z-50 flex w-[340px] flex-col gap-2.5">
            <AnimatePresence>
                {toasts.map((t) => (
                    <motion.div
                        key={t._nid}
                        initial={{opacity: 0, x: 40, scale: 0.96}}
                        animate={{opacity: 1, x: 0, scale: 1}}
                        exit={{opacity: 0, x: 40, scale: 0.96}}
                        transition={{type: 'spring', stiffness: 380, damping: 30}}
                        className="pointer-events-auto rounded-xl border bg-surface/95 p-4 shadow-glow-block backdrop-blur-md"
                        style={{borderColor: 'rgba(255,81,104,0.4)'}}
                    >
                        <div className="flex items-start gap-3">
                            <div className="mt-0.5 grid h-8 w-8 shrink-0 place-items-center rounded-lg bg-block/15">
                                <ShieldAlert size={16} className="text-block"/>
                            </div>
                            <div className="min-w-0 flex-1">
                                <div className="flex items-center justify-between">
                                    <span className="text-sm font-bold text-block">Transaction Blocked</span>
                                    <button onClick={() => dismiss(t._nid)} className="text-faint hover:text-ink"><X
                                        size={14}/></button>
                                </div>
                                <div className="mono mt-1 truncate text-xs text-ink">
                                    {t.customerId} · {fmtCurrency2(t.amount)}
                                </div>
                                <div className="mono mt-0.5 text-[11px] text-muted">
                                    {cityFromCoords(t.latitude, t.longitude)} · risk {t.riskScore.toFixed(0)}
                                </div>
                                {t.reasons?.[1] && (
                                    <div className="mt-1.5 text-[11px] leading-snug text-faint">{t.reasons[1]}</div>
                                )}
                            </div>
                        </div>
                    </motion.div>
                ))}
            </AnimatePresence>
        </div>
    );
}
