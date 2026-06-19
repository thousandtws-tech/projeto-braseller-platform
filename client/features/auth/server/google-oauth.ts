import { cookies } from 'next/headers'
import { COOKIE_NAME } from '@/entities/session/server/session'
import { resolveGatewayUrl } from '@/shared/config/gateway-url'

export type GoogleCallbackResult =
  | { ok: true }
  | { ok: false; status: number; message: string }

export async function getGoogleAuthorizeUrl(): Promise<string | null> {
  const res = await fetch(`${resolveGatewayUrl()}/api/auth/oauth/google/authorize-url`, {
    headers: { Accept: 'application/json' },
    cache: 'no-store',
  })

  if (!res.ok) {
    return null
  }

  const data = await res.json() as { authorizeUrl?: string; authorize_url?: string }
  return data.authorizeUrl ?? data.authorize_url ?? null
}

export async function finishGoogleCallback(code: string, tenantName?: string): Promise<GoogleCallbackResult> {
  const res = await fetch(`${resolveGatewayUrl()}/api/auth/oauth/google/callback`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
    body: JSON.stringify({ code, tenantName: tenantName || undefined }),
    cache: 'no-store',
  })

  const body = await res.json().catch(() => ({})) as Record<string, unknown>

  if (!res.ok) {
    return {
      ok: false,
      status: res.status,
      message: normalizeGoogleAuthError(String(body.message ?? 'google_auth_failed')),
    }
  }

  const accessToken = stringValue(body.accessToken) ?? stringValue(body.access_token)

  if (!accessToken) {
    return { ok: false, status: 502, message: 'invalid_auth_response' }
  }

  const store = await cookies()
  store.set(COOKIE_NAME, accessToken, {
    httpOnly: true,
    secure: process.env.NODE_ENV === 'production',
    sameSite: 'lax',
    maxAge: 60 * 60 * 24,
    path: '/',
  })

  return { ok: true }
}

function stringValue(value: unknown): string | null {
  return typeof value === 'string' && value.trim() ? value : null
}

function normalizeGoogleAuthError(message: string) {
  if (message === 'tenantName is required for Keycloak signup') {
    return 'google_account_not_registered'
  }
  return message
}
