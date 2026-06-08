import type { Metadata } from 'next'
import { RegisterForm } from '@/features/auth'

export const metadata: Metadata = { title: 'Criar conta' }

export default function RegisterPage() {
  return (
    <div className="space-y-8">
      <div className="flex lg:hidden items-center gap-2 justify-center mb-2">
        <div className="size-8 rounded-lg bg-primary flex items-center justify-center">
          <span className="text-primary-foreground font-bold">B</span>
        </div>
        <span className="font-semibold text-lg">Brasaller</span>
      </div>

      <div className="space-y-2">
        <h1 className="text-2xl font-semibold tracking-tight">Criar sua conta</h1>
        <p className="text-sm text-muted-foreground">
          Configure seu tenant e comece com 14 dias de trial
        </p>
      </div>

      <RegisterForm />

      <p className="text-center text-sm text-muted-foreground">
        Já tem uma conta?{' '}
        <a href="/login" className="text-primary hover:underline font-medium">
          Entrar
        </a>
      </p>
    </div>
  )
}
