import {useState} from 'react';
import {motion} from 'framer-motion';
import {CalendarClock, Fingerprint, Hash, Layers, Loader2, Search, Smartphone} from 'lucide-react';
import {getTwin} from '../services/api';
import Panel from '../components/common/Panel';
import TwinRadar from '../components/twin/TwinRadar';
import {fmtCurrency2, fmtDateTime, fmtNumber, timeAgo} from '../lib/format';

export default function Twins() {
    const [query, setQuery] = useState('CUST-01007');
    const [twin, setTwin] = useState(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);

    async function lookup() {
        if (!query.trim()) return;
        setLoading(true);
        setError(null);
        try {
            setTwin(await getTwin(query.trim()));
        } catch (e) {
            setError(e.message);
            setTwin(null);
        } finally {
            setLoading(false);
        }
    }

    return (
        <div className="space-y-5">
            <div>
                <h1 className="text-2xl font-extrabold tracking-tight text-ink">Identity Twins</h1>
                <p className="mono mt-1 text-xs text-muted">Inspect a customer's behavioral baseline · GET
                    /api/v1/twins/&#123;id&#125;</p>
            </div>

            <div className="panel panel-pad">
                <div className="flex gap-3">
                    <div className="relative flex-1">
                        <Search size={16}
                                className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-faint"/>
                        <input
                            className="field pl-9" placeholder="Customer ID (e.g. CUST-01007)"
                            value={query} onChange={(e) => setQuery(e.target.value)}
                            onKeyDown={(e) => e.key === 'Enter' && lookup()}
                        />
                    </div>
                    <button onClick={lookup} disabled={loading} className="btn-primary px-6">
                        {loading ? <Loader2 size={16} className="animate-spin"/> : <Fingerprint size={16}/>}
                        Look up
                    </button>
                </div>
                {error && <p className="mono mt-3 text-xs text-block">{error}</p>}
            </div>

            {twin && (
                <motion.div initial={{opacity: 0, y: 12}} animate={{opacity: 1, y: 0}}
                            className="grid grid-cols-1 gap-5 xl:grid-cols-5">
                    <div className="space-y-5 xl:col-span-2">
                        <Panel kicker="Subject" title={twin.customerId} right={<span className="live-dot"/>}>
                            <div className="grid grid-cols-2 gap-4">
                                <Stat icon={Hash} label="Transactions" value={fmtNumber(twin.transactionCount)}/>
                                <Stat icon={Layers} label="Categories" value={twin.knownCategories ?? '—'}/>
                                <Stat icon={Smartphone} label="Known devices" value={twin.knownDevices ?? '—'}/>
                                <Stat icon={CalendarClock} label="Last sync"
                                      value={timeAgo(new Date(twin.lastUpdated).getTime())}/>
                            </div>
                            <div className="mt-4 grid grid-cols-2 gap-4 border-t border-line/60 pt-4">
                                <Stat label="Mean amount" value={fmtCurrency2(twin.meanAmount)}/>
                                <Stat label="Std deviation" value={fmtCurrency2(twin.stdDevAmount)}/>
                            </div>
                            <div className="mono mt-4 border-t border-line/60 pt-3 text-[11px] text-faint">
                                Twin established {fmtDateTime(twin.createdAt)}
                            </div>
                        </Panel>
                    </div>

                    <Panel className="xl:col-span-3" kicker="Behavioral fingerprint" title="Twin Stability Profile">
                        {twin.dimensions ? (
                            <>
                                <TwinRadar dimensions={twin.dimensions}/>
                                <p className="mono mt-2 text-center text-[11px] text-faint">
                                    Higher = more predictable. Deviations from this profile drive risk scoring.
                                </p>
                            </>
                        ) : (
                            <p className="text-sm text-faint">No behavioral dimensions available for this twin.</p>
                        )}
                    </Panel>
                </motion.div>
            )}

            {!twin && !loading && (
                <div className="panel grid h-64 place-items-center text-center text-sm text-faint">
                    <div><Fingerprint size={36} className="mx-auto mb-3 opacity-30"/>Look up a customer to inspect their
                        digital twin.
                    </div>
                </div>
            )}
        </div>
    );
}

function Stat({icon: Icon, label, value}) {
    return (
        <div>
            <div className="mb-1 flex items-center gap-1.5 text-faint">
                {Icon && <Icon size={12}/>}<span className="label-kicker">{label}</span>
            </div>
            <div className="stat-num text-lg font-bold text-ink">{value}</div>
        </div>
    );
}
