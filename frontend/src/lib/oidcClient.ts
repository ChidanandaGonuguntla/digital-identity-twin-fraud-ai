import { env } from '@/config/env';

type OpenIdConfiguration = {
  authorization_endpoint: string;
  token_endpoint: string;
};

const PKCE_VERIFIER_KEY = 'dti.pkce.verifier';
const OIDC_STATE_KEY = 'dti.oidc.state';

function base64UrlEncode(bytes: Uint8Array) {
  let binary = '';
  bytes.forEach((byte) => {
    binary += String.fromCharCode(byte);
  });
  return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

async function sha256(value: string) {
  const data = new TextEncoder().encode(value);
  const digest = await crypto.subtle.digest('SHA-256', data);
  return new Uint8Array(digest);
}

function randomString(length = 48) {
  const bytes = new Uint8Array(length);
  crypto.getRandomValues(bytes);
  return base64UrlEncode(bytes);
}

export async function startOidcLogin() {
  if (!env.oidcClientId || !env.oidcRedirectUri) {
    throw new Error('OIDC client configuration is incomplete');
  }
  const issuer = env.oidcIssuer.replace(/\/$/, '');
  const discovery = await fetch(`${issuer}/.well-known/openid-configuration`);
  if (!discovery.ok) {
    throw new Error('Unable to load OIDC discovery document');
  }
  const config = (await discovery.json()) as OpenIdConfiguration;
  const verifier = randomString(64);
  const challenge = base64UrlEncode(await sha256(verifier));
  const state = randomString(24);
  sessionStorage.setItem(PKCE_VERIFIER_KEY, verifier);
  sessionStorage.setItem(OIDC_STATE_KEY, state);
  const params = new URLSearchParams({
    client_id: env.oidcClientId,
    redirect_uri: env.oidcRedirectUri,
    response_type: 'code',
    scope: env.oidcScope,
    code_challenge: challenge,
    code_challenge_method: 'S256',
    state
  });
  window.location.href = `${config.authorization_endpoint}?${params.toString()}`;
}

export async function completeOidcLogin(code: string, state: string) {
  const expectedState = sessionStorage.getItem(OIDC_STATE_KEY);
  const verifier = sessionStorage.getItem(PKCE_VERIFIER_KEY);
  sessionStorage.removeItem(OIDC_STATE_KEY);
  sessionStorage.removeItem(PKCE_VERIFIER_KEY);
  if (!expectedState || expectedState !== state) {
    throw new Error('Invalid OIDC state');
  }
  if (!verifier) {
    throw new Error('Missing PKCE verifier');
  }
  const issuer = env.oidcIssuer.replace(/\/$/, '');
  const discovery = await fetch(`${issuer}/.well-known/openid-configuration`);
  if (!discovery.ok) {
    throw new Error('Unable to load OIDC discovery document');
  }
  const config = (await discovery.json()) as OpenIdConfiguration;
  const body = new URLSearchParams({
    grant_type: 'authorization_code',
    client_id: env.oidcClientId,
    code,
    redirect_uri: env.oidcRedirectUri,
    code_verifier: verifier
  });
  const tokenResponse = await fetch(config.token_endpoint, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body
  });
  if (!tokenResponse.ok) {
    const errorBody = await tokenResponse.text().catch(() => '');
    throw new Error(errorBody || 'OIDC token exchange failed');
  }
  const payload = (await tokenResponse.json()) as {
    access_token: string;
    id_token?: string;
  };
  return payload.access_token;
}

export function decodeJwtPayload(token: string): Record<string, unknown> {
  const parts = token.split('.');
  if (parts.length < 2) {
    return {};
  }
  const normalized = parts[1].replace(/-/g, '+').replace(/_/g, '/');
  const padded = normalized.padEnd(normalized.length + ((4 - (normalized.length % 4)) % 4), '=');
  return JSON.parse(atob(padded)) as Record<string, unknown>;
}
