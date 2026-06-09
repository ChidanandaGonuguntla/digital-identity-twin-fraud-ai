export type UserRole =
  | 'FRAUD_ANALYST'
  | 'FRAUD_MANAGER'
  | 'MODEL_RISK_ADMIN'
  | 'AUDITOR'
  | 'ADMIN';

export const pagePermissions: Record<string, UserRole[]> = {
  '/welcome': ['FRAUD_ANALYST', 'FRAUD_MANAGER', 'MODEL_RISK_ADMIN', 'AUDITOR', 'ADMIN'],
  '/command-center': ['FRAUD_ANALYST', 'FRAUD_MANAGER', 'ADMIN'],
  '/live-decisions': ['FRAUD_ANALYST', 'FRAUD_MANAGER', 'ADMIN'],
  '/twins': ['FRAUD_ANALYST', 'FRAUD_MANAGER', 'ADMIN'],
  '/models': ['MODEL_RISK_ADMIN', 'ADMIN'],
  '/model-governance': ['MODEL_RISK_ADMIN', 'ADMIN'],
  '/feature-store': ['MODEL_RISK_ADMIN', 'ADMIN'],
  '/ops': ['MODEL_RISK_ADMIN', 'ADMIN'],
  '/audit': ['AUDITOR', 'ADMIN', 'FRAUD_ANALYST', 'FRAUD_MANAGER'],
  '/cases': ['FRAUD_ANALYST', 'FRAUD_MANAGER', 'ADMIN'],
  '/step-up': ['FRAUD_ANALYST', 'FRAUD_MANAGER', 'ADMIN'],
  '/settings': ['ADMIN']
};

export function canAccessRoute(role: UserRole | undefined, path: string): boolean {
  if (!role) return false;
  const allowed = pagePermissions[path];
  if (!allowed) return true;
  return allowed.includes(role);
}

export function canAssignCases(role: UserRole | undefined): boolean {
  return role === 'FRAUD_ANALYST' || role === 'FRAUD_MANAGER' || role === 'ADMIN';
}

export function canEscalateCases(role: UserRole | undefined): boolean {
  return role === 'FRAUD_MANAGER' || role === 'ADMIN';
}

export function canBackfillCases(role: UserRole | undefined): boolean {
  return role === 'FRAUD_MANAGER' || role === 'ADMIN';
}
