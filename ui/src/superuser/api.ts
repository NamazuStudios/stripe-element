// Always read the session token at call time — the CMS manages its lifecycle.
export function sessionHeaders(extra?: Record<string, string>): Record<string, string> {
  const token = (window as any).__elementsApiClient?.getSessionToken()
  const headers: Record<string, string> = { 'Content-Type': 'application/json', ...extra }
  if (token) headers['session_secret'] = token
  return headers
}

// Respect the user's CMS pagination preference.
export function defaultLimit(): number {
  return (window as any).__elementsSettings?.getResultsPerPage() ?? 20
}
