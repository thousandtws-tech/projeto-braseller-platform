'use server'

import { revalidatePath } from 'next/cache'
import { redirect } from 'next/navigation'
import { cookies } from 'next/headers'
import { getToken, getSession, COOKIE_NAME } from '@/entities/session/server/session'

function resolveGatewayUrl() {
  const raw =
    process.env.GATEWAY_URL ??
    process.env.NEXT_PUBLIC_GATEWAY_URL ??
    'http://localhost:8080'
  return raw.trim().replace(/^["']|["']$/g, '').replace(/\/+$/, '')
}

const GATEWAY_URL = resolveGatewayUrl()

async function handleExpired(): Promise<never> {
  const store = await cookies()
  store.delete(COOKIE_NAME)
  redirect('/login?expired=1')
}

type OfxImportState =
  | { success: true; count: number }
  | { success: false; error: string }
  | null

export async function importOfxAction(
  prevState: OfxImportState,
  formData: FormData
): Promise<OfxImportState> {
  const [token, session] = await Promise.all([getToken(), getSession()])
  if (!token || !session?.tenantId) return handleExpired()

  const file = formData.get('file') as File | null
  if (!file || file.size === 0) return { success: false, error: 'Selecione um arquivo OFX.' }

  const ofxContent = await file.text()

  try {
    const res = await fetch(
      `${GATEWAY_URL}/api/reports/tenants/${session.tenantId}/bank/ofx-import`,
      {
        method: 'POST',
        headers: {
          'Content-Type': 'text/plain',
          Authorization: `Bearer ${token}`,
        },
        body: ofxContent,
        cache: 'no-store',
      }
    )
    if (res.status === 401) return handleExpired()
    if (!res.ok) {
      const body = await res.json().catch(() => ({})) as Record<string, unknown>
      return { success: false, error: String(body.message ?? 'Erro ao importar extrato.') }
    }
    const data = await res.json() as unknown[]
    revalidatePath('/extrato')
    revalidatePath('/dre')
    return { success: true, count: Array.isArray(data) ? data.length : 0 }
  } catch (err) {
    if (err instanceof Error && err.message === 'NEXT_REDIRECT') throw err
    return { success: false, error: 'Não foi possível importar o extrato.' }
  }
}
