'use server'

import { cookies } from 'next/headers'
import { redirect } from 'next/navigation'
import { resolveGatewayUrl } from '@/shared/config/gateway-url'
import { COOKIE_NAME } from '@/entities/session/server/session'
import { getLocale, localePath } from '@/shared/i18n/server-locale'
import { getDictionary } from '@/shared/i18n/get-dictionary'
import type { CompanyLookup } from '@/shared/types'

const GATEWAY_URL = resolveGatewayUrl()

type AuthState = { error: string } | null
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
      return { error: body.message ?? dict.auth.serverErrors.invalidCredentials }
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
      return { error: body.message ?? dict.auth.serverErrors.registerError }
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
