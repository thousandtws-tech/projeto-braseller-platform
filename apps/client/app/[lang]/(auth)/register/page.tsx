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
    <div className="space-y-8">
      <div className="animate-element mb-2 flex items-center justify-center gap-2 lg:hidden">
        <div className="size-8 rounded-lg bg-primary flex items-center justify-center">
          <span className="text-primary-foreground font-bold">B</span>
        </div>
        <span className="font-semibold text-lg">Brasaller</span>
      </div>

      <div className="animate-element space-y-2">
        <h1 className="text-2xl font-semibold tracking-tight">{dict.auth.register.title}</h1>
        <p className="text-sm text-muted-foreground">
          {dict.auth.register.subtitle}
        </p>
      </div>

      <RegisterForm dict={dict} />

      <p className="animate-element animate-delay-800 text-center text-sm text-muted-foreground">
        {dict.auth.register.hasAccount}{' '}
        <Link href={`/${lang}/login`} className="text-primary hover:underline font-medium">
          {dict.auth.register.signIn}
        </Link>
      </p>
    </div>
  )
}
