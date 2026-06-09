import { PageHeader } from '@/components/ui/PageHeader';
import { env } from '@/config/env';

export function SettingsPage() {
  return (
    <div className="page-stack">
      <PageHeader eyebrow="Configuration" title="Runtime Settings" description="Environment variables, WebSocket endpoint, and backend integration settings." />
      <div className="settings-grid">
        <div className="glass-card large-copy">
          <h3>API</h3>
          <p>Base URL: <b>{env.apiBaseUrl}</b></p>
          <p>Backend origin: <b>{env.backendOrigin}</b></p>
          <p>Mock mode: <b>{String(env.useMock)}</b></p>
        </div>
        <div className="glass-card large-copy">
          <h3>WebSocket</h3>
          <p>URL: <b>{import.meta.env.DEV ? `ws://${window.location.host}/ws/decisions (proxied)` : env.wsUrl}</b></p>
          <p>Direct: <b>{env.wsUrl}</b></p>
          <p>Reconnect strategy: <b>Exponential backoff</b></p>
        </div>
      </div>
    </div>
  );
}
