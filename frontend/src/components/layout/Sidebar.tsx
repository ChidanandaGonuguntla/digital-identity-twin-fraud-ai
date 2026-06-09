import { NavLink } from 'react-router-dom';
import { Activity, BrainCircuit, ClipboardList, Database, FileSearch, Gauge, Home, Radar, Scale, Server, Settings, ShieldCheck, UserCheck } from 'lucide-react';
import { canAccessRoute } from '@/config/pagePermissions';
import { useAuthStore } from '@/store/authStore';

const nav = [
  { to: '/welcome', label: 'Welcome', icon: Home },
  { to: '/command-center', label: 'Command Center', icon: Gauge },
  { to: '/live-decisions', label: 'Live Decisions', icon: Activity },
  { to: '/twins', label: 'Twin Explorer', icon: Radar },
  { to: '/models', label: 'Model Monitoring', icon: BrainCircuit },
  { to: '/model-governance', label: 'Model Governance', icon: Scale },
  { to: '/feature-store', label: 'Feature Store', icon: Database },
  { to: '/ops', label: 'Platform Ops', icon: Server },
  { to: '/audit', label: 'Audit Trail', icon: FileSearch },
  { to: '/cases', label: 'Case Management', icon: ClipboardList },
  { to: '/step-up', label: 'Step-Up', icon: UserCheck },
  { to: '/settings', label: 'Settings', icon: Settings }
];

export function Sidebar() {
  const role = useAuthStore((state) => state.user?.role);
  const visibleNav = nav.filter((item) => canAccessRoute(role, item.to));

  return (
    <aside className="sidebar">
      <div className="brand-block">
        <div className="brand-mark"><ShieldCheck size={24} /></div>
        <div>
          <div className="brand-title">Digital Twin</div>
          <div className="brand-subtitle">Fraud Intelligence</div>
        </div>
      </div>
      <nav className="side-nav">
        {visibleNav.map((item) => {
          const Icon = item.icon;
          return (
            <NavLink key={item.to} to={item.to} className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
              <Icon size={18} />
              <span>{item.label}</span>
            </NavLink>
          );
        })}
      </nav>
      <div className="sidebar-card">
        <div className="eyebrow">Policy</div>
        <strong>fraud-policy-v1.0.0</strong>
        <p>Rule + twin deviation + ML decision orchestration.</p>
      </div>
    </aside>
  );
}
