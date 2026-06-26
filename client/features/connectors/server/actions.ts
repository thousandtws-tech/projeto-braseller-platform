'use server'

import { redirect } from 'next/navigation'
import { cookies } from 'next/headers'
import { getToken, COOKIE_NAME } from '@/entities/session/server/session'
import { isLocale } from '@/shared/i18n/config'
import { localePath } from '@/shared/i18n/server-locale'
import { syncConnector } from '@/shared/api/gateway'
import type { SyncJob } from '@/shared/types'
import {
  buildMercadoLivreOAuthUrl,
  createMercadoLivreOAuthState,
  MERCADO_LIVRE_OAUTH_STATE_COOKIE,
  MERCADO_LIVRE_OAUTH_STATE_TTL_SECONDS,
} from './mercado-livre-oauth'

const GATEWAY_URL = process.env.GATEWAY_URL 


// Limpa cookie e redireciona para login quando o JWT expirou
async function handleExpired(): Promise<never> {
  const store = await cookies()
  store.delete(COOKIE_NAME)
  redirect(await localePath('/login?expired=1'))
}

type AuthenticateState =
  | { success: true; platform: string; status: string; expires_at?: string }
  | { success: false; error: string }
  | null

export type PluggyConnectTokenState =
  | { success: true; accessToken: string; includeSandbox: boolean }
  | { success: false; error: string }

export async function startMercadoLivreOAuthAction(formData: FormData): Promise<never> {
  const langValue = formData.get('lang')
  const lang = typeof langValue === 'string' && isLocale(langValue) ? langValue : null

  if (!lang) {
    redirect('/pt-BR/conectores?auth_error=invalid_locale')
  }

  const token = await getToken()
  if (!token) {
    redirect(`/${lang}/login?expired=1`)
  }

  let authorizationUrl: string
  try {
    const state = createMercadoLivreOAuthState()
    authorizationUrl = buildMercadoLivreOAuthUrl(lang, state)
    const store = await cookies()

    store.set(MERCADO_LIVRE_OAUTH_STATE_COOKIE, state, {
      httpOnly: true,
      secure: process.env.NODE_ENV === 'production',
      sameSite: 'lax',
      maxAge: MERCADO_LIVRE_OAUTH_STATE_TTL_SECONDS,
      path: `/${lang}/conectores/callback/mercado-livre`,
    })
  } catch {
    authorizationUrl = `/${lang}/conectores?auth_error=oauth_not_configured`
  }

  redirect(authorizationUrl)
}

export async function authenticateAction(
  prevState: AuthenticateState,
  formData: FormData
): Promise<AuthenticateState> {
  const connectorName = formData.get('connectorName') as string
  if (!connectorName) return { success: false, error: 'Conector inválido.' }

  const token = await getToken()
  if (!token) return handleExpired()

  const credentials: Record<string, string> = {}
  for (const [key, value] of formData.entries()) {
    if (key !== 'connectorName' && typeof value === 'string' && value.trim()) {
      credentials[key] = value.trim()
    }
  }

  if (Object.keys(credentials).length === 0) {
    return { success: false, error: 'Informe as credenciais para continuar.' }
  }

  try {
    const res = await fetch(
      `${GATEWAY_URL}/api/core/connectors/${connectorName}/authenticate`,
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({ credentials }),
        cache: 'no-store',
      }
    )

    if (res.status === 401) return handleExpired()

    if (!res.ok) {
      const body = await res.json().catch(() => ({}))
      return { success: false, error: body.message ?? 'Credenciais inválidas.' }
    }

    const data = await res.json()
    return {
      success: true,
      platform: data.platform ?? connectorName,
      status: data.status ?? 'active',
      expires_at: data.expires_at,
    }
  } catch (err) {
    // Re-lança NEXT_REDIRECT para não engolir o redirect()
    if (err instanceof Error && err.message === 'NEXT_REDIRECT') throw err
    return { success: false, error: 'Não foi possível autenticar o conector.' }
  }
}

export async function createPluggyConnectTokenAction(): Promise<PluggyConnectTokenState> {
  const token = await getToken()
  if (!token) return handleExpired()

  try {
    const res = await fetch(`${GATEWAY_URL}/api/core/open-finance/pluggy/connect-token`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify({}),
      cache: 'no-store',
    })

    if (res.status === 401) return handleExpired()

    if (!res.ok) {
      const body = await res.json().catch(() => ({})) as Record<string, unknown>
      return {
        success: false,
        error: typeof body.message === 'string'
          ? body.message
          : 'Não foi possível iniciar o Open Finance agora.',
      }
    }

    const data = await res.json() as { accessToken?: string }
    if (!data.accessToken) {
      return { success: false, error: 'Resposta inválida ao iniciar o Open Finance.' }
    }

    return {
      success: true,
      accessToken: data.accessToken,
      includeSandbox: process.env.PLUGGY_CONNECT_INCLUDE_SANDBOX !== 'false',
    }
  } catch (err) {
    if (err instanceof Error && err.message === 'NEXT_REDIRECT') throw err
    return { success: false, error: 'Não foi possível conectar com o Open Finance.' }
  }
}

type SyncState =
  | { success: true; job: SyncJob }
  | { success: false; error: string }
  | null

export async function syncAllAction(
  prevState: SyncState,
  formData: FormData
): Promise<SyncState> {
  const connectorName = formData.get('connectorName') as string
  if (!connectorName) return { success: false, error: 'Conector inválido.' }

  const token = await getToken()
  if (!token) return handleExpired()

  try {
    const job = await syncConnector(token, connectorName)
    return { success: true, job }
  } catch (err) {
    if (err instanceof Error && err.message === 'NEXT_REDIRECT') throw err
    const msg = err instanceof Error ? err.message : 'Não foi possível iniciar a sincronização.'
    return { success: false, error: msg }
  }
}
