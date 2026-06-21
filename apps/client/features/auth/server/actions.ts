'use server'

import { cookies } from 'next/headers'
import { redirect } from 'next/navigation'
import { resolveGatewayUrl } from '@/shared/config/gateway-url'
import { COOKIE_NAME } from '@/entities/session/server/session'
import { getLocale, localePath } from '@/shared/i18n/server-locale'
import { getDictionary } from '@/shared/i18n/get-dictionary'
import type { CompanyLookup } from '@/shared/types'

const GATEWAY_URL = resolveGatewayUrl()

type AuthState = { error?: string; success?: string; email?: string } | null
type CompanyLookupState = { data: CompanyLookup; error?: never } | { data?: never; error: string }

export async function loginAction(prevState: AuthState, formData: FormData): Promise<AuthState> {
  const email = formData.get('email') as string
  const password = formData.get('password') as string
  const dict = await getDictionary(await getLocale())

  if (!email || !password) {
    return { error: dict.auth.serverErrors.fillAllFields }
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
      return { error: loginErrorMessage(body.message, dict) }
    }

    const data = await res.json()
    accessToken = data.accessToken ?? data.access_token ?? null

    if (!accessToken) {
      return { error: dict.auth.serverErrors.invalidAuthResponse }
    }
  } catch {
    return { error: dict.auth.serverErrors.serviceUnavailable }
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
  const dict = await getDictionary(await getLocale())

  if (!tenantName || !fullName || !email || !password) {
    return { error: dict.auth.serverErrors.fillAllFields }
  }
  if (password.length < 8) {
    return { error: dict.auth.serverErrors.passwordMinLength }
  }

  let redirectTo: string | null = null

  try {
    const res = await fetch(`${GATEWAY_URL}/api/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ tenantName, fullName, email, password }),
      cache: 'no-store',
    })

    if (!res.ok) {
      const body = await res.json().catch(() => ({}))
      return { error: body.message ?? dict.auth.serverErrors.registerError }
    }

    redirectTo = await localePath(`/verify-email?email=${encodeURIComponent(email)}`)
  } catch {
    return { error: dict.auth.serverErrors.serviceUnavailable }
  }

  if (redirectTo) {
    redirect(redirectTo)
  }
  return null
}

export async function requestEmailVerificationAction(prevState: AuthState, formData: FormData): Promise<AuthState> {
  const email = (formData.get('email') as string)?.trim()
  const dict = await getDictionary(await getLocale())

  if (!email) {
    return { error: dict.auth.serverErrors.fillAllFields }
  }

  try {
    const res = await fetch(`${GATEWAY_URL}/api/auth/email-verification/request`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email }),
      cache: 'no-store',
    })

    if (!res.ok) {
      const body = await res.json().catch(() => ({}))
      return { error: body.message ?? 'Nao foi possivel reenviar o codigo agora.' }
    }

    return { success: 'Se o e-mail estiver pendente, enviaremos um novo codigo.', email }
  } catch {
    return { error: dict.auth.serverErrors.serviceUnavailable }
  }
}

export async function verifyEmailAction(prevState: AuthState, formData: FormData): Promise<AuthState> {
  const email = (formData.get('email') as string)?.trim()
  const code = (formData.get('code') as string)?.trim()
  const dict = await getDictionary(await getLocale())
  let redirectTo: string | null = null

  if (!email || !code) {
    return { error: dict.auth.serverErrors.fillAllFields }
  }

  try {
    const res = await fetch(`${GATEWAY_URL}/api/auth/email-verification/verify`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, code }),
      cache: 'no-store',
    })

    if (!res.ok) {
      const body = await res.json().catch(() => ({}))
      return { error: codeErrorMessage(body.message) }
    }

    redirectTo = await localePath('/login?verified=1')
  } catch {
    return { error: dict.auth.serverErrors.serviceUnavailable }
  }

  if (redirectTo) {
    redirect(redirectTo)
  }
  return null
}

export async function forgotPasswordAction(prevState: AuthState, formData: FormData): Promise<AuthState> {
  const email = (formData.get('email') as string)?.trim()
  const dict = await getDictionary(await getLocale())

  if (!email) {
    return { error: dict.auth.serverErrors.fillAllFields }
  }

  try {
    const res = await fetch(`${GATEWAY_URL}/api/auth/password-reset/request`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email }),
      cache: 'no-store',
    })

    if (!res.ok) {
      const body = await res.json().catch(() => ({}))
      return { error: body.message ?? 'Nao foi possivel iniciar a recuperacao agora.' }
    }

    return { success: 'Se o e-mail existir, enviaremos um codigo de recuperacao.', email }
  } catch {
    return { error: dict.auth.serverErrors.serviceUnavailable }
  }
}

export async function resetPasswordAction(prevState: AuthState, formData: FormData): Promise<AuthState> {
  const email = (formData.get('email') as string)?.trim()
  const code = (formData.get('code') as string)?.trim()
  const newPassword = formData.get('newPassword') as string
  const confirmPassword = formData.get('confirmPassword') as string
  const dict = await getDictionary(await getLocale())
  let redirectTo: string | null = null

  if (!email || !code || !newPassword || !confirmPassword) {
    return { error: dict.auth.serverErrors.fillAllFields }
  }
  if (newPassword.length < 8) {
    return { error: dict.auth.serverErrors.passwordMinLength }
  }
  if (newPassword !== confirmPassword) {
    return { error: 'As senhas nao conferem.' }
  }

  try {
    const res = await fetch(`${GATEWAY_URL}/api/auth/password-reset/reset`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, code, newPassword }),
      cache: 'no-store',
    })

    if (!res.ok) {
      const body = await res.json().catch(() => ({}))
      return { error: codeErrorMessage(body.message) }
    }

    redirectTo = await localePath('/login?reset=1')
  } catch {
    return { error: dict.auth.serverErrors.serviceUnavailable }
  }

  if (redirectTo) {
    redirect(redirectTo)
  }
  return null
}

export async function lookupCompanyByCnpjAction(cnpj: string): Promise<CompanyLookupState> {
  const digits = cnpj.replace(/\D/g, '')
  const dict = await getDictionary(await getLocale())

  if (digits.length !== 14) {
    return { error: dict.auth.serverErrors.cnpjDigitsRequired }
  }

  try {
    const res = await fetch(`${GATEWAY_URL}/api/users/company-lookup/cnpj/${digits}`, {
      method: 'GET',
      headers: { Accept: 'application/json' },
      cache: 'no-store',
    })

    if (res.status === 404) {
      return { error: dict.auth.serverErrors.cnpjNotFound }
    }

    if (!res.ok) {
      const body = await res.json().catch(() => ({}))
      return { error: body.message ?? dict.auth.serverErrors.cnpjLookupError }
    }

    return { data: await res.json() as CompanyLookup }
  } catch {
    return { error: dict.auth.serverErrors.cnpjServiceUnavailable }
  }
}

export async function logoutAction(): Promise<void> {
  const store = await cookies()
  store.delete(COOKIE_NAME)
  redirect(await localePath('/login'))
}

function loginErrorMessage(message: unknown, dict: Awaited<ReturnType<typeof getDictionary>>) {
  if (message === 'email_not_verified') {
    return 'Verifique seu e-mail antes de entrar. Use o codigo enviado para ativar sua conta.'
  }
  if (message === 'account_blocked') {
    return 'Nao foi possivel acessar esta conta no momento.'
  }
  return typeof message === 'string' && message && message !== 'invalid_credentials'
    ? message
    : dict.auth.serverErrors.invalidCredentials
}

function codeErrorMessage(message: unknown) {
  if (message === 'invalid_or_expired_code') {
    return 'Codigo invalido, expirado ou ja utilizado.'
  }
  return typeof message === 'string' && message ? message : 'Nao foi possivel validar o codigo.'
}
