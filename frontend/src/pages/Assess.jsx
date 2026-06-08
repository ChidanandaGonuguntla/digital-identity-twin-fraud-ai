import {useState} from 'react';
import {motion} from 'framer-motion';
import {AlertCircle, Loader2, ScanSearch} from 'lucide-react';
import {assessTransaction} from '../services/api';
import {MERCHANT_CATEGORIES, SIGNALS} from '../lib/constants';
import ScoreGauge from '../components/common/ScoreGauge';
import RiskBadge from '../components/common/RiskBadge';
import Panel from '../components/common/Panel';

const PRESETS = {
    normal: {
        customerId: 'CUST-01007',
        amount: 64.5,
        merchantCategory: 'grocery',
        deviceId: 'dev-1007-home',
        latitude: 35.2271,
        longitude: -80.8431
    },
    fraud: {
        customerId: 'CUST-01007',
        amount: 4200,
        merchantCategory: 'jewelry',
        deviceId: 'dev-unknown-x',
        latitude: 1.3521,
        longitude: 103.8198
    },
};

export default function Assess() {
    const [form, setForm] = useState(PRESETS.normal);
    const [result, setResult] = useState(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);

    const set = (k) => (e) => setForm((f) => ({...f, [k]: e.target.value}));

    async function submit() {
        setLoading(true);
        setError(null);
        setResult(null);
        try {
            const payload = {
                ...form,
                amount: Number(form.amount),
                latitude: Number(form.latitude),
                longitude: Number(form.longitude),
                transactionId: `TXN-MANUAL-${Date.now().toString(36).toUpperCase()}`,
                timestamp: new Date().toISOString(),
            };
            setResult(await assessTransaction(payload));
        } catch (e) {
            setError(e.message);
        } finally {
            setLoading(false);
        }
    }

    return (
        <div className="space-y-5">
            <div>
                <h1 className="text-2xl font-extrabold tracking-tight text-ink">Assess Transaction</h1>
                <p className="mono mt-1 text-xs text-muted">Submit an event to the scoring engine · POST
                    /api/v1/transactions/assess</p>
            </div>

            <div className="grid grid-cols-1 gap-5 lg:grid-cols-2">
                <Panel kicker="Input" title="Transaction" right={
                    <div className="flex gap-1.5">
                        <button onClick={() => setForm(PRESETS.normal)}
                                className="mono rounded-md border border-line px-2.5 py-1 text-[11px] text-muted hover:text-ink">Normal
                        </button>
                        <button onClick={() => setForm(PRESETS.fraud)}
                                className="mono rounded-md border border-block/40 px-2.5 py-1 text-[11px] text-block hover:brightness-110">Fraud
                        </button>
                    </div>
                }>
                    <div className="space-y-3.5">
                        <Field label="Customer ID"><input className="field" value={form.customerId}
                                                          onChange={set('customerId')}/></Field>
                        <div className="grid grid-cols-2 gap-3.5">
                            <Field label="Amount (USD)"><input className="field" type="number" value={form.amount}
                                                               onChange={set('amount')}/></Field>
                            <Field label="Category">
                                <select className="field" value={form.merchantCategory}
                                        onChange={set('merchantCategory')}>
                                    {MERCHANT_CATEGORIES.map((c) => <option key={c} value={c}>{c}</option>)}
                                </select>
                            </Field>
                        </div>
                        <Field label="Device ID"><input className="field" value={form.deviceId}
                                                        onChange={set('deviceId')}/></Field>
                        <div className="grid grid-cols-2 gap-3.5">
                            <Field label="Latitude"><input className="field" type="number" value={form.latitude}
                                                           onChange={set('latitude')}/></Field>
                            <Field label="Longitude"><input className="field" type="number" value={form.longitude}
                                                            onChange={set('longitude')}/></Field>
                        </div>
                        <button onClick={submit} disabled={loading} className="btn-primary w-full">
                            {loading ? <Loader2 size={16} className="animate-spin"/> : <ScanSearch size={16}/>}
                            {loading ? 'Scoring…' : 'Assess Transaction'}
                        </button>
                        {error && (
                            <div
                                className="flex items-start gap-2 rounded-lg border border-block/40 bg-block/10 p-3 text-xs text-block">
                                <AlertCircle size={15} className="mt-0.5 shrink-0"/><span
                                className="mono">{error}</span>
                            </div>
                        )}
                    </div>
                </Panel>

                <Panel kicker="Output" title="Risk Assessment">
                    {!result && !loading && (
                        <div className="grid h-[320px] place-items-center text-center text-sm text-faint">
                            <div><ScanSearch size={34} className="mx-auto mb-3 opacity-30"/>Submit a transaction to see
                                its risk assessment.
                            </div>
                        </div>
                    )}
                    {result && (
                        <motion.div initial={{opacity: 0, y: 10}} animate={{opacity: 1, y: 0}} className="space-y-5">
                            <div className="flex items-center gap-5">
                                <ScoreGauge score={result.riskScore}/>
                                <div className="space-y-2">
                                    <RiskBadge decision={result.decision} size="lg"/>
                                    <div className="mono text-xs text-muted">{result.transactionId}</div>
                                    {result.coldStart &&
                                        <div className="mono text-[11px] text-azure">cold-start · twin still
                                            learning</div>}
                                </div>
                            </div>
                            {result.signals?.length > 0 && (
                                <div>
                                    <div className="label-kicker mb-2.5">Signal contributions</div>
                                    <div className="space-y-2.5">
                                        {result.signals.map((s) => (
                                            <div key={s.name}>
                                                <div className="mb-1 flex justify-between text-sm"><span
                                                    className="text-ink">{SIGNALS[s.name] || s.name}</span><span
                                                    className="mono text-faint">+{s.contribution.toFixed(1)}</span>
                                                </div>
                                                <div className="h-1.5 overflow-hidden rounded-full bg-base">
                                                    <div className="h-full rounded-full bg-challenge"
                                                         style={{width: `${Math.min(100, (s.contribution / 40) * 100)}%`}}/>
                                                </div>
                                            </div>
                                        ))}
                                    </div>
                                </div>
                            )}
                            <div>
                                <div className="label-kicker mb-2.5">Reason codes</div>
                                <ul className="space-y-2">
                                    {result.reasons?.map((r, i) => (
                                        <li key={i} className="flex gap-2.5 text-sm text-muted"><span
                                            className="mt-1.5 h-1 w-1 shrink-0 rounded-full bg-signal"/>{r}</li>
                                    ))}
                                </ul>
                            </div>
                        </motion.div>
                    )}
                </Panel>
            </div>
        </div>
    );
}

function Field({label, children}) {
    return (
        <label className="block">
            <span className="label-kicker mb-1.5 block">{label}</span>
            {children}
        </label>
    );
}
