import React, { useEffect, useMemo, useState } from 'react';
import { createRoot } from 'react-dom/client';
import { Activity, Brain, ShieldCheck, Radar, UserRound, Database, Zap, Lock, Network, Gauge, RefreshCcw } from 'lucide-react';
import { api, streamUrl } from './api/client';
import './styles.css';

function Login({ onLogin }) {
  return <div className="login-shell">
    <div className="login-card glass">
      <div className="brand-mark"><ShieldCheck size={34}/></div>
      <p className="eyebrow">Aegis Fraud AI</p>
      <h1>Digital Identity Twin</h1>
      <p className="muted">Enterprise fraud intelligence workspace. Keycloak/OIDC-ready login placeholder.</p>
      <div className="login-grid">
        <label>Username<input defaultValue="admin" /></label>
        <label>Password<input defaultValue="admin" type="password" /></label>
      </div>
      <button className="primary wide" onClick={onLogin}>Sign in to Fraud Command Center</button>
      <div className="keycloak-note"><Lock size={16}/> Replace this handler with Keycloak Authorization Code + PKCE.</div>
    </div>
  </div>
}

const decisionClass = (d) => (d || '').toLowerCase().replace('_', '-');

function Metric({ icon: Icon, label, value, sub }) {
  return <div className="metric glass">
    <div className="metric-icon"><Icon size={22}/></div>
    <div><span>{label}</span><strong>{value}</strong><small>{sub}</small></div>
  </div>
}

function Topology() {
  return <section className="panel topology glass">
    <div className="section-title"><Network size={20}/><div><h2>Enterprise AI Runtime Topology</h2><p>UI → API → Twin Store → Risk Engine → Feedback Learning</p></div></div>
    <div className="flow">
      {['React UI','Spring Boot API','Digital Twin','Risk Scoring','Decisioning','Feedback Loop'].map((n,i)=><div className="node" key={n}><span>{String(i+1).padStart(2,'0')}</span>{n}</div>)}
    </div>
  </section>
}

function TwinCard({ twin, selected, onSelect }) {
  return <button className={`twin-card glass ${selected ? 'selected' : ''}`} onClick={() => onSelect(twin.customerId)}>
    <div className="avatar"><UserRound size={24}/></div>
    <div className="twin-info"><b>{twin.customerId}</b><span>{twin.segment}</span></div>
    <div className={`risk-pill ${String(twin.riskLevel).toLowerCase()}`}>{twin.riskLevel}</div>
    <div className="trust"><span>Trust</span><strong>{twin.trustScore}</strong></div>
  </button>
}

function TwinDetail({ twin }) {
  if (!twin) return <div className="panel glass">Select a customer twin.</div>;
  return <section className="panel glass twin-detail">
    <div className="section-title"><Brain size={20}/><div><h2>Living Digital Identity Twin</h2><p>{twin.customerId} · {twin.segment}</p></div></div>
    <div className="trust-ring"><div><strong>{twin.trustScore}</strong><span>Trust Score</span></div></div>
    <div className="chip-group"><b>Known Devices</b>{twin.knownDevices?.map(x => <span key={x}>{x}</span>)}</div>
    <div className="chip-group"><b>Known Locations</b>{twin.knownLocations?.map(x => <span key={x}>{x}</span>)}</div>
    <div className="chip-group"><b>Trusted Merchants</b>{twin.trustedMerchants?.map(x => <span key={x}>{x}</span>)}</div>
    <div className="baseline"><Gauge/> Avg Transaction Baseline <strong>${Number(twin.averageTransactionAmount).toLocaleString()}</strong></div>
  </section>
}

