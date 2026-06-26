import Link from 'next/link'
import { AlertCircle } from 'lucide-react'
import { VerifyCodeForm } from '@/features/auth'
import { getDictionary } from '@/shared/i18n/get-dictionary'
import type { Locale } from '@/shared/i18n/config'

interface Props {
  params: Promise<{ lang: Locale }>
  searchParams: Promise<{ email?: string; registered?: string; reason?: string }>
}

export async function generateMetadata({ params }: Props) {
  const { lang } = await params
  const dict = await getDictionary(lang)
  return { title: dict.auth.verifyCode.title }
}

export default async function VerifyCodePage({ params, searchParams }: Props) {
  const { lang } = await params
  const { email, registered, reason } = await searchParams
  const dict = await getDictionary(lang)

  if (!email) {
    return (
      <div className="flex flex-col gap-8">
        <div className="flex flex-col gap-2">
          <p className="text-xs font-medium uppercase tracking-[0.14em] text-muted-foreground">Segurança da conta</p>
          <h1 className="text-3xl font-semibold tracking-[-0.04em]">{dict.auth.verifyCode.title}</h1>
          <p className="text-sm leading-6 text-muted-foreground">{dict.auth.verifyCode.missingEmail}</p>
        </div>

        <Link href={`/${lang}/register`} className="text-sm font-medium text-foreground underline-offset-4 hover:underline">
          {dict.auth.verifyCode.backToRegister}
        </Link>
      </div>
    )
  }

  return (
    <div className="flex flex-col gap-8">
      <div className="flex flex-col gap-2">
        <p className="text-xs font-medium uppercase tracking-[0.14em] text-muted-foreground">Segurança da conta</p>
        <h1 className="text-3xl font-semibold tracking-[-0.04em]">{dict.auth.verifyCode.title}</h1>
        <p className="text-sm leading-6 text-muted-foreground">
          {dict.auth.verifyCode.subtitle}
        </p>
      </div>

      {registered === '1' && (
        <div className="flex items-start gap-2.5 rounded-md border border-border bg-muted px-3.5 py-3 text-sm text-foreground">
          <AlertCircle className="mt-0.5 size-4 shrink-0" />
          <span>{dict.auth.verifyCode.registeredHint}</span>
        </div>
      )}

      {reason === 'email_verification_required' && (
        <div className="flex items-start gap-2.5 rounded-md border border-border bg-muted px-3.5 py-3 text-sm text-foreground">
          <AlertCircle className="mt-0.5 size-4 shrink-0" />
          <span>{dict.auth.verifyCode.loginBlockedHint}</span>
        </div>
      )}

      <VerifyCodeForm email={email} registered={registered === '1'} />

      <p className="border-t border-border pt-6 text-center text-sm text-muted-foreground">
        {dict.auth.verifyCode.backToLoginPrefix}{' '}
        <Link href={`/${lang}/login?email=${encodeURIComponent(email)}`} className="font-medium text-foreground underline-offset-4 hover:underline">
          {dict.auth.verifyCode.backToLogin}
        </Link>
      </p>
    </div>
  )
}
