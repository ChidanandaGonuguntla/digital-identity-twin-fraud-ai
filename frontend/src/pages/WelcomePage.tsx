import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { Activity, ArrowRight, BrainCircuit, DatabaseZap, ShieldCheck } from 'lucide-react';
import { PageHeader } from '@/components/ui/PageHeader';

export function WelcomePage() {
  return (
    <div className="page-stack">
      <PageHeader eyebrow="Welcome" title="Digital Identity Twin Fraud Intelligence" description="A production-grade analyst workspace for real-time fraud decisions, customer twin drift, model monitoring, and step-up operations." />
      <motion.div className="hero-panel" initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }}>
        <div>
          <div className="eyebrow">Architecture</div>
          <h2>Controller → Application Service → Rule + Twin + ML → Orchestrator → Audit → Response</h2>
          <p>Monitor the full decision lifecycle from live transaction event to explainable outcome.</p>
          <Link className="primary-button inline" to="/command-center">Open Command Center <ArrowRight size={16} /></Link>
        </div>
        <div className="architecture-flow">
          {['API', 'Twin', 'Rules', 'ML', 'Decision', 'Audit'].map((x) => <span key={x}>{x}</span>)}
        </div>
      </motion.div>
      <div className="feature-grid">
        <div className="feature-card"><ShieldCheck /><h3>Explainable decisions</h3><p>Reason codes, evidence, score contribution, model and policy version.</p></div>
        <div className="feature-card"><Activity /><h3>Live WebSocket stream</h3><p>Real-time fraud decision events with reconnect and simulator fallback.</p></div>
        <div className="feature-card"><BrainCircuit /><h3>Model monitoring</h3><p>Precision, recall, AUC, drift and active model metadata.</p></div>
        <div className="feature-card"><DatabaseZap /><h3>Twin behavior</h3><p>Device, country, category, amount and temporal behavior baseline.</p></div>
      </div>
    </div>
  );
}
