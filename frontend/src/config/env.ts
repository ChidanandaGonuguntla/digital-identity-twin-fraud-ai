const backendOrigin = import.meta.env.VITE_BACKEND_ORIGIN ?? 'http://localhost:9997';

export const env = {
  backendOrigin,
  apiBaseUrl: import.meta.env.VITE_API_BASE_URL ?? `${backendOrigin}/api/v1`,
  wsUrl: import.meta.env.VITE_WS_URL ?? 'ws://localhost:9997/ws/decisions',
  grafanaUrl: import.meta.env.VITE_GRAFANA_URL ?? 'http://localhost:3001',
  useMock: String(import.meta.env.VITE_USE_MOCK ?? 'false') === 'true',
  securityEnabled: String(import.meta.env.VITE_SECURITY_ENABLED ?? 'false') === 'true',
  oidcEnabled: String(import.meta.env.VITE_OIDC_ENABLED ?? 'false') === 'true',
  oidcIssuer: import.meta.env.VITE_OIDC_ISSUER ?? '',
  oidcClientId: import.meta.env.VITE_OIDC_CLIENT_ID ?? '',
  oidcRedirectUri: import.meta.env.VITE_OIDC_REDIRECT_URI ?? `${window.location.origin}/login/callback`,
  oidcScope: import.meta.env.VITE_OIDC_SCOPE ?? 'openid profile email',
  appName: import.meta.env.VITE_APP_NAME ?? 'Digital Identity Twin'
};
