const AUTH_KEY = 'dti.user';

function sanitizeLegacyAuthStorage() {
  const stored = localStorage.getItem(AUTH_KEY);
  if (!stored) return;

  if (!stored.trimStart().startsWith('{')) {
    localStorage.removeItem(AUTH_KEY);
    return;
  }

  try {
    const parsed = JSON.parse(stored) as { email?: string; name?: string; role?: string };
    if (!parsed.email || !parsed.name || !parsed.role) {
      localStorage.removeItem(AUTH_KEY);
    }
  } catch {
    localStorage.removeItem(AUTH_KEY);
  }
}

sanitizeLegacyAuthStorage();
