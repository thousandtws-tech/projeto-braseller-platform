import { NextResponse } from 'next/server'
import { finishGoogleCallback } from '@/features/auth/server/google-oauth'

export async function POST(request: Request) {
  let payload: { code?: string; tenantName?: string }

  try {
    payload = await request.json()
  } catch {
    return NextResponse.json({ message: 'invalid_google_callback_payload' }, { status: 400 })
  }

  const code = payload.code?.trim()
  const tenantName = payload.tenantName?.trim()

  if (!code) {
    return NextResponse.json({ message: 'google_code_required' }, { status: 400 })
  }

  try {
    const result = await finishGoogleCallback(code, tenantName)
    return result.ok
      ? NextResponse.json({ ok: true })
      : NextResponse.json({ message: result.message }, { status: result.status })
  } catch {
    return NextResponse.json({ message: 'google_auth_unavailable' }, { status: 503 })
  }
}
