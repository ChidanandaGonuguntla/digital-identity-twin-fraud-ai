import {AnimatePresence, motion} from 'framer-motion';
import {Clock, MapPin, Smartphone, Tag, User, X} from 'lucide-react';
import ScoreGauge from '../common/ScoreGauge';
import RiskBadge from '../common/RiskBadge';
import {SIGNALS} from '../../lib/constants';
import {cityFromCoords, fmtCurrency2, fmtDateTime} from '../../lib/format';

export default function TxnDetailDrawer({txn, onClose}) {
    return (
        <AnimatePresence>
            {txn && (
                <>
                    <motion.div className="fixed inset-0 z-40 bg-black/50 backdrop-blur-[2px]"
                                initial={{opacity: 0}} animate={{opacity: 1}} exit={{opacity: 0}} onClick={onClose}/>
                    <motion.aside
                        className="fixed right-0 top-0 z-50 flex h-full w-[460px] max-w-[94vw] flex-col border-l border-line bg-surface shadow-2xl"
                        initial={{x: 480}} animate={{x: 0}} exit={{x: 480}}
                        transition={{type: 'spring', stiffness: 320, damping: 34}}
                    >
                        <header className="flex items-center justify-between border-b border-line px-5 py-4">
                            <div>
                                <div className="label-kicker mb-1">Transaction</div>
                                <div className="mono text-sm text-ink">{txn.transactionId}</div>
                            </div>
                            <button onClick={onClose} className="btn-ghost p-2"><X size={16}/></button>
                        </header>

                        <div className="flex-1 overflow-y-auto">
                            <div className="flex items-center gap-5 border-b border-line/60 px-5 py-5">
                                <ScoreGauge score={txn.riskScore}/>
                                <div className="space-y-2">
                                    <RiskBadge decision={txn.decision} size="lg"/>
                                    <div
                                        className="stat-num text-2xl font-bold text-ink">{fmtCurrency2(txn.amount)}</div>
                                    {txn.coldStart &&
                                        <span className="mono text-[11px] text-azure">cold-start · twin learning</span>}
                                </div>
                            </div>

                            <div className="grid grid-cols-2 gap-px border-b border-line/60 bg-line/40">
                                <Meta icon={User} label="Customer" value={txn.customerId}/>
                                <Meta icon={Tag} label="Category" value={txn.merchantCategory}/>
                                <Meta icon={MapPin} label="Location"
                                      value={cityFromCoords(txn.latitude, txn.longitude)}/>
                                <Meta icon={Smartphone} label="Device" value={txn.deviceId}/>
                                <Meta icon={Clock} label="Time" value={fmtDateTime(txn.timestamp)}
                                      className="col-span-2"/>
                            </div>

                            <div className="px-5 py-5">
                                <div className="label-kicker mb-3">Signal contributions</div>
                                {txn.signals?.length ? (
                                    <div className="space-y-3">
                                        {txn.signals.map((s) => (
                                            <SignalBar key={s.name} name={SIGNALS[s.name] || s.name}
                                                       value={s.contribution}/>
                                        ))}
                                    </div>
                                ) : (
                                    <p className="text-sm text-faint">No deviation signals — consistent with the
                                        customer twin.</p>
                                )}
                            </div>

                            <div className="border-t border-line/60 px-5 py-5">
                                <div className="label-kicker mb-3">Reason codes</div>
                                <ul className="space-y-2">
                                    {txn.reasons?.map((r, i) => (
                                        <li key={i} className="flex gap-2.5 text-sm text-muted">
                                            <span className="mt-1.5 h-1 w-1 shrink-0 rounded-full bg-signal"/>{r}
                                        </li>
                                    ))}
                                </ul>
                            </div>
                        </div>
                    </motion.aside>
                </>
            )}
        </AnimatePresence>
    );
}

function Meta({icon: Icon, label, value, className = ''}) {
    return (
        <div className={`bg-surface px-5 py-3.5 ${className}`}>
            <div className="mb-1 flex items-center gap-1.5 text-faint"><Icon size={12}/><span
                className="label-kicker">{label}</span></div>
            <div className="mono truncate text-sm text-ink">{value}</div>
        </div>
    );
}

function SignalBar({name, value}) {
    const pct = Math.min(100, (value / 40) * 100);
    return (
        <div>
            <div className="mb-1 flex justify-between text-sm"><span className="text-ink">{name}</span>
                <span className="mono text-faint">+{value.toFixed(1)}</span></div>
            <div className="h-1.5 overflow-hidden rounded-full bg-base">
                <div className="h-full rounded-full bg-block" style={{width: `${pct}%`}}/>
            </div>
        </div>
    );
}
