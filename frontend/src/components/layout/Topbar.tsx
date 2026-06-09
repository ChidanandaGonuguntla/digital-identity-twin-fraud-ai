import { Bell, LogOut, Search } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '@/store/authStore';
import { useDecisionStore } from '@/store/decisionStore';

export function Topbar() {
  const user = useAuthStore((s) => s.user);
  const logout = useAuthStore((s) => s.logout);
  const status = useDecisionStore((s) => s.connectionStatus);
  const navigate = useNavigate();

  return (
    <header className="topbar">
      <div className="search-box">
        <Search size={18} />
        <input placeholder="Search customer, transaction, device, model version..." />
      </div>
      <div className="topbar-actions">
        <div className={`connection-pill ${status}`}>WS {status}</div>
        <button className="icon-button" aria-label="Notifications"><Bell size={18} /></button>
        <div className="user-chip">
          <span>{user?.name ?? 'Analyst'}</span>
          <small>{user?.role}</small>
        </div>
        <button className="icon-button" onClick={() => { logout(); navigate('/login'); }} aria-label="Logout">
          <LogOut size={18} />
        </button>
      </div>
    </header>
  );
}
