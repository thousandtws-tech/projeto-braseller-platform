'use server'

import { getToken, getSession } from '@/lib/auth'

const GATEWAY_URL =
  process.env.GATEWAY_URL ??
  process.env.NEXT_PUBLIC_GATEWAY_URL ??
  'http://localhost:8080'

type ActionState = { success: true; message: string } | { success: false; error: string } | null

export async function grantAccountantAction(
  prevState: ActionState,
  formData: FormData
): Promise<ActionState> {
  const email = (formData.get('accountantEmail') as string)?.trim()
  const firstName = (formData.get('accountantFirstName') as string)?.trim()
  const lastName = (formData.get('accountantLastName') as string)?.trim()
  const temporaryPassword = (formData.get('accountantPassword') as string)

  if (!email || !firstName || !lastName || !temporaryPassword) {
    return { success: false, error: 'Preencha todos os campos.' }
  }

  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!emailRegex.test(email)) return { success: false, error: 'E-mail inválido.' }

  if (temporaryPassword.length < 8) {
    return { success: false, error: 'A senha temporária deve ter no mínimo 8 caracteres.' }
  }

  const [token, session] = await Promise.all([getToken(), getSession()])

  if (!token || !session) return { success: false, error: 'Sessão expirada. Faça login novamente.' }

  try {
    const res = await fetch(
      `${GATEWAY_URL}/api/users/tenants/${session.tenantId}/accountants`,
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({ email, firstName, lastName, temporaryPassword }),
        cache: 'no-store',
      }
    )

    if (!res.ok) {
      const body = await res.json().catch(() => ({}))
      return { success: false, error: body.message ?? 'Não foi possível conceder o acesso.' }
    }

    return { success: true, message: `Acesso concedido para ${firstName} ${lastName} (${email}).` }
  } catch {
    return { success: false, error: 'Serviço temporariamente indisponível.' }
  }
}
