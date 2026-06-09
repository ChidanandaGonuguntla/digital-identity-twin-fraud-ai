import { FormEvent, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { KeyRound, ShieldCheck, Sparkles } from 'lucide-react';
import { env } from '@/config/env';
import { startOidcLogin } from '@/lib/oidcClient';
import { useAuthStore } from '@/store/authStore';

type AuthConfig = {
  securityEnabled: boolean;
  provider: string;
  issuerUri?: string;
};

export function LoginPage() {
  const [email, setEmail] = useState('analyst@citizens.com');
  const [password, setPassword] = useState('password');
  const [authConfig, setAuthConfig] = useState<AuthConfig | null>(null);
  const [error, setError] = useState<string | null>(null);
  const login = useAuthStore((s) => s.login);
  const navigate = useNavigate();

  useEffect(() => {
    fetch(`${env.apiBaseUrl}/auth/config`)
      .then((response) => (response.ok ? response.json() : null))
      .then((payload) => setAuthConfig(payload))
      .catch(() => setAuthConfig(null));
  }, []);

  const oidcActive =
    env.oidcEnabled ||
    authConfig?.provider === 'oidc' ||
    Boolean(authConfig?.issuerUri);

  async function submit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    try {
      await login(email, password);
      navigate('/welcome');
    } catch (err) {
      setError((err as Error).message);
    }
  }

  async function signInWithSso() {
    setError(null);
    try {
      await startOidcLogin();
    } catch (err) {
      setError((err as Error).message);
    }
  }

  return (
    <div className="login-page">
      <div className="login-aurora" />
      <motion.div className="login-card" initial={{ opacity: 0, y: 24 }} animate={{ opacity: 1, y: 0 }}>
        <div className="login-brand">
          <div className="brand-mark large"><ShieldCheck size={32} /></div>
          <div>
            <h1>Digital Identity Twin</h1>
            <p>Enterprise fraud intelligence console</p>
          </div>
        </div>
        {oidcActive && (
          <button className="secondary-button" type="button" onClick={signInWithSso} style={{ width: '100%', marginBottom: 16 }}>
            <KeyRound size={16} /> Sign in with enterprise SSO
          </button>
        )}
        {!oidcActive && (
          <form onSubmit={submit} className="login-form">
            <label>Email</label>
            <input value={email} onChange={(e) => setEmail(e.target.value)} type="email" required />
            <label>Password</label>
            <input value={password} onChange={(e) => setPassword(e.target.value)} type="password" required />
            <button className="primary-button" type="submit">Sign in securely</button>
          </form>
        )}
        {error && <div className="error-banner">{error}</div>}
        <div className="login-footer">
          <Sparkles size={16} />
          {oidcActive
            ? 'OIDC/Okta SSO when identity provider is enabled. Local JWT login remains available when provider=local.'
            : 'JWT auth when security is enabled. Demo login uses analyst@citizens.com / password.'}
        </div>
      </motion.div>
      <section className="login-hero-copy">
        <div className="eyebrow">Fraud ops command center</div>
        <h2>Detect abnormal behavior through living customer twins.</h2>
        <p>Rules, behavioral deviation, and ML scoring combined into one explainable decision pipeline.</p>
      </section>
    </div>
  );
}
