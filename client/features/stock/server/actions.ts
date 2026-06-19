'use server'

import { revalidatePath } from 'next/cache'
import { redirect } from 'next/navigation'
import { cookies } from 'next/headers'
import { getToken, getSession, COOKIE_NAME } from '@/entities/session/server/session'
import { localePath } from '@/shared/i18n/server-locale'

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
  redirect(await localePath('/login?expired=1'))
}

// ─── Import NF-e XML ──────────────────────────────────────────────────────────

type NfeImportState =
  | { success: true; nfeId: string }
  | { success: false; error: string }
  | null

export async function importNfeXmlAction(
  prevState: NfeImportState,
  formData: FormData
): Promise<NfeImportState> {
  const [token, session] = await Promise.all([getToken(), getSession()])
  if (!token || !session?.tenantId) return handleExpired()

  const file = formData.get('file') as File | null
  if (!file || file.size === 0) return { success: false, error: 'Selecione um arquivo XML.' }

  const xmlContent = await file.text()

  try {
    const res = await fetch(
      `${GATEWAY_URL}/api/reports/tenants/${session.tenantId}/stock/nfe-import`,
      {
        method: 'POST',
        headers: {
          'Content-Type': 'text/xml',
          Authorization: `Bearer ${token}`,
        },
        body: xmlContent,
        cache: 'no-store',
      }
    )
    if (res.status === 401) return handleExpired()
    if (!res.ok) {
      const body = await res.json().catch(() => ({})) as Record<string, unknown>
      return { success: false, error: String(body.message ?? 'Erro ao importar NF-e.') }
    }
    const data = await res.json() as { id: string }
    revalidatePath('/estoque')
    revalidatePath('/dre')
    return { success: true, nfeId: data.id ?? '' }
  } catch (err) {
    if (err instanceof Error && err.message === 'NEXT_REDIRECT') throw err
    return { success: false, error: 'Não foi possível importar o XML.' }
  }
}

// ─── Upsert stock item ────────────────────────────────────────────────────────

type StockItemState =
  | { success: true; sku: string }
  | { success: false; error: string }
  | null

function normalizeDecimalInput(value: FormDataEntryValue | null): string {
  return String(value ?? '').trim().replace(/\s/g, '').replace(',', '.')
}

export async function upsertStockItemAction(
  prevState: StockItemState,
  formData: FormData
): Promise<StockItemState> {
  const [token, session] = await Promise.all([getToken(), getSession()])
  if (!token || !session?.tenantId) return handleExpired()

  const sku = (formData.get('sku') as string)?.trim()
  const description = (formData.get('description') as string)?.trim()
  const unitCost = normalizeDecimalInput(formData.get('unit_cost'))

  if (!sku) return { success: false, error: 'SKU obrigatório.' }
  if (!/^\d+(\.\d{1,2})?$/.test(unitCost)) return { success: false, error: 'Custo unitário inválido.' }

  try {
    const res = await fetch(
      `${GATEWAY_URL}/api/reports/tenants/${session.tenantId}/stock/items`,
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({ sku, description: description || sku, unit_cost: unitCost }),
        cache: 'no-store',
      }
    )
    if (res.status === 401) return handleExpired()
    if (!res.ok) {
      const body = await res.json().catch(() => ({})) as Record<string, unknown>
      return { success: false, error: String(body.message ?? 'Erro ao salvar produto.') }
    }
    revalidatePath('/estoque')
    return { success: true, sku }
  } catch (err) {
    if (err instanceof Error && err.message === 'NEXT_REDIRECT') throw err
    return { success: false, error: 'Não foi possível salvar o produto.' }
  }
}
