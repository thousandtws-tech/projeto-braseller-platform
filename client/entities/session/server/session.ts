import { cookies } from 'next/headers'
import type { UserSession } from '@/shared/types'

export const COOKIE_NAME = 'brasaller_token'

export async function getToken(): Promise<string | null> {
  const store = await cookies()
  return store.get(COOKIE_NAME)?.value ?? null
}

export async function getSession(): Promise<UserSession | null> {
  const token = await getToken()
  if (!token) return null

  try {
    const parts = token.split('.')
    if (parts.length !== 3) throw new Error('invalid jwt format')

    const payload = JSON.parse(Buffer.from(parts[1], 'base64url').toString())
    const exp = typeof payload.exp === 'number' ? payload.exp : Number(payload.exp)

    if (Number.isFinite(exp) && exp > 0 && exp * 1000 <= Date.now()) {
      return null
    }

    // Map gateway JWT claims with fallbacks used by older token versions.
    const session: UserSession = {
      tenantId: payload.tenantId ?? payload.tenant_id ?? '',
      userId: payload.userId ?? payload.user_id ?? payload.sub ?? '',
      email: payload.email ?? '',
      fullName: payload.fullName ?? payload.full_name ?? payload.name ?? '',
      roles: Array.isArray(payload.roles)
        ? payload.roles
        : payload.role
          ? [payload.role]
          : [],
    }

    // Reject tokens without the minimum identity claims required by the app.
    if (!session.email && !session.userId) return null

    return session
  } catch {
    return null
  }
}
