import { NextRequest, NextResponse } from 'next/server'
import { COOKIE_NAME } from '@/entities/session/server/session'
import { defaultLocale, isLocale, locales, LOCALE_COOKIE, type Locale } from '@/shared/i18n/config'

const PROTECTED = ['/dashboard', '/lancamentos', '/despesas', '/estoque', '/extrato', '/dre', '/balanco', '/conectores', '/notificacoes', '/configuracoes', '/contador', '/plano', '/conectores/callback', '/bpo']
const AUTH_ONLY = ['/login', '/register', '/forgot-password', '/reset-password', '/verify-email']
const UNLOCALIZED_PREFIXES = ['/auth', '/api']

function decodeBase64Url(value: string) {
  const normalized = value.replace(/-/g, '+').replace(/_/g, '/')
  const padded = normalized.padEnd(Math.ceil(normalized.length / 4) * 4, '=')
  return atob(padded)
}

function isExpiredToken(token: string) {
  try {
    const parts = token.split('.')
    if (parts.length !== 3) return true

    const payload = JSON.parse(decodeBase64Url(parts[1]))
    const exp = typeof payload.exp === 'number' ? payload.exp : Number(payload.exp)
    return Number.isFinite(exp) && exp > 0 && exp * 1000 <= Date.now()
  } catch {
    return true
  }
}

function isServerActionRequest(request: NextRequest) {
  return request.method === 'POST' && request.headers.has('next-action')
}

function negotiateLocale(request: NextRequest): Locale {
  const cookieLocale = request.cookies.get(LOCALE_COOKIE)?.value
  if (isLocale(cookieLocale)) return cookieLocale

  const acceptLanguage = request.headers.get('accept-language')
  if (acceptLanguage) {
    for (const part of acceptLanguage.split(',')) {
      const tag = part.split(';')[0].trim()
      const match = locales.find(
        (l) => l.toLowerCase() === tag.toLowerCase() || l.toLowerCase() === tag.split('-')[0].toLowerCase()
      )
      if (match) return match
    }
  }

  return defaultLocale
}

export function proxy(request: NextRequest) {
  if (isServerActionRequest(request)) {
    return NextResponse.next()
  }

  const { pathname } = request.nextUrl

  if (UNLOCALIZED_PREFIXES.some((p) => pathname === p || pathname.startsWith(p + '/'))) {
    return NextResponse.next()
  }

  const segments = pathname.split('/')
  const maybeLocale = segments[1]

  if (!isLocale(maybeLocale)) {
    const locale = negotiateLocale(request)
    const redirectUrl = new URL(`/${locale}${pathname}`, request.url)
    redirectUrl.search = request.nextUrl.search
    const response = NextResponse.redirect(redirectUrl)
    response.cookies.set(LOCALE_COOKIE, locale, { path: '/', maxAge: 60 * 60 * 24 * 365 })
    return response
  }

  const locale = maybeLocale
  const innerPath = '/' + segments.slice(2).join('/')

  const token = request.cookies.get(COOKIE_NAME)?.value
  const tokenExpired = token ? isExpiredToken(token) : false
  const hasActiveToken = Boolean(token && !tokenExpired)

  const isProtected = PROTECTED.some((p) => innerPath === p || innerPath.startsWith(p + '/'))
  const isAuthOnly = AUTH_ONLY.some((p) => innerPath === p || innerPath.startsWith(p + '/'))

  if (isProtected && !hasActiveToken) {
    const loginUrl = new URL(`/${locale}/login`, request.url)
    loginUrl.searchParams.set('from', innerPath)
    const response = NextResponse.redirect(loginUrl)
    if (tokenExpired) response.cookies.delete(COOKIE_NAME)
    return response
  }

  if ((isAuthOnly || innerPath === '/') && hasActiveToken) {
    return NextResponse.redirect(new URL(`/${locale}/dashboard`, request.url))
  }

  const response = NextResponse.next()
  if (request.cookies.get(LOCALE_COOKIE)?.value !== locale) {
    response.cookies.set(LOCALE_COOKIE, locale, { path: '/', maxAge: 60 * 60 * 24 * 365 })
  }
  if (tokenExpired) response.cookies.delete(COOKIE_NAME)
  return response
}

export const config = {
  matcher: ['/((?!_next/static|_next/image|favicon.ico|public).*)'],
}
