import type { Locale } from '@/shared/i18n/config'

const MERCADO_LIVRE_CLIENT_ID = '4587994283757685'
const MERCADO_LIVRE_AUTH_URL = 'https://auth.mercadolivre.com.br/authorization'

/**
 * Monta o redirect_uri que aponta para a rota de callback do Next.js.
 * Deve ser idêntico ao enviado na etapa de autenticação com o backend.
 */
export function getMercadoLivreRedirectUri(lang: string): string {
  const base =
    process.env.NEXT_PUBLIC_APP_URL ??
    (typeof window !== 'undefined' ? window.location.origin : 'http://localhost:3000')
  return `${base}/${lang}/conectores/callback/mercado-livre`
}

/**
 * Gera a URL de autorização do Mercado Livre com o redirect_uri correto.
 * O `state` carrega o locale para que a rota de callback saiba para onde redirecionar.
 */
export function mercadoLivreOAuthUrl(lang?: Locale): string {
  const locale = lang ?? 'pt-BR'
  const url = new URL(MERCADO_LIVRE_AUTH_URL)
  url.searchParams.set('response_type', 'code')
  url.searchParams.set('client_id', MERCADO_LIVRE_CLIENT_ID)
  url.searchParams.set('redirect_uri', getMercadoLivreRedirectUri(locale))
  url.searchParams.set('state', locale)
  return url.toString()
}
