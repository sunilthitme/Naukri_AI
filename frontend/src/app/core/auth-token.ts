const TOKEN_KEY = 'token';
const USER_EMAIL_KEY = 'userEmail';
const EXPIRY_SKEW_MS = 30_000;

export function storedToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

export function storedUserEmail(): string | null {
  return isJwtUsable(storedToken()) ? localStorage.getItem(USER_EMAIL_KEY) : null;
}

export function saveStoredAuth(token: string, email: string): void {
  localStorage.setItem(TOKEN_KEY, token);
  localStorage.setItem(USER_EMAIL_KEY, email);
}

export function clearStoredAuth(): void {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_EMAIL_KEY);
}

export function isJwtUsable(token: string | null): boolean {
  if (!token) {
    return false;
  }
  const payload = jwtPayload(token);
  if (!payload) {
    return false;
  }
  const expiresAtSeconds = Number(payload['exp']);
  return Number.isFinite(expiresAtSeconds) && expiresAtSeconds * 1000 > Date.now() + EXPIRY_SKEW_MS;
}

function jwtPayload(token: string): Record<string, unknown> | null {
  try {
    const [, encodedPayload] = token.split('.');
    if (!encodedPayload) {
      return null;
    }
    const padded = encodedPayload.replace(/-/g, '+').replace(/_/g, '/').padEnd(
      Math.ceil(encodedPayload.length / 4) * 4,
      '='
    );
    return JSON.parse(atob(padded)) as Record<string, unknown>;
  } catch {
    return null;
  }
}
