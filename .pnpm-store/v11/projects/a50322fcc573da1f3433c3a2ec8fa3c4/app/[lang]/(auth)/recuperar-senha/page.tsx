import Link from 'next/link'
import { ArrowLeft } from 'lucide-react'
import { PasswordRecoveryForm } from '@/features/auth'
import type { Locale } from '@/shared/i18n/config'

export const metadata = { title: 'Recuperar senha' }

export default async function PasswordRecoveryPage({
  params,
}: {
  params: Promise<{ lang: Locale }>
}) {
  const { lang } = await params

  return (
    <div className="flex flex-col gap-8">
      <div className="flex items-center gap-2 lg:hidden">
        <div className="flex size-8 items-center justify-center rounded-md bg-primary font-bold text-primary-foreground">B</div>
        <span className="text-lg font-semibold">Brasaller</span>
      </div>
      <div className="flex flex-col gap-2">
        <p className="text-xs font-medium uppercase tracking-[0.14em] text-muted-foreground">Recuperação de acesso</p>
        <h1 className="text-3xl font-semibold tracking-[-0.04em]">Redefina sua senha</h1>
        <p className="text-sm leading-6 text-muted-foreground">
          Informe o e-mail usado no cadastro para receber as próximas instruções.
        </p>
      </div>
      <PasswordRecoveryForm />
      <Link href={`/${lang}/login`} className="flex items-center justify-center gap-2 border-t border-border pt-6 text-sm font-medium text-foreground">
        <ArrowLeft className="size-4" />
        Voltar para o login
      </Link>
    </div>
  )
}
