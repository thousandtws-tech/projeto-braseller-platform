import type { Metadata } from 'next'
import { AlertCircle } from 'lucide-react'
import { LoginForm } from '@/features/auth'

export const metadata: Metadata = { title: 'Entrar' }

interface Props {
  searchParams: Promise<{ expired?: string; error?: string }>
}

export default async function LoginPage({ searchParams }: Props) {
  const { expired, error } = await searchParams
  const oauthError = googleAuthErrorMessage(error)

  return (
    <div className="space-y-8">
      <div className="mb-2 flex items-center justify-center gap-2 lg:hidden">
        <div className="flex size-8 items-center justify-center rounded-lg bg-primary">
          <span className="font-bold text-primary-foreground">B</span>
        </div>
        <span className="text-lg font-semibold">Brasaller</span>
      </div>

      <div className="space-y-2">
        <h1 className="text-2xl font-semibold tracking-tight">Bem-vindo de volta</h1>
        <p className="text-sm text-muted-foreground">
          Acesse sua conta para gerenciar suas vendas
        </p>
      </div>

      {expired === '1' && (
        <div className="flex items-start gap-2.5 rounded-lg border border-amber-500/30 bg-amber-500/8 px-3.5 py-3 text-sm text-amber-700 dark:text-amber-400">
          <AlertCircle className="mt-0.5 size-4 shrink-0" />
          <span>Sua sessao expirou. Faca login novamente para continuar.</span>
        </div>
      )}

      {oauthError && (
        <div className="flex items-start gap-2.5 rounded-lg border border-destructive/30 bg-destructive/8 px-3.5 py-3 text-sm text-destructive">
          <AlertCircle className="mt-0.5 size-4 shrink-0" />
          <span>{oauthError}</span>
        </div>
      )}

      <LoginForm />

      <p className="text-center text-sm text-muted-foreground">
        Nao tem uma conta?{' '}
        <a href="/register" className="font-medium text-primary hover:underline">
          Criar conta
        </a>
      </p>
    </div>
  )
}

function googleAuthErrorMessage(error?: string) {
  if (error === 'google_unavailable') {
    return 'Login com Google indisponivel no momento.'
  }
  if (error === 'google_account_not_registered') {
    return 'Essa conta Google ainda nao esta cadastrada. Crie a conta primeiro ou entre com e-mail e senha.'
  }
  if (error === 'oauth_failed') {
    return 'Nao foi possivel concluir o login com Google.'
  }
  return null
}
