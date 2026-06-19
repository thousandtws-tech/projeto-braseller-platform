import { randomBytes, timingSafeEqual } from 'node:crypto'

import type { Locale } from '@/shared/i18n/config'

export const MERCADO_LIVRE_OAUTH_STATE_COOKIE = 'brasaller_ml_oauth_state'
export const MERCADO_LIVRE_OAUTH_STATE_TTL_SECONDS = 10 * 60

const MERCADO_LIVRE_AUTHORIZATION_URL =
  'https://auth.mercadolivre.com.br/authorization'

export function createMercadoLivreOAuthState(): string {
  return randomBytes(32).toString('base64url')
}

export function getMercadoLivreRedirectUri(lang: Locale): string {
  const appUrl = requiredEnv('NEXT_PUBLIC_APP_URL').replace(/\/+$/, '')
  return `${appUrl}/${lang}/conectores/callback/mercado-livre`
}

export function buildMercadoLivreOAuthUrl(lang: Locale, state: string): string {
  const clientId = requiredEnv('NEXT_PUBLIC_MERCADO_LIVRE_CLIENT_ID')
  const redirectUri = getMercadoLivreRedirectUri(lang)

  return (
    `${MERCADO_LIVRE_AUTHORIZATION_URL}?response_type=code` +
    `&client_id=${encodeURIComponent(clientId)}` +
    `&redirect_uri=${encodeURIComponent(redirectUri)}` +
    `&state=${encodeURIComponent(state)}`
  )
}

export function isValidMercadoLivreOAuthState(
  receivedState: string | undefined,
  expectedState: string | undefined
): boolean {
  if (!receivedState || !expectedState) return false

  const received = Buffer.from(receivedState)
  const expected = Buffer.from(expectedState)

  return received.length === expected.length && timingSafeEqual(received, expected)
}

function requiredEnv(name: string): string {
  const value = process.env[name]?.trim()
  if (!value) throw new Error(`Missing required environment variable: ${name}`)
  return value
}
