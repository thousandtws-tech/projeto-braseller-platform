'use server'

import { cookies } from 'next/headers'
import { redirect } from 'next/navigation'
import { resolveGatewayUrl } from '@/shared/config/gateway-url'
import { COOKIE_NAME } from '@/entities/session/server/session'
import { localePath } from '@/shared/i18n/server-locale'
import type { CompanyLookup } from '@/shared/types'

const GATEWAY_URL = resolveGatewayUrl()

type AuthState = { error: string } | null
type CompanyLookupState = { data: CompanyLookup; error?: never } | { data?: never; error: string }

export async function loginAction(prevState: AuthState, formData: FormData): Promise<AuthState> {
  const email = formData.get('email') as string
  const password = formData.get('password') as string

  if (!email || !password) {
    return { error: 'Preencha todos os campos.' }
  }

  let accessToken: string | null = null

  try {
    const res = await fetch(`${GATEWAY_URL}/api/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password }),
      cache: 'no-store',
    })

    if (!res.ok) {
      const body = await res.json().catch(() => ({}))
      return { error: body.message ?? 'Credenciais invalidas. Verifique e tente novamente.' }
    }

    const data = await res.json()
    accessToken = data.accessToken ?? data.access_token ?? null

    if (!accessToken) {
      return { error: 'Resposta invalida do servidor de autenticacao.' }
    }
  } catch {
    return { error: 'Servico temporariamente indisponivel. Tente novamente em instantes.' }
  }

  const store = await cookies()
  store.set(COOKIE_NAME, accessToken, {
    httpOnly: true,
    secure: process.env.NODE_ENV === 'production',
    sameSite: 'lax',
    maxAge: 60 * 60 * 24,
    path: '/',
  })

  redirect(await localePath('/dashboard'))
}

export async function registerAction(prevState: AuthState, formData: FormData): Promise<AuthState> {
  const tenantName = (formData.get('tenantName') as string)?.trim()
  const fullName = (formData.get('fullName') as string)?.trim()
  const email = (formData.get('email') as string)?.trim()
  const password = formData.get('password') as string

  if (!tenantName || !fullName || !email || !password) {
    return { error: 'Preencha todos os campos.' }
  }
  if (password.length < 8) {
    return { error: 'A senha deve ter no minimo 8 caracteres.' }
  }

  let accessToken: string | null = null

  try {
    const res = await fetch(`${GATEWAY_URL}/api/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ tenantName, fullName, email, password }),
      cache: 'no-store',
    })

    if (!res.ok) {
      const body = await res.json().catch(() => ({}))
      return { error: body.message ?? 'Erro ao criar conta. Tente novamente.' }
    }

    const data = await res.json()
    accessToken = data.accessToken ?? data.access_token ?? null

    if (!accessToken) {
      return { error: 'Resposta invalida do servidor de autenticacao.' }
    }
  } catch {
    return { error: 'Servico temporariamente indisponivel. Tente novamente em instantes.' }
  }

  const store = await cookies()
  store.set(COOKIE_NAME, accessToken, {
    httpOnly: true,
    secure: process.env.NODE_ENV === 'production',
    sameSite: 'lax',
    maxAge: 60 * 60 * 24,
    path: '/',
  })

  redirect(await localePath('/dashboard'))
}

export async function lookupCompanyByCnpjAction(cnpj: string): Promise<CompanyLookupState> {
  const digits = cnpj.replace(/\D/g, '')

  if (digits.length !== 14) {
    return { error: 'Informe um CNPJ com 14 digitos.' }
  }

  try {
    const res = await fetch(`${GATEWAY_URL}/api/users/company-lookup/cnpj/${digits}`, {
      method: 'GET',
      headers: { Accept: 'application/json' },
      cache: 'no-store',
    })

    if (res.status === 404) {
      return { error: 'CNPJ nao encontrado na Receita Federal.' }
    }

    if (!res.ok) {
      const body = await res.json().catch(() => ({}))
      return { error: body.message ?? 'Nao foi possivel consultar o CNPJ agora.' }
    }

    return { data: await res.json() as CompanyLookup }
  } catch {
    return { error: 'Servico de consulta CNPJ temporariamente indisponivel.' }
  }
}

export async function logoutAction(): Promise<void> {
  const store = await cookies()
  store.delete(COOKIE_NAME)
  redirect(await localePath('/login'))
}
