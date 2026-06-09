import { Link } from 'react-router-dom';
import { ShieldAlert } from 'lucide-react';

export function UnauthorizedPage() {
  return (
    <div className="login-shell">
      <div className="login-card">
        <div className="brand-mark" style={{ margin: '0 auto 1rem' }}>
          <ShieldAlert size={24} />
        </div>
        <h1>Access denied</h1>
        <p>Your role does not have permission to view this page.</p>
        <Link to="/welcome" className="primary-btn" style={{ display: 'inline-block', marginTop: '1rem' }}>
          Back to welcome
        </Link>
      </div>
    </div>
  );
}