function EvaluatePanel({ customerId, onEvaluated }) {
  const [form, setForm] = useState({ customerId: customerId || 'CUST1001', eventType: 'PAYMENT', amount: 4500, merchant: 'Unknown Crypto Exchange', payee: 'WALLET-8841', deviceId: 'android-new-919', location: 'Lagos', ipAddress: '102.88.10.44', loginHour: 2, merchantCategory: 'CRYPTO' });
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');
  useEffect(() => setForm(f => ({...f, customerId: customerId || f.customerId})), [customerId]);
  async function submit() {
    try {
      setError('');
      const payload = { ...form, amount: Number(form.amount), loginHour: Number(form.loginHour) };
      const res = await api.evaluate(payload);
      setResult(res);
      onEvaluated?.(res);
    } catch (e) { setError(e.message); }
  }
  return <section className="panel glass evaluator">
    <div className="section-title"><Radar size={20}/><div><h2>Realtime Fraud Evaluation</h2><p>Submit a banking event and compare it against the customer identity twin.</p></div></div>
    <div className="form-grid">
      {Object.entries(form).map(([k,v]) => <label key={k}>{k}<input value={v ?? ''} onChange={e => setForm({...form, [k]: e.target.value})}/></label>)}
    </div>
    <button className="primary" onClick={submit}><Zap size={18}/> Evaluate Fraud Risk</button>
    {error && <div className="error">{error}</div>}
    {result && <div className={`decision-result ${decisionClass(result.decision)}`}>
      <div><span>Decision</span><strong>{result.decision}</strong></div>
      <div><span>Risk Score</span><strong>{result.riskScore}</strong></div>
      <ul>{result.reasons.map(r => <li key={r}>{r}</li>)}</ul>
    </div>}
  </section>
}

function EventTimeline({ events, live }) {
  return <section className="panel glass timeline">
    <div className="section-title"><Activity size={20}/><div><h2>Realtime Decision Stream</h2><p>{live ? 'SSE connected' : 'SSE offline'} · latest fraud decisions</p></div></div>
    <div className="events-list">
      {events.map(e => <div className="event-row" key={e.eventId}>
        <div className={`status-dot ${decisionClass(e.decision)}`}></div>
        <div><b>{e.customerId}</b><span>{e.eventType || 'EVENT'} · {e.merchant || e.payee || 'Activity'}</span></div>
        <strong className={decisionClass(e.decision)}>{e.decision}</strong>
        <small>{e.riskScore}</small>
      </div>)}
    </div>
  </section>
}

function FeedbackPanel({ lastDecision, onFeedback }) {
  const [outcome, setOutcome] = useState('CONFIRMED_FRAUD');
  const eventId = lastDecision?.eventId;
  async function submit() {
    if (!lastDecision) return;
    const updated = await api.feedback({ eventId, customerId: lastDecision.customerId, outcome, comments: 'Analyst disposition submitted from demo UI.' });
    onFeedback?.(updated);
  }
  return <section className="panel glass">
    <div className="section-title"><RefreshCcw size={20}/><div><h2>Analyst Feedback Learning</h2><p>Close the loop so the twin adapts after investigation.</p></div></div>
    <div className="feedback-box">
      <span>Current event</span><strong>{eventId || 'Run evaluation first'}</strong>
      <select value={outcome} onChange={e => setOutcome(e.target.value)}>
        <option>CONFIRMED_FRAUD</option><option>LEGITIMATE</option><option>FALSE_POSITIVE</option><option>NEEDS_INVESTIGATION</option>
      </select>
      <button className="secondary" onClick={submit} disabled={!lastDecision}>Apply Feedback</button>
    </div>
  </section>
}

function DatasetPanel() {
  const [rows, setRows] = useState([]);
  useEffect(() => { api.datasetSample().then(setRows).catch(console.error); }, []);
  return <section className="panel glass dataset">
    <div className="section-title"><Database size={20}/><div><h2>PaySim-Inspired Dataset Reference</h2><p>Synthetic mobile-money style records mapped to identity twin events.</p></div></div>
    <table><thead><tr>{['step','type','amount','nameOrig','nameDest','isFraud'].map(h => <th key={h}>{h}</th>)}</tr></thead><tbody>{rows.map((r,i)=><tr key={i}>{['step','type','amount','nameOrig','nameDest','isFraud'].map(h=><td key={h}>{r[h]}</td>)}</tr>)}</tbody></table>
  </section>
}

