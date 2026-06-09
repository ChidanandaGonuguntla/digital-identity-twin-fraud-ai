import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Loader2 } from 'lucide-react';
import { completeOidcLogin } from '@/lib/oidcClient';
import { useAuthStore } from '@/store/authStore';

export function OidcCallbackPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const loginWithAccessToken = useAuthStore((state) => state.loginWithAccessToken);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const code = searchParams.get('code');
    const state = searchParams.get('state');
    const oauthError = searchParams.get('error_description') ?? searchParams.get('error');
    if (oauthError) {
      setError(oauthError);
      return;
    }
    if (!code || !state) {
      setError('Missing authorization code');
      return;
    }
    completeOidcLogin(code, state)
      .then((token) => loginWithAccessToken(token))
      .then(() => navigate('/welcome', { replace: true }))
      .catch((err: Error) => setError(err.message));
  }, [loginWithAccessToken, navigate, searchParams]);

  return (
    <div className="login-page">
      <div className="login-card">
        {error ? <div className="error-banner">{error}</div> : (
          <div className="empty-state"><Loader2 className="spin" size={20} /> Completing SSO sign-in…</div>
        )}
      </div>
    </div>
  );
}
