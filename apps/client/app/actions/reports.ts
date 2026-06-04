'use server'

import { refresh, revalidatePath } from 'next/cache'
import { getToken, getSession } from '@/lib/auth'
import type { FiscalProfile, CloudinaryUploadSignature } from '@/types'

function resolveGatewayUrl() {
  const raw =
    process.env.GATEWAY_URL ??
    process.env.NEXT_PUBLIC_GATEWAY_URL ??
    'http://localhost:8080'

  return raw.trim().replace(/^["']|["']$/g, '').replace(/\/+$/, '')
}

const GATEWAY_URL = resolveGatewayUrl()

// ─── Expense actions ──────────────────────────────────────────────────────────

type ExpenseState =
  | { success: true; expenseDate: string }
  | { success: false; error: string }
  | null

function expenseErrorMessage(message?: string) {
  if (message === 'invalid_expense_category') {
    return 'Categoria de despesa inválida. Selecione uma categoria da lista.'
  }

  return message ?? 'Erro ao criar despesa.'
}

export async function createExpenseAction(
  prevState: ExpenseState,
  formData: FormData
): Promise<ExpenseState> {
  const [token, session] = await Promise.all([getToken(), getSession()])
  if (!token || !session?.tenantId) return { success: false, error: 'Sessão expirada.' }

  const file = formData.get('file') as File | null
  const description     = (formData.get('description') as string)?.trim()
  const category        = (formData.get('category') as string)?.trim()
  const rawAmount       = String(formData.get('amount') ?? '').trim().replace(',', '.')
  const expense_date    = (formData.get('expense_date') as string) ||
    new Date().toISOString().split('T')[0]
  const validAmount = /^\d+(\.\d{1,2})?$/.test(rawAmount) && Number(rawAmount) > 0

  if (!description || !category || !validAmount) {
    return { success: false, error: 'Preencha todos os campos corretamente.' }
  }

  if (!file || file.size <= 0) {
    return { success: false, error: 'Anexe o comprovante da despesa.' }
  }

  let attachment: Record<string, unknown> | undefined

  // Upload do comprovante obrigatorio para o Cloudinary
  if (file && file.size > 0) {
    try {
      // 1. Obter assinatura do reports-service
      const sigRes = await fetch(
        `${GATEWAY_URL}/api/reports/tenants/${session.tenantId}/expenses/upload-signature`,
        { headers: { Authorization: `Bearer ${token}` }, cache: 'no-store' }
      )
      if (!sigRes.ok) return { success: false, error: 'Não foi possível obter assinatura para upload.' }

      const sig = await sigRes.json() as CloudinaryUploadSignature

      // 2. Upload direto para o Cloudinary
      const cloudForm = new FormData()
      cloudForm.append('file', file)
      cloudForm.append('api_key', sig.api_key)
      cloudForm.append('timestamp', String(sig.timestamp))
      cloudForm.append('signature', sig.signature)
      cloudForm.append('folder', sig.folder)
      if (sig.use_filename)    cloudForm.append('use_filename', 'true')
      if (sig.unique_filename) cloudForm.append('unique_filename', 'true')

      const uploadRes = await fetch(sig.upload_url, { method: 'POST', body: cloudForm })
      if (!uploadRes.ok) return { success: false, error: 'Falha no upload do comprovante.' }

      const uploaded = await uploadRes.json()
      attachment = {
        public_id:         uploaded.public_id,
        secure_url:        uploaded.secure_url,
        resource_type:     uploaded.resource_type ?? sig.resource_type,
        original_filename: uploaded.original_filename ?? file.name,
        content_type:      file.type,
        size_bytes:        file.size,
      }
    } catch {
      return { success: false, error: 'Erro ao fazer upload do comprovante.' }
    }
  }

  // 3. Criar a despesa no reports-service
  try {
    const body: Record<string, unknown> = { expense_date, category, description, amount: rawAmount }
    if (attachment) body.attachment = attachment

    const res = await fetch(
      `${GATEWAY_URL}/api/reports/tenants/${session.tenantId}/expenses`,
      {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
        body: JSON.stringify(body),
        cache: 'no-store',
      }
    )
    if (!res.ok) {
      const err = await res.json().catch(() => ({})) as Record<string, unknown>
      const msg = (err.message ?? err.details) as string | undefined
      return { success: false, error: expenseErrorMessage(msg) }
    }
    const created = await res.json().catch(() => null) as { expense_date?: string } | null
    const createdExpenseDate = created?.expense_date ?? expense_date

    revalidatePath('/despesas')
    revalidatePath('/dre')
    refresh()
    return { success: true, expenseDate: createdExpenseDate }
  } catch {
    return { success: false, error: 'Serviço indisponível.' }
  }
}

export async function deleteExpenseAction(expenseId: string): Promise<void> {
  const [token, session] = await Promise.all([getToken(), getSession()])
  if (!token || !session?.tenantId) return

  await fetch(
    `${GATEWAY_URL}/api/reports/tenants/${session.tenantId}/expenses/${expenseId}`,
    { method: 'DELETE', headers: { Authorization: `Bearer ${token}` }, cache: 'no-store' }
  ).catch(() => null)

  revalidatePath('/despesas')
  revalidatePath('/dre')
  refresh()
}

// ─── Fiscal profile action ────────────────────────────────────────────────────

type FiscalProfileState =
  | { success: true; data: FiscalProfile }
  | { success: false; error: string }
  | null

export async function saveFiscalProfileAction(
  prevState: FiscalProfileState,
  formData: FormData
): Promise<FiscalProfileState> {
  const [token, session] = await Promise.all([getToken(), getSession()])
  if (!token || !session) return { success: false, error: 'Sessão expirada.' }

  const body = {
    tax_regime:         formData.get('tax_regime') as string,
    estimated_tax_rate: parseFloat(formData.get('estimated_tax_rate') as string) || 0,
    notes:              (formData.get('notes') as string) || undefined,
  }

  try {
    const res = await fetch(
      `${GATEWAY_URL}/api/reports/tenants/${session.tenantId}/fiscal-profile`,
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
      return { success: false, error: (err.message as string) ?? 'Erro ao salvar perfil fiscal.' }
    }
    const data = await res.json() as FiscalProfile
    revalidatePath('/configuracoes')
    revalidatePath('/dre')
    return { success: true, data }
  } catch {
    return { success: false, error: 'Serviço indisponível.' }
  }
}
