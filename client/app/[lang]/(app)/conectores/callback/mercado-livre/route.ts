import { NextRequest, NextResponse } from 'next/server'

import { COOKIE_NAME } from '@/entities/session/server/session'
import {
  getMercadoLivreRedirectUri,
  isValidMercadoLivreOAuthState,
  MERCADO_LIVRE_OAUTH_STATE_COOKIE,
} from '@/features/connectors/server/mercado-livre-oauth'
import { isLocale } from '@/shared/i18n/config'

interface Context {
  params: Promise<{ lang: string }>
}

export async function GET(request: NextRequest, context: Context) {
  const { lang: langValue } = await context.params
  const lang = isLocale(langValue) ? langValue : 'pt-BR'
  const connectorsUrl = new URL(`/${lang}/conectores`, request.url)
  const loginUrl = new URL(`/${lang}/login?expired=1`, request.url)
  const code = request.nextUrl.searchParams.get('code') ?? undefined
  const error = request.nextUrl.searchParams.get('error') ?? undefined
  const state = request.nextUrl.searchParams.get('state') ?? undefined
  const expectedState = request.cookies.get(MERCADO_LIVRE_OAUTH_STATE_COOKIE)?.value
  const token = request.cookies.get(COOKIE_NAME)?.value

  if (!isValidMercadoLivreOAuthState(state, expectedState)) {
    return oauthRedirect(connectorsUrl, lang, 'invalid_state')
  }

  if (error) {
    return oauthRedirect(
      connectorsUrl,
      lang,
      error === 'access_denied' ? 'access_denied' : 'authentication_failed'
    )
  }

  if (!code) {
    return oauthRedirect(connectorsUrl, lang, 'missing_code')
  }

  if (!token) {
    return oauthRedirect(loginUrl, lang)
  }

  const gatewayUrl = process.env.GATEWAY_URL?.replace(/\/+$/, '')
  if (!gatewayUrl) {
    return oauthRedirect(connectorsUrl, lang, 'oauth_not_configured')
  }

  try {
    const response = await fetch(
      `${gatewayUrl}/api/core/connectors/mercado-livre/authenticate`,
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          credentials: {
            code,
            redirect_uri: getMercadoLivreRedirectUri(lang),
          },
        }),
        cache: 'no-store',
      }
    )

    if (response.status === 401) {
      const redirectResponse = oauthRedirect(loginUrl, lang)
      redirectResponse.cookies.delete(COOKIE_NAME)
      return redirectResponse
    }

    if (!response.ok) {
      return oauthRedirect(connectorsUrl, lang, 'authentication_failed')
    }
  } catch {
    return oauthRedirect(connectorsUrl, lang, 'service_unavailable')
  }

  connectorsUrl.searchParams.set('connected', 'mercado-livre')
  return oauthRedirect(connectorsUrl, lang)
}

function oauthRedirect(url: URL, lang: string, error?: string) {
  if (error) url.searchParams.set('auth_error', error)

  const response = NextResponse.redirect(url)
  response.cookies.set(MERCADO_LIVRE_OAUTH_STATE_COOKIE, '', {
    httpOnly: true,
    secure: process.env.NODE_ENV === 'production',
    sameSite: 'lax',
    maxAge: 0,
    path: `/${lang}/conectores/callback/mercado-livre`,
  })
  return response
}
