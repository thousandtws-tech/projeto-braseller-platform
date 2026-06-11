import { redirect } from 'next/navigation'
import { getToken } from '@/entities/session/server/session'
import { getDictionary } from '@/shared/i18n/get-dictionary'
import type { Locale } from '@/shared/i18n/config'

const GATEWAY_URL =
  process.env.GATEWAY_URL ??
  process.env.NEXT_PUBLIC_GATEWAY_URL ??
  'http://localhost:8080'

const ML_REDIRECT_URI =
  'https://gateway-api.salmonrock-4d3f2812.brazilsouth.azurecontainerapps.io/integrations/mercado-livre/callback'

interface Props {
  params: Promise<{ lang: Locale }>
  searchParams: Promise<{ code?: string; error?: string; connector?: string }>
}

export default async function ConnectorCallbackPage({ params, searchParams }: Props) {
  const { lang } = await params
  const dict = await getDictionary(lang)
  const { code, error } = await searchParams

  if (error) {
    redirect(`/${lang}/conectores?auth_error=${encodeURIComponent(error)}`)
  }

  if (!code) {
    redirect(`/${lang}/conectores`)
  }

  const token = await getToken()
  if (!token) {
    redirect(`/${lang}/login?expired=1`)
  }

  try {
    const res = await fetch(`${GATEWAY_URL}/api/core/connectors/mercado-livre/authenticate`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify({
        credentials: {
          code,
          redirect_uri: ML_REDIRECT_URI,
        },
      }),
      cache: 'no-store',
    })

    if (res.status === 401) {
      redirect(`/${lang}/login?expired=1`)
    }

    if (!res.ok) {
      const body = await res.json().catch(() => ({}))
      const msg = encodeURIComponent(body.message ?? dict.connectors.callback.authError)
      redirect(`/${lang}/conectores?auth_error=${msg}`)
    }

    redirect(`/${lang}/conectores?connected=mercado-livre`)
  } catch {
    redirect(`/${lang}/conectores?auth_error=${encodeURIComponent(dict.connectors.callback.serviceUnavailable)}`)
  }
}
