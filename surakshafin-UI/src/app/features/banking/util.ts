/** Extracts the API's own error message when there is one. */
export function errorMessage(err: any, fallback = 'Something went wrong. Please try again.'): string {
  return err?.error?.message ?? fallback;
}

/** A fresh idempotency key for one transfer attempt. */
export function newIdempotencyKey(): string {
  const c: Crypto | undefined = (globalThis as any).crypto;
  if (c && typeof c.randomUUID === 'function') {
    return c.randomUUID();
  }
  return `${Date.now().toString(36)}-${Math.random().toString(36).slice(2)}-${Math.random().toString(36).slice(2)}`;
}

export function lastFour(accountNumber: string): string {
  return '••' + accountNumber.slice(-4);
}