function App() {
  const [authed, setAuthed] = useState(localStorage.getItem('aegisAuthed') === 'true');
  const [dashboard, setDashboard] = useState(null);
  const [twins, setTwins] = useState([]);
  const [selected, setSelected] = useState('CUST1001');
  const [events, setEvents] = useState([]);
  const [lastDecision, setLastDecision] = useState(null);
  const [live, setLive] = useState(false);

  async function load() {
    const [d,t,e] = await Promise.all([api.dashboard(), api.twins(), api.recentEvents()]);
    setDashboard(d); setTwins(t); setEvents(e); if (!selected && t[0]) setSelected(t[0].customerId);
  }
  useEffect(() => { if (authed) load().catch(console.error); }, [authed]);
  useEffect(() => {
    if (!authed) return;
    const source = new EventSource(streamUrl);
    source.addEventListener('open', () => setLive(true));
    source.addEventListener('error', () => setLive(false));
    source.addEventListener('fraud-decision', e => {
      const data = JSON.parse(e.data);
      setLastDecision(data);
      setEvents(prev => [{ eventId: data.eventId, customerId: data.customerId, eventType: 'LIVE', merchant: 'Realtime evaluation', decision: data.decision, riskScore: data.riskScore }, ...prev].slice(0, 20));
      api.dashboard().then(setDashboard).catch(console.error);
    });
    return () => source.close();
  }, [authed]);

  const selectedTwin = useMemo(() => twins.find(t => t.customerId === selected), [twins, selected]);
  if (!authed) return <Login onLogin={() => { localStorage.setItem('aegisAuthed','true'); setAuthed(true); }} />;

  return <div className="app">
    <aside className="sidebar glass"><div className="logo"><ShieldCheck/> Aegis Twin AI</div><nav><a>Command Center</a><a>Identity Twins</a><a>Fraud Evaluation</a><a>Decision Stream</a><a>Feedback Learning</a></nav><button className="logout" onClick={() => {localStorage.removeItem('aegisAuthed'); setAuthed(false)}}>Logout</button></aside>
    <main>
      <header className="hero glass"><p className="eyebrow">Enterprise Banking Fraud AI</p><h1>Digital Identity Twin Command Center</h1><p>Realtime identity intelligence, fraud decisioning, explainability, and analyst feedback learning in one cockpit.</p></header>
      <section className="metrics">
        <Metric icon={Brain} label="Identity Twins" value={dashboard?.totalTwins ?? '—'} sub="active behavioral baselines" />
        <Metric icon={Activity} label="Events Evaluated" value={dashboard?.totalEvents ?? '—'} sub="seed + live decisions" />
        <Metric icon={ShieldCheck} label="Blocked" value={dashboard?.blocked ?? '—'} sub="high-confidence fraud" />
        <Metric icon={Radar} label="Fraud Pressure" value={`${dashboard?.fraudPressureIndex ?? '—'}%`} sub="composite risk index" />
      </section>
      <Topology />
      <div className="grid two">
        <section className="panel glass"><div className="section-title"><UserRound size={20}/><div><h2>Customer Twins</h2><p>Select a twin to drive evaluations.</p></div></div>{twins.map(t => <TwinCard key={t.customerId} twin={t} selected={selected===t.customerId} onSelect={setSelected}/>)}</section>
        <TwinDetail twin={selectedTwin}/>
      </div>
      <div className="grid two wide-left"><EvaluatePanel customerId={selected} onEvaluated={setLastDecision}/><FeedbackPanel lastDecision={lastDecision} onFeedback={load}/></div>
      <div className="grid two"><EventTimeline events={events} live={live}/><DatasetPanel/></div>
    </main>
  </div>
}

createRoot(document.getElementById('root')).render(<App/>);
