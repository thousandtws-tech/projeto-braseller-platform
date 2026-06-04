import { redirect } from 'next/navigation'
import { getToken } from '@/lib/auth'

const GATEWAY_URL =
  process.env.GATEWAY_URL ??
  process.env.NEXT_PUBLIC_GATEWAY_URL ??
  'http://localhost:8080'

const ML_REDIRECT_URI =
  'https://gateway-api.salmonrock-4d3f2812.brazilsouth.azurecontainerapps.io/integrations/mercado-livre/callback'

interface Props {
  searchParams: Promise<{ code?: string; error?: string; connector?: string }>
}

export default async function ConnectorCallbackPage({ searchParams }: Props) {
  const { code, error } = await searchParams

  if (error) {
    redirect(`/conectores?auth_error=${encodeURIComponent(error)}`)
  }

  if (!code) {
    redirect('/conectores')
  }

  const token = await getToken()
  if (!token) {
    redirect('/login?expired=1')
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
      redirect('/login?expired=1')
    }

    if (!res.ok) {
      const body = await res.json().catch(() => ({}))
      const msg = encodeURIComponent(body.message ?? 'Erro ao autenticar o Mercado Livre.')
      redirect(`/conectores?auth_error=${msg}`)
    }

    redirect('/conectores?connected=mercado-livre')
  } catch {
    redirect('/conectores?auth_error=Servi%C3%A7o+indispon%C3%ADvel')
  }
}
