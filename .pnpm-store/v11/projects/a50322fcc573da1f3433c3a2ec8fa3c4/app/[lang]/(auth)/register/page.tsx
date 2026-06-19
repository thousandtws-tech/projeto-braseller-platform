import Link from 'next/link'
import { RegisterForm } from '@/features/auth'
import { getDictionary } from '@/shared/i18n/get-dictionary'
import type { Locale } from '@/shared/i18n/config'

interface Props {
  params: Promise<{ lang: Locale }>
}

export async function generateMetadata({ params }: Props) {
  const { lang } = await params
  const dict = await getDictionary(lang)
  return { title: dict.auth.register.title }
}

export default async function RegisterPage({ params }: Props) {
  const { lang } = await params
  const dict = await getDictionary(lang)

  return (
    <div className="flex flex-col gap-8">
      <div className="flex items-center gap-2 lg:hidden">
        <div className="flex size-8 items-center justify-center rounded-md bg-primary">
          <span className="text-primary-foreground font-bold">B</span>
        </div>
        <span className="font-semibold text-lg">Brasaller</span>
      </div>

      <div className="flex flex-col gap-2">
        <p className="text-xs font-medium uppercase tracking-[0.14em] text-muted-foreground">Comece agora</p>
        <h1 className="text-3xl font-semibold tracking-[-0.04em]">{dict.auth.register.title}</h1>
        <p className="text-sm leading-6 text-muted-foreground">
          {dict.auth.register.subtitle}
        </p>
      </div>

      <RegisterForm />

      <p className="border-t border-border pt-6 text-center text-sm text-muted-foreground">
        {dict.auth.register.hasAccount}{' '}
        <Link href={`/${lang}/login`} className="font-medium text-foreground underline-offset-4 hover:underline">
          {dict.auth.register.signIn}
        </Link>
      </p>
    </div>
  )
}
