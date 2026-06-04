'use server'

import { cookies } from 'next/headers'
import { redirect } from 'next/navigation'
import { COOKIE_NAME } from '@/lib/auth'

const GATEWAY_URL =
  process.env.GATEWAY_URL ??
  process.env.NEXT_PUBLIC_GATEWAY_URL ??
  'http://localhost:8080'

type AuthState = { error: string } | null

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

  redirect('/dashboard')
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

  redirect('/dashboard')
}

export async function logoutAction(): Promise<void> {
  const store = await cookies()
  store.delete(COOKIE_NAME)
  redirect('/login')
}
