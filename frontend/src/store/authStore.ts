import { create } from 'zustand';
import { env } from '@/config/env';
import type { UserRole } from '@/config/pagePermissions';
import { decodeJwtPayload } from '@/lib/oidcClient';

type User = {
  name: string;
  email: string;
  role: UserRole;
};

type AuthState = {
  user: User | null;
  accessToken: string | null;
  login: (email: string, password: string) => Promise<void>;
  loginWithAccessToken: (accessToken: string) => Promise<void>;
  logout: () => void;
};

const AUTH_KEY = 'dti.user';
const TOKEN_KEY = 'dti.token';

const VALID_ROLES: UserRole[] = [
  'FRAUD_ANALYST',
  'FRAUD_MANAGER',
  'MODEL_RISK_ADMIN',
  'AUDITOR',
  'ADMIN'
];

function loadStoredUser(): User | null {
  const stored = localStorage.getItem(AUTH_KEY);
  if (!stored) return null;
  try {
    const parsed = JSON.parse(stored) as Partial<User>;
    if (
      parsed &&
      typeof parsed.email === 'string' &&
      typeof parsed.name === 'string' &&
      parsed.role &&
      VALID_ROLES.includes(parsed.role as UserRole)
    ) {
      return parsed as User;
    }
  } catch {
    /* legacy plain-text value */
  }
  localStorage.removeItem(AUTH_KEY);
  return null;
}

function loadStoredToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

function normalizeRole(email: string, backendRole?: string): UserRole {
  if (backendRole && VALID_ROLES.includes(backendRole as UserRole)) {
    return backendRole as UserRole;
  }
  if (!env.securityEnabled) {
    return email.includes('admin') ? 'ADMIN' : 'FRAUD_ANALYST';
  }
  return 'FRAUD_ANALYST';
}

function roleFromOidcClaims(claims: Record<string, unknown>): UserRole {
  const candidates: string[] = [];
  const pushValues = (value: unknown) => {
    if (typeof value === 'string') candidates.push(value);
    if (Array.isArray(value)) value.forEach((item) => typeof item === 'string' && candidates.push(item));
  };
  pushValues(claims.roles);
  pushValues(claims.groups);
  if (claims.realm_access && typeof claims.realm_access === 'object') {
    pushValues((claims.realm_access as Record<string, unknown>).roles);
  }
  for (const raw of candidates) {
    const normalized = raw.toUpperCase().replace(/-/g, '_').replace(/^ROLE_/, '');
    if (VALID_ROLES.includes(normalized as UserRole)) {
      return normalized as UserRole;
    }
  }
  return 'FRAUD_ANALYST';
}

export const useAuthStore = create<AuthState>((set) => ({
  user: loadStoredUser(),
  accessToken: loadStoredToken(),
  async login(email: string, password: string) {
    let accessToken: string | null = null;
    let role: UserRole = normalizeRole(email);

    if (env.securityEnabled) {
      const response = await fetch(`${env.apiBaseUrl}/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password })
      });
      if (!response.ok) {
        const body = await response.text().catch(() => '');
        throw new Error(body || 'Login failed');
      }
      const payload = (await response.json()) as {
        accessToken: string;
        role?: string;
        email?: string;
      };
      accessToken = payload.accessToken;
      role = normalizeRole(email, payload.role);
      localStorage.setItem(TOKEN_KEY, accessToken);
    }

    const user: User = {
      name: email.split('@')[0]?.replace(/[._-]/g, ' ') || 'Fraud Analyst',
      email,
      role
    };
    localStorage.setItem(AUTH_KEY, JSON.stringify(user));
    set({ user, accessToken });
  },
  async loginWithAccessToken(accessToken: string) {
    const claims = decodeJwtPayload(accessToken);
    const email =
      (typeof claims.email === 'string' && claims.email) ||
      (typeof claims.preferred_username === 'string' && claims.preferred_username) ||
      'oidc-user@citizens.com';
    const role = roleFromOidcClaims(claims);
    localStorage.setItem(TOKEN_KEY, accessToken);
    const user: User = {
      name: email.split('@')[0]?.replace(/[._-]/g, ' ') || 'Enterprise User',
      email,
      role
    };
    localStorage.setItem(AUTH_KEY, JSON.stringify(user));
    set({ user, accessToken });
  },
  logout() {
    localStorage.removeItem(AUTH_KEY);
    localStorage.removeItem(TOKEN_KEY);
    set({ user: null, accessToken: null });
  }
}));

export function getAccessToken(): string | null {
  return useAuthStore.getState().accessToken ?? loadStoredToken();
}
