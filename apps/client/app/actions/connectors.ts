'use server'

import { redirect } from 'next/navigation'
import { cookies } from 'next/headers'
import { getToken, COOKIE_NAME } from '@/lib/auth'
import { syncConnector, getSyncJob } from '@/lib/api'
import type { SyncJob } from '@/types'

const GATEWAY_URL = process.env.GATEWAY_URL 


// Limpa cookie e redireciona para login quando o JWT expirou
async function handleExpired(): Promise<never> {
  const store = await cookies()
  store.delete(COOKIE_NAME)
  redirect('/login?expired=1')
}

type AuthenticateState =
  | { success: true; platform: string; status: string; expires_at?: string }
  | { success: false; error: string }
  | null

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
    const jobId = await syncConnector(token, connectorName)

    // Aguarda 1.5s e tenta buscar o status do job
    await new Promise((r) => setTimeout(r, 1500))

    try {
      const job = await getSyncJob(token, jobId)
      return { success: true, job }
    } catch {
      // Job ainda não disponível — retorna estado mínimo para não bloquear o usuário
      return {
        success: true,
        job: {
          job_id: jobId,
          tenant_id: '',
          connector_name: connectorName,
          status: 'QUEUED',
          orders_synced: 0,
          payments_synced: 0,
          fees_synced: 0,
        },
      }
    }
  } catch (err) {
    if (err instanceof Error && err.message === 'NEXT_REDIRECT') throw err
    const msg = err instanceof Error ? err.message : 'Não foi possível iniciar a sincronização.'
    return { success: false, error: msg }
  }
}
