import type { Metadata } from 'next'
import { AlertCircle } from 'lucide-react'
import { LoginForm } from './login-form'

export const metadata: Metadata = { title: 'Entrar' }

interface Props {
  searchParams: Promise<{ expired?: string }>
}

export default async function LoginPage({ searchParams }: Props) {
  const { expired } = await searchParams

  return (
    <div className="space-y-8">
      {/* Logo mobile */}
      <div className="flex lg:hidden items-center gap-2 justify-center mb-2">
        <div className="size-8 rounded-lg bg-primary flex items-center justify-center">
          <span className="text-primary-foreground font-bold">B</span>
        </div>
        <span className="font-semibold text-lg">Brasaller</span>
      </div>

      <div className="space-y-2">
        <h1 className="text-2xl font-semibold tracking-tight">Bem-vindo de volta</h1>
        <p className="text-sm text-muted-foreground">
          Acesse sua conta para gerenciar suas vendas
        </p>
      </div>

      {expired === '1' && (
        <div className="flex items-start gap-2.5 rounded-lg border border-amber-500/30 bg-amber-500/8 px-3.5 py-3 text-sm text-amber-700 dark:text-amber-400">
          <AlertCircle className="size-4 mt-0.5 shrink-0" />
          <span>Sua sessão expirou. Faça login novamente para continuar.</span>
        </div>
      )}

      <LoginForm />

      <p className="text-center text-sm text-muted-foreground">
        Não tem uma conta?{' '}
        <a href="/register" className="text-primary hover:underline font-medium">
          Criar conta
        </a>
      </p>
    </div>
  )
}
