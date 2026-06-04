'use server'

import { revalidatePath } from 'next/cache'
import { getToken, getSession } from '@/lib/auth'
import type { NotificationPreferences } from '@/types'

const GATEWAY_URL =
  process.env.GATEWAY_URL ??
  process.env.NEXT_PUBLIC_GATEWAY_URL ??
  'http://localhost:8080'

async function getAuthContext() {
  const [token, session] = await Promise.all([getToken(), getSession()])
  return { token, tenantId: session?.tenantId }
}

export async function markAsReadAction(notificationId: string): Promise<void> {
  const { token, tenantId } = await getAuthContext()
  if (!token || !tenantId) return

  await fetch(`${GATEWAY_URL}/api/notifications/tenants/${tenantId}/${notificationId}/read`, {
    method: 'PATCH',
    headers: { Authorization: `Bearer ${token}` },
    cache: 'no-store',
  }).catch(() => null)

  revalidatePath('/notificacoes')
}

export async function clearReadAction(): Promise<void> {
  const { token, tenantId } = await getAuthContext()
  if (!token || !tenantId) return

  try {
    await fetch(`${GATEWAY_URL}/api/notifications/tenants/${tenantId}/clear-read`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}` },
      cache: 'no-store',
    })
    revalidatePath('/notificacoes')
  } catch {
    return
  }
}

type PrefsState =
  | { success: true; data: NotificationPreferences }
  | { success: false; error: string }
  | null

export async function updatePreferencesAction(
  prevState: PrefsState,
  formData: FormData
): Promise<PrefsState> {
  const { token, tenantId } = await getAuthContext()
  if (!token || !tenantId) return { success: false, error: 'Sessão expirada.' }

  const body: Record<string, unknown> = {
    emailEnabled:                  formData.get('emailEnabled') === 'true',
    newSaleEnabled:                formData.get('newSaleEnabled') === 'true',
    monthlyClosingEnabled:         formData.get('monthlyClosingEnabled') === 'true',
    mlPaymentReleaseEnabled:       formData.get('mlPaymentReleaseEnabled') === 'true',
    weeklyAccountantReportEnabled: formData.get('weeklyAccountantReportEnabled') === 'true',
    recipientEmail:                formData.get('recipientEmail') as string || undefined,
    accountantEmail:               formData.get('accountantEmail') as string || undefined,
  }

  try {
    const res = await fetch(
      `${GATEWAY_URL}/api/notifications/tenants/${tenantId}/preferences`,
      {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify(body),
        cache: 'no-store',
      }
    )

    if (!res.ok) {
      const err = await res.json().catch(() => ({})) as Record<string, unknown>
      return { success: false, error: (err.message as string) ?? 'Erro ao salvar preferências.' }
    }

    const data = await res.json() as NotificationPreferences
    revalidatePath('/notificacoes')
    return { success: true, data }
  } catch {
    return { success: false, error: 'Serviço indisponível.' }
  }
}
