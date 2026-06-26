'use server'

import { cookies } from 'next/headers'
import { redirect } from 'next/navigation'
import { resolveGatewayUrl } from '@/shared/config/gateway-url'
import { COOKIE_NAME } from '@/entities/session/server/session'
import { localePath } from '@/shared/i18n/server-locale'
import type { CompanyLookup } from '@/shared/types'

const GATEWAY_URL = resolveGatewayUrl()

type AuthState = { error: string } | null
export type RecoveryState = { error?: string; success?: boolean } | null
export type VerifyEmailState = { error?: string; success?: string } | null
type CompanyLookupState = { data: CompanyLookup; error?: never } | { data?: never; error: string }

type ApiErrorPayload = {
  message?: string
}

type LoginResponse = {
  accessToken?: string
  access_token?: string
}

type RegistrationResponse = {
  email?: string
}

function readErrorMessage(payload: ApiErrorPayload | null | undefined, fallback: string) {
  return typeof payload?.message === 'string' && payload.message.trim().length > 0
    ? payload.message
    : fallback
}

async function readErrorPayload(response: Response): Promise<ApiErrorPayload> {
  return response.json().catch(() => ({}))
}

function mapLoginError(code: string) {
  if (code === 'invalid_credentials') {
    return 'Credenciais inválidas. Verifique e tente novamente.'
  }
  if (code === 'account_not_active') {
    return 'Sua conta ainda não está disponível para acesso.'
  }
  return 'Não foi possível concluir o login agora. Tente novamente.'
}

function mapRegisterError(code: string) {
  if (code === 'Could not register tenant') {
    return 'Já existe uma conta com este e-mail ou empresa.'
  }
  return 'Erro ao criar conta. Tente novamente.'
}

function mapVerifyEmailError(code: string) {
  if (code === 'invalid_verification_code') {
    return 'Código inválido. Confira o e-mail e tente novamente.'
  }
  if (code === 'verification_code_expired') {
    return 'O código expirou. Solicite um novo envio para continuar.'
  }
  if (code === 'email_already_verified') {
    return 'Este e-mail já foi verificado. Faça login para continuar.'
  }
  return 'Não foi possível validar o código agora. Tente novamente.'
}

function mapResendVerificationError(code: string) {
  if (code === 'verification_code_recently_sent') {
    return 'Aguarde alguns instantes antes de solicitar um novo código.'
  }
  if (code === 'email_already_verified') {
    return 'Este e-mail já foi verificado. Faça login para continuar.'
  }
  return 'Não foi possível reenviar o código agora. Tente novamente.'
}

