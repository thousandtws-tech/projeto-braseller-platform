'use server'

import { refresh, revalidatePath } from 'next/cache'
import { getToken, getSession } from '@/entities/session/server/session'
import { isBpoOperator } from '@/entities/session/model/permissions'
import type { FiscalProfile, CloudinaryUploadSignature, BatchAccountingClosingResponse } from '@/shared/types'

function resolveGatewayUrl() {
  const raw =
    process.env.GATEWAY_URL ??
    process.env.NEXT_PUBLIC_GATEWAY_URL ??
    'http://localhost:8080'

  return raw.trim().replace(/^["']|["']$/g, '').replace(/\/+$/, '')
}

type ProfitDistributionState =
  | { success: true; amount: number }
  | { success: false; error: string }
  | null

function profitDistributionErrorMessage(message?: string) {
  if (message === 'insufficient_distributable_profit') {
    return 'O valor informado e maior que o lucro disponivel para esse periodo.'
  }
  if (message === 'accounting_period_closing_not_found') {
    return 'Esse periodo ainda nao foi fechado e assinado pelo contador.'
  }
  if (message === 'amount_must_be_positive') {
    return 'Informe um valor maior que zero.'
  }
  if (message === 'period_month is required') {
    return 'Informe o periodo da retirada.'
  }
  return message ?? 'Erro ao registrar retirada.'
}

export async function createProfitDistributionAction(
  prevState: ProfitDistributionState,
  formData: FormData
): Promise<ProfitDistributionState> {
  const [token, session] = await Promise.all([getToken(), getSession()])
  if (!token || !session?.tenantId) return { success: false, error: 'Sessao expirada.' }

  const periodMonth = String(formData.get('period_month') ?? '').trim()
  const rawAmount = String(formData.get('amount') ?? '').trim().replace(',', '.')
  const distributedAt = String(formData.get('distributed_at') ?? '').trim()
  const recipientName = String(formData.get('recipient_name') ?? '').trim()
  const notes = String(formData.get('notes') ?? '').trim()
  const amount = Number(rawAmount)

  if (!periodMonth || !/^\d{4}-\d{2}$/.test(periodMonth) || !Number.isFinite(amount) || amount <= 0) {
    return { success: false, error: 'Preencha periodo e valor corretamente.' }
  }

  try {
    const res = await fetch(
      `${GATEWAY_URL}/api/reports/tenants/${session.tenantId}/profit/distributions`,
      {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
        body: JSON.stringify({
          period_month: periodMonth,
          amount: rawAmount,
          distributed_at: distributedAt || undefined,
          recipient_name: recipientName || undefined,
          notes: notes || undefined,
        }),
        cache: 'no-store',
      }
    )
    if (!res.ok) {
      const err = await res.json().catch(() => ({})) as Record<string, unknown>
      const msg = (err.message ?? err.details) as string | undefined
      return { success: false, error: profitDistributionErrorMessage(msg) }
    }

    revalidatePath('/dre')
    refresh()
    return { success: true, amount }
  } catch {
    return { success: false, error: 'Servico indisponivel.' }
  }
}

const GATEWAY_URL = resolveGatewayUrl()

type BatchClosingState =
  | { success: true; data: BatchAccountingClosingResponse }
  | { success: false; error: string }
  | null

function batchClosingErrorMessage(message?: string) {
  if (message === 'tenant_ids is required') {
    return 'Selecione pelo menos um cliente para fechamento.'
  }
  if (message === 'tenant_ids_limit_exceeded') {
    return 'O lote pode ter no maximo 100 clientes por vez.'
  }
  if (message === 'signature_hash is required') {
    return 'Nao foi possivel gerar a assinatura do lote.'
  }
  if (message === 'accountant_role_required') {
    return 'Apenas usuarios do time BPO podem assinar fechamentos em lote.'
  }
  if (message === 'tenant_mismatch') {
    return 'O lote contem cliente fora da carteira deste contador.'
  }
  return message ?? 'Erro ao assinar fechamentos em lote.'
}

export async function batchSignAccountingClosingsAction(
  prevState: BatchClosingState,
  formData: FormData
): Promise<BatchClosingState> {
  const [token, session] = await Promise.all([getToken(), getSession()])
  if (!token || !session || !isBpoOperator(session.roles)) {
    return { success: false, error: 'Sessao expirada ou usuario sem acesso BPO.' }
  }

  const periodMonth = String(formData.get('period_month') ?? '').trim()
  const tenantIds = formData
    .getAll('tenant_ids')
    .map((value) => String(value).trim())
    .filter(Boolean)
  const uniqueTenantIds = [...new Set(tenantIds)]

  if (!/^\d{4}-\d{2}$/.test(periodMonth) || uniqueTenantIds.length === 0) {
    return { success: false, error: 'Selecione clientes pendentes e um periodo valido.' }
  }

  const signatureHash = `bpo-batch:${periodMonth}:${session.userId}:${Date.now()}`

  try {
    const res = await fetch(
      `${GATEWAY_URL}/api/reports/bpo/closings/${periodMonth}/batch-sign`,
      {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
        body: JSON.stringify({
          tenant_ids: uniqueTenantIds,
          signature_hash: signatureHash,
        }),
        cache: 'no-store',
      }
    )
    if (!res.ok) {
      const err = await res.json().catch(() => ({})) as Record<string, unknown>
      const msg = (err.message ?? err.details) as string | undefined
      return { success: false, error: batchClosingErrorMessage(msg) }
    }

    const data = await res.json() as BatchAccountingClosingResponse
    revalidatePath('/bpo')
    revalidatePath('/dre')
    refresh()
    return { success: true, data }
  } catch {
    return { success: false, error: 'Servico indisponivel.' }
  }
}

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
    estimated_tax_rate: (parseFloat(formData.get('estimated_tax_rate') as string) || 0) / 100,
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
