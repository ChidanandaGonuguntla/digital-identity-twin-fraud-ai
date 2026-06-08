import {
    selectDecisionStream,
    selectKpis,
    selectScoreHistogram,
    selectSignalLeaderboard,
    useStore
} from '../store/useStore';
import {Activity, Banknote, CheckCircle2, Gauge, ShieldQuestion, ShieldX} from 'lucide-react';
import KpiCard from '../components/dashboard/KpiCard';
import Panel from '../components/common/Panel';
import DecisionStream from '../components/dashboard/DecisionStream';
import ScoreHistogram from '../components/dashboard/ScoreHistogram';
import SignalLeaderboard from '../components/dashboard/SignalLeaderboard';
import RiskBadge from '../components/common/RiskBadge';
import {cityFromCoords, fmtCurrency, fmtNumber, fmtPct, timeAgo} from '../lib/format';

export default function Operations() {
    const txns = useStore((s) => s.transactions);
    const k = selectKpis(txns);

    return (
        <div className="space-y-5">
            <PageHead/>

            <div className="grid grid-cols-2 gap-4 lg:grid-cols-3 xl:grid-cols-6">
                <KpiCard label="Throughput" value={fmtNumber(k.total)} sub="txns in window" icon={Activity}
                         accent="#5aa2ff" delay={0}/>
                <KpiCard label="Approved" value={fmtPct(k.approvalRate)} sub={`${fmtNumber(k.allowed)} allowed`}
                         icon={CheckCircle2} accent="#34e0a1" delay={0.04}/>
                <KpiCard label="Challenged" value={fmtNumber(k.challenged)} sub="step-up auth" icon={ShieldQuestion}
                         accent="#ffb24d" delay={0.08}/>
                <KpiCard label="Blocked" value={fmtNumber(k.blocked)} sub={fmtPct(k.blockRate) + ' of volume'}
                         icon={ShieldX} accent="#ff5168" delay={0.12}/>
                <KpiCard label="Avg risk" value={k.avgScore.toFixed(1)} sub="composite score" icon={Gauge}
                         accent="#9b8cff" delay={0.16}/>
                <KpiCard label="Prevented" value={fmtCurrency(k.prevented)} sub="blocked exposure" icon={Banknote}
                         accent="#34e0a1" delay={0.2}/>
            </div>

            <div className="grid grid-cols-1 gap-5 xl:grid-cols-3">
                <Panel className="xl:col-span-2" kicker="Last 2 minutes" title="Decision Stream"
                       right={<Legend/>}>
                    <DecisionStream data={selectDecisionStream(txns)}/>
                </Panel>
                <Panel kicker="Top drivers" title="Fraud Signals">
                    <SignalLeaderboard data={selectSignalLeaderboard(txns)}/>
                </Panel>
            </div>

            <div className="grid grid-cols-1 gap-5 xl:grid-cols-3">
                <Panel kicker="Distribution" title="Risk Score Histogram">
                    <ScoreHistogram data={selectScoreHistogram(txns)}/>
                </Panel>
                <Panel className="xl:col-span-2" kicker="Real time" title="Recent Decisions"
                       right={<span className="mono text-[11px] text-faint">{fmtNumber(txns.length)} buffered</span>}
                       bodyClass="p-0">
                    <MiniFeed txns={txns.slice(0, 7)}/>
                </Panel>
            </div>
        </div>
    );
}

function PageHead() {
    return (
        <div className="flex items-end justify-between">
            <div>
                <h1 className="text-2xl font-extrabold tracking-tight text-ink">Operations</h1>
                <p className="mono mt-1 text-xs text-muted">Real-time fraud decisioning across the transaction
                    stream</p>
            </div>
        </div>
    );
}

function Legend() {
    return (
        <div className="flex items-center gap-3">
            {[['Allow', '#34e0a1'], ['Challenge', '#ffb24d'], ['Block', '#ff5168']].map(([l, c]) => (
                <span key={l} className="flex items-center gap-1.5 text-[11px] text-muted">
          <span className="h-2 w-2 rounded-full" style={{background: c}}/>{l}
        </span>
            ))}
        </div>
    );
}

function MiniFeed({txns}) {
    if (!txns.length) return <div className="py-12 text-center text-sm text-faint">Waiting for transactions…</div>;
    return (
        <div className="divide-y divide-line/60">
            {txns.map((t) => (
                <div key={t.transactionId} className="flex items-center gap-4 px-5 py-2.5 text-sm">
                    <RiskBadge decision={t.decision}/>
                    <span className="mono w-28 shrink-0 text-muted">{t.customerId}</span>
                    <span className="mono w-20 shrink-0 text-right font-semibold text-ink">${t.amount.toFixed(2)}</span>
                    <span
                        className="mono hidden flex-1 truncate text-faint md:block">{t.merchantCategory} · {cityFromCoords(t.latitude, t.longitude)}</span>
                    <span className="mono ml-auto shrink-0 text-[11px] text-faint">{timeAgo(t.timestamp)}</span>
                </div>
            ))}
        </div>
    );
}