export async function loginAction(prevState: AuthState, formData: FormData): Promise<AuthState> {
  void prevState
  const email = (formData.get('email') as string | null)?.trim() ?? ''
  const password = (formData.get('password') as string | null) ?? ''

  if (!email || !password) {
    return { error: 'Preencha todos os campos.' }
  }

  let redirectTo: string | null = null
  let accessToken: string | null = null

  try {
    const res = await fetch(`${GATEWAY_URL}/api/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password }),
      cache: 'no-store',
    })

    if (!res.ok) {
      const body = await readErrorPayload(res)
      const code = readErrorMessage(body, 'invalid_credentials')

      if (code === 'email_verification_required') {
        redirectTo = await localePath(`/verify-code?email=${encodeURIComponent(email)}&reason=email_verification_required`)
      } else {
        return { error: mapLoginError(code) }
      }
    } else {
      const data = await res.json() as LoginResponse
      accessToken = data.accessToken ?? data.access_token ?? null

      if (!accessToken) {
        return { error: 'Resposta inválida do servidor de autenticação.' }
      }
    }
  } catch {
    return { error: 'Serviço temporariamente indisponível. Tente novamente em instantes.' }
  }

  if (redirectTo) {
    redirect(redirectTo)
  }

  if (!accessToken) {
    return { error: 'Não foi possível concluir o login agora. Tente novamente.' }
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
  void prevState
  const tenantName = (formData.get('tenantName') as string | null)?.trim() ?? ''
  const fullName = (formData.get('fullName') as string | null)?.trim() ?? ''
  const email = (formData.get('email') as string | null)?.trim() ?? ''
  const password = (formData.get('password') as string | null) ?? ''

  if (!tenantName || !fullName || !email || !password) {
    return { error: 'Preencha todos os campos.' }
  }
  if (password.length < 8) {
    return { error: 'A senha deve ter no mínimo 8 caracteres.' }
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
      const body = await readErrorPayload(res)
      return { error: mapRegisterError(readErrorMessage(body, 'register_error')) }
    }

    const data = await res.json() as RegistrationResponse
    const targetEmail = typeof data.email === 'string' && data.email.trim().length > 0 ? data.email : email
    redirectTo = await localePath(`/verify-code?email=${encodeURIComponent(targetEmail)}&registered=1`)
  } catch {
    return { error: 'Serviço temporariamente indisponível. Tente novamente em instantes.' }
  }

  if (redirectTo) {
    redirect(redirectTo)
  }

  return { error: 'Erro ao criar conta. Tente novamente.' }
}

export async function verifyEmailCodeAction(prevState: VerifyEmailState, formData: FormData): Promise<VerifyEmailState> {
  void prevState
  const email = (formData.get('email') as string | null)?.trim() ?? ''
  const code = (formData.get('code') as string | null)?.trim() ?? ''

  if (!email || !code) {
    return { error: 'Informe o e-mail e o código recebido.' }
  }

  let redirectTo: string | null = null

  try {
    const res = await fetch(`${GATEWAY_URL}/api/auth/verify-email`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, code }),
      cache: 'no-store',
    })

    if (!res.ok) {
      const body = await readErrorPayload(res)
      return { error: mapVerifyEmailError(readErrorMessage(body, 'verification_failed')) }
    }

    redirectTo = await localePath(`/login?verified=1&email=${encodeURIComponent(email)}`)
  } catch {
    return { error: 'Serviço temporariamente indisponível. Tente novamente em instantes.' }
  }

  if (redirectTo) {
    redirect(redirectTo)
  }

  return { error: 'Não foi possível concluir a verificação agora.' }
}

export async function resendEmailVerificationCodeAction(
  prevState: VerifyEmailState,
  formData: FormData
): Promise<VerifyEmailState> {
  void prevState
  const email = (formData.get('email') as string | null)?.trim() ?? ''

  if (!email) {
    return { error: 'Informe o e-mail para reenviar o código.' }
  }

  try {
    const res = await fetch(`${GATEWAY_URL}/api/auth/resend-email-verification`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email }),
      cache: 'no-store',
    })

    if (!res.ok) {
      const body = await readErrorPayload(res)
      return { error: mapResendVerificationError(readErrorMessage(body, 'resend_failed')) }
    }

    return { success: 'Enviamos um novo código para o seu e-mail.' }
  } catch {
    return { error: 'Serviço temporariamente indisponível. Tente novamente em instantes.' }
  }
}

export async function requestPasswordRecoveryAction(
  prevState: RecoveryState,
  formData: FormData
): Promise<RecoveryState> {
  void prevState
  const email = (formData.get('email') as string)?.trim()
  if (!email) return { error: 'Informe seu e-mail.' }

  try {
    const res = await fetch(`${GATEWAY_URL}/api/auth/forgot-password`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email }),
      cache: 'no-store',
    })

    if (!res.ok) {
      const body = await res.json().catch(() => ({}))
      return { error: body.message ?? 'Não foi possível iniciar a recuperação agora.' }
    }

    return { success: true }
  } catch {
    return { error: 'Serviço temporariamente indisponível. Tente novamente em instantes.' }
  }
}

export async function lookupCompanyByCnpjAction(cnpj: string): Promise<CompanyLookupState> {
  const digits = cnpj.replace(/\D/g, '')

  if (digits.length !== 14) {
    return { error: 'Informe um CNPJ com 14 dígitos.' }
  }

  try {
    const res = await fetch(`${GATEWAY_URL}/api/users/company-lookup/cnpj/${digits}`, {
      method: 'GET',
      headers: { Accept: 'application/json' },
      cache: 'no-store',
    })

    if (res.status === 404) {
      return { error: 'CNPJ não encontrado na Receita Federal.' }
    }

    if (!res.ok) {
      const body = await res.json().catch(() => ({}))
      return { error: body.message ?? 'Não foi possível consultar o CNPJ agora.' }
    }

    return { data: await res.json() as CompanyLookup }
  } catch {
    return { error: 'Serviço de consulta CNPJ temporariamente indisponível.' }
  }
}

export async function logoutAction(): Promise<void> {
  const store = await cookies()
  store.delete(COOKIE_NAME)
  redirect(await localePath('/login'))
}
