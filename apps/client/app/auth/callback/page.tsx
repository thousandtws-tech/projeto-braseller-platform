'use client'

import { useEffect, use } from 'react'
import { useRouter } from 'next/navigation'
import { defaultLocale, isLocale, LOCALE_COOKIE } from '@/shared/i18n/config'

function getLocalePrefix() {
  const match = document.cookie.match(new RegExp(`${LOCALE_COOKIE}=([^;]+)`))
  const value = match?.[1]
  return isLocale(value) ? value : defaultLocale
}

export default function AuthCallbackPage({
  searchParams,
}: {
  searchParams: Promise<{ code?: string; error?: string }>
}) {
  const { code, error } = use(searchParams)
  const router = useRouter()

  useEffect(() => {
    const lang = getLocalePrefix()

    if (error || !code) {
      router.replace(`/${lang}/login?error=oauth_failed`)
      return
    }

    fetch('/api/auth/google/callback', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ code }),
    })
      .then(async (res) => {
        if (res.ok) {
          router.replace(`/${lang}/dashboard`)
          return
        }
        const body = await res.json().catch(() => ({})) as { message?: string }
        const errorCode = body.message === 'google_account_not_registered'
          ? 'google_account_not_registered'
          : 'oauth_failed'
        router.replace(`/${lang}/login?error=${errorCode}`)
      })
      .catch(() => router.replace(`/${lang}/login?error=oauth_failed`))
  }, [code, error, router])

  return (
    <div className="flex min-h-dvh items-center justify-center bg-background px-6">
      <div className="flex w-full max-w-sm flex-col items-center gap-5 rounded-lg border border-border bg-card p-8 text-center">
        <div className="flex size-10 items-center justify-center rounded-md bg-foreground text-sm font-bold text-background">B</div>
        <div className="size-7 animate-spin rounded-full border-2 border-muted border-t-foreground" />
        <div>
          <p className="font-medium">Validando seu acesso</p>
          <p className="mt-1 text-sm text-muted-foreground">Autenticando com Google...</p>
        </div>
      </div>
    </div>
  )
}
