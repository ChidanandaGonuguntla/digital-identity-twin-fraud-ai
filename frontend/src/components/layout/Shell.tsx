import { Outlet } from 'react-router-dom';
import { useDecisionWebSocket } from '@/hooks/useDecisionWebSocket';
import { Sidebar } from './Sidebar';
import { Topbar } from './Topbar';

export function Shell() {
  useDecisionWebSocket();
  return (
    <div className="app-shell">
      <Sidebar />
      <main className="main-shell">
        <Topbar />
        <section className="page-surface">
          <Outlet />
        </section>
      </main>
    </div>
  );
}
