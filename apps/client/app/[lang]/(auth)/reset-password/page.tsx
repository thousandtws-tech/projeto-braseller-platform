import { ResetPasswordForm } from '@/features/auth'
import type { Locale } from '@/shared/i18n/config'

interface Props {
  params: Promise<{ lang: Locale }>
  searchParams: Promise<{ email?: string }>
}

export async function generateMetadata() {
  return { title: 'Redefinir senha' }
}

export default async function ResetPasswordPage({ params, searchParams }: Props) {
  const { lang } = await params
  const { email } = await searchParams

  return (
    <div className="space-y-8">
      <div className="animate-element mb-2 flex items-center justify-center gap-2 lg:hidden">
        <div className="flex size-8 items-center justify-center rounded-lg bg-primary">
          <span className="font-bold text-primary-foreground">B</span>
        </div>
        <span className="text-lg font-semibold">Brasaller</span>
      </div>

      <div className="animate-element animate-delay-100 space-y-2">
        <h1 className="text-2xl font-semibold tracking-tight">Redefinir senha</h1>
        <p className="text-sm text-muted-foreground">
          Digite o codigo recebido por e-mail e escolha uma nova senha.
        </p>
      </div>

      <ResetPasswordForm lang={lang} email={email} />
    </div>
  )
}
