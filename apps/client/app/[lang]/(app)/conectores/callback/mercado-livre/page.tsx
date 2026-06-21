/**
 * Rota de callback OAuth do Mercado Livre.
 *
 * O Mercado Livre redireciona para:
 *   /[lang]/conectores/callback/mercado-livre?code=...&state=[lang]
 *
 * Esta página recebe o `code`, chama o backend para trocar pelo token,
 * e redireciona para /[lang]/conectores com o resultado.
 */
export { default } from '@/features/connectors/ui/callback/connectors-callback-page'
