import {AnimatePresence, motion} from 'framer-motion';
import {ShieldAlert, Trash2, X} from 'lucide-react';
import {useStore} from '../../store/useStore';
import RiskBadge from '../common/RiskBadge';
import {cityFromCoords, fmtCurrency2, timeAgo} from '../../lib/format';

export default function NotificationCenter({open, onClose}) {
    const notifications = useStore((s) => s.notifications);
    const clear = useStore((s) => s.clearNotifications);

    return (
        <AnimatePresence>
            {open && (
                <>
                    <motion.div
                        className="fixed inset-0 z-40 bg-black/50 backdrop-blur-[2px]"
                        initial={{opacity: 0}} animate={{opacity: 1}} exit={{opacity: 0}}
                        onClick={onClose}
                    />
                    <motion.aside
                        className="fixed right-0 top-0 z-50 flex h-full w-[400px] max-w-[92vw] flex-col border-l border-line bg-surface shadow-2xl"
                        initial={{x: 420}} animate={{x: 0}} exit={{x: 420}}
                        transition={{type: 'spring', stiffness: 320, damping: 34}}
                    >
                        <header className="flex items-center justify-between border-b border-line px-5 py-4">
                            <div className="flex items-center gap-2.5">
                                <ShieldAlert size={18} className="text-block"/>
                                <div>
                                    <div className="font-semibold text-ink">Fraud Alerts</div>
                                    <div className="label-kicker">{notifications.length} flagged</div>
                                </div>
                            </div>
                            <div className="flex items-center gap-1">
                                <button onClick={clear} className="btn-ghost p-2" title="Clear all"><Trash2 size={15}/>
                                </button>
                                <button onClick={onClose} className="btn-ghost p-2"><X size={16}/></button>
                            </div>
                        </header>

                        <div className="flex-1 overflow-y-auto p-3">
                            {notifications.length === 0 && (
                                <div className="mt-20 text-center text-sm text-faint">
                                    <ShieldAlert size={30} className="mx-auto mb-3 opacity-40"/>
                                    No alerts yet. High-risk decisions appear here in real time.
                                </div>
                            )}
                            <div className="space-y-2">
                                <AnimatePresence initial={false}>
                                    {notifications.map((n) => (
                                        <motion.div
                                            key={n._nid}
                                            layout
                                            initial={{opacity: 0, y: -8}} animate={{opacity: 1, y: 0}}
                                            exit={{opacity: 0}}
                                            className="rounded-lg border border-line bg-base/50 p-3.5"
                                            style={n.decision === 'BLOCK' ? {borderColor: 'rgba(255,81,104,0.35)'} : {}}
                                        >
                                            <div className="mb-2 flex items-center justify-between">
                                                <RiskBadge decision={n.decision}/>
                                                <span
                                                    className="mono text-[10px] text-faint">{timeAgo(n.timestamp)}</span>
                                            </div>
                                            <div className="flex items-center justify-between text-sm">
                                                <span className="mono text-ink">{n.customerId}</span>
                                                <span
                                                    className="mono font-semibold text-ink">{fmtCurrency2(n.amount)}</span>
                                            </div>
                                            <div className="mono mt-1 text-[11px] text-muted">
                                                {n.merchantCategory} · {cityFromCoords(n.latitude, n.longitude)} ·
                                                score {n.riskScore.toFixed(0)}
                                            </div>
                                            {n.reasons?.[0] && (
                                                <div
                                                    className="mt-2 border-t border-line/60 pt-2 text-[11px] leading-relaxed text-faint">
                                                    {n.reasons[0]}
                                                </div>
                                            )}
                                        </motion.div>
                                    ))}
                                </AnimatePresence>
                            </div>
                        </div>
                    </motion.aside>
                </>
            )}
        </AnimatePresence>
    );
}
