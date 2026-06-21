import { redirect } from 'next/navigation'
import { getToken } from '@/entities/session/server/session'
import { getDictionary } from '@/shared/i18n/get-dictionary'
import type { Locale } from '@/shared/i18n/config'
import { getMercadoLivreRedirectUri } from '../../lib/mercado-livre-oauth'

const GATEWAY_URL =
  process.env.GATEWAY_URL ??
  process.env.NEXT_PUBLIC_GATEWAY_URL ??
  'http://localhost:8080'

interface Props {
  params: Promise<{ lang: Locale }>
  searchParams: Promise<{ code?: string; error?: string; state?: string }>
}

export default async function MercadoLivreCallbackPage({ params, searchParams }: Props) {
  const { lang } = await params
  const dict = await getDictionary(lang)
  const { code, error } = await searchParams

  // Erros retornados diretamente pelo Mercado Livre
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

  // O redirect_uri enviado ao backend deve ser idêntico ao usado na autorização OAuth
  const redirectUri = getMercadoLivreRedirectUri(lang)

  // Fazemos o fetch fora do try para manter o resultado acessível após o bloco
  // O redirect() NÃO pode ficar dentro de try/catch pois lança NEXT_REDIRECT internamente
  let ok = false
  let errorMsg = dict.connectors.callback.authError
  let expired = false

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
      expired = true
    } else if (!res.ok) {
      const body = await res.json().catch(() => ({}))
      errorMsg = body.message ?? dict.connectors.callback.authError
    } else {
      ok = true
    }
  } catch {
    errorMsg = dict.connectors.callback.serviceUnavailable
  }

  // Todos os redirects ficam FORA do try/catch
  if (expired) {
    redirect(`/${lang}/login?expired=1`)
  }

  if (!ok) {
    redirect(`/${lang}/conectores?auth_error=${encodeURIComponent(errorMsg)}`)
  }

  redirect(`/${lang}/conectores?connected=mercado-livre`)
}
