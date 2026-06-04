'use client'

import { useEffect, use } from 'react'
import { useRouter } from 'next/navigation'

export default function AuthCallbackPage({
  searchParams,
}: {
  searchParams: Promise<{ code?: string; error?: string }>
}) {
  const { code, error } = use(searchParams)
  const router = useRouter()

  useEffect(() => {
    if (error || !code) {
      router.replace('/login?error=oauth_failed')
      return
    }

    fetch('/api/auth/google/callback', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ code }),
    })
      .then(() => router.replace('/dashboard'))
      .catch(() => router.replace('/login?error=oauth_failed'))
  }, [code, error, router])

  return (
    <div className="min-h-screen flex items-center justify-center">
      <div className="text-center space-y-3">
        <div className="size-10 border-2 border-primary border-t-transparent rounded-full animate-spin mx-auto" />
        <p className="text-sm text-muted-foreground">Autenticando com Google...</p>
      </div>
    </div>
  )
}
