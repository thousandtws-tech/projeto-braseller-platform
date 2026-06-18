import { redirect } from 'next/navigation'
import { getToken } from '@/entities/session/server/session'
import { getDictionary } from '@/shared/i18n/get-dictionary'
import type { Locale } from '@/shared/i18n/config'

const GATEWAY_URL =
  process.env.GATEWAY_URL ??
  process.env.NEXT_PUBLIC_GATEWAY_URL ??
  'http://localhost:8080'

interface Props {
  params: Promise<{ lang: Locale }>
  searchParams: Promise<{
    code?: string
    error?: string
  }>
}

export default async function MercadoLivreCallbackPage({
  params,
  searchParams,
}: Props) {
  const { lang } = await params
  const dict = await getDictionary(lang)
  const { code, error } = await searchParams

  const redirectUri = `${process.env.NEXT_PUBLIC_APP_URL ?? 'http://localhost:3000'}/${lang}/conectores/callback/mercado-livre`

  if (error) {
    redirect(`/${lang}/conectores?auth_error=${encodeURIComponent(error)}`)
  }

  if (!code) {
    redirect(`/${lang}/conectores?auth_error=${encodeURIComponent(dict.connectors.callback.authError)}`)
  }

  const token = await getToken()

  if (!token) {
    redirect(`/${lang}/login?expired=1`)
  }

  let redirectTo = `/${lang}/conectores?connected=mercado-livre`

  try {
    const res = await fetch(
      `${GATEWAY_URL}/api/core/connectors/mercado-livre/authenticate`,
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          credentials: {
            code,
            redirect_uri: redirectUri,
          },
        }),
        cache: 'no-store',
      }
    )

    if (res.status === 401) {
      redirectTo = `/${lang}/login?expired=1`
    } else if (!res.ok) {
      const body = await res.json().catch(() => ({}))

      redirectTo = `/${lang}/conectores?auth_error=${encodeURIComponent(
        body.message ?? dict.connectors.callback.authError
      )}`
    }
  } catch {
    redirectTo = `/${lang}/conectores?auth_error=${encodeURIComponent(
      dict.connectors.callback.serviceUnavailable
    )}`
  }

  redirect(redirectTo)
}