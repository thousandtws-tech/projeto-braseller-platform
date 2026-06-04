import { NextRequest, NextResponse } from 'next/server'
import { COOKIE_NAME } from '@/lib/auth'

const PROTECTED = ['/dashboard', '/lancamentos', '/despesas', '/dre', '/conectores', '/notificacoes', '/configuracoes', '/contador', '/plano', '/conectores/callback']
const AUTH_ONLY = ['/login', '/register']

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

export function proxy(request: NextRequest) {
  const { pathname } = request.nextUrl
  const token = request.cookies.get(COOKIE_NAME)?.value
  const tokenExpired = token ? isExpiredToken(token) : false
  const hasActiveToken = Boolean(token && !tokenExpired)

  const isProtected = PROTECTED.some((p) => pathname === p || pathname.startsWith(p + '/'))
  const isAuthOnly = AUTH_ONLY.some((p) => pathname === p || pathname.startsWith(p + '/'))

  if (isProtected && !hasActiveToken) {
    const loginUrl = new URL('/login', request.url)
    loginUrl.searchParams.set('from', pathname)
    const response = NextResponse.redirect(loginUrl)
    if (tokenExpired) response.cookies.delete(COOKIE_NAME)
    return response
  }

  if ((isAuthOnly || pathname === '/') && hasActiveToken) {
    return NextResponse.redirect(new URL('/dashboard', request.url))
  }

  if (tokenExpired) {
    const response = NextResponse.next()
    response.cookies.delete(COOKIE_NAME)
    return response
  }

  return NextResponse.next()
}

export const config = {
  matcher: ['/((?!_next/static|_next/image|favicon.ico|public).*)'],
}
