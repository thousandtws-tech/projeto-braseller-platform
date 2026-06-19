import { type NextRequest, NextResponse } from 'next/server'
import { getGoogleAuthorizeUrl } from '@/features/auth/server/google-oauth'

export async function GET(request: NextRequest) {
  try {
    const authorizeUrl = await getGoogleAuthorizeUrl()

    if (!authorizeUrl) {
      return NextResponse.redirect(new URL('/login?error=google_unavailable', request.nextUrl.origin))
    }

    return NextResponse.redirect(authorizeUrl)
  } catch {
    return NextResponse.redirect(new URL('/login?error=google_unavailable', request.nextUrl.origin))
  }
}
