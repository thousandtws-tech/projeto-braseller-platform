import { ForgotPasswordForm } from '@/features/auth'
import type { Locale } from '@/shared/i18n/config'

interface Props {
  params: Promise<{ lang: Locale }>
}

export async function generateMetadata() {
  return { title: 'Recuperar senha' }
}

export default async function ForgotPasswordPage({ params }: Props) {
  const { lang } = await params

  return (
    <div className="space-y-8">
      <div className="animate-element mb-2 flex items-center justify-center gap-2 lg:hidden">
        <div className="flex size-8 items-center justify-center rounded-lg bg-primary">
          <span className="font-bold text-primary-foreground">B</span>
        </div>
        <span className="text-lg font-semibold">Brasaller</span>
      </div>

      <div className="animate-element animate-delay-100 space-y-2">
        <h1 className="text-2xl font-semibold tracking-tight">Recuperar senha</h1>
        <p className="text-sm text-muted-foreground">
          Informe seu e-mail para receber um codigo de redefinicao.
        </p>
      </div>

      <ForgotPasswordForm lang={lang} />
    </div>
  )
}
