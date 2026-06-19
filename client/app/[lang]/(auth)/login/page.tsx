import Link from 'next/link'
import { AlertCircle } from 'lucide-react'
import { LoginForm } from '@/features/auth'
import { getDictionary } from '@/shared/i18n/get-dictionary'
import type { Dictionary } from '@/shared/i18n/get-dictionary'
import type { Locale } from '@/shared/i18n/config'

interface Props {
  params: Promise<{ lang: Locale }>
  searchParams: Promise<{ expired?: string; error?: string; verified?: string; email?: string }>
}

export async function generateMetadata({ params }: Props) {
  const { lang } = await params
  const dict = await getDictionary(lang)
  return { title: dict.auth.login.title }
}

export default async function LoginPage({ params, searchParams }: Props) {
  const { lang } = await params
  const { expired, error, verified, email } = await searchParams
  const dict = await getDictionary(lang)
  const oauthError = googleAuthErrorMessage(error, dict.auth.login.errors)

  return (
    <div className="flex flex-col gap-8">
      <div className="flex items-center gap-2 lg:hidden">
        <div className="flex size-8 items-center justify-center rounded-md bg-primary">
          <span className="font-bold text-primary-foreground">B</span>
        </div>
        <span className="text-lg font-semibold">Brasaller</span>
      </div>

      <div className="flex flex-col gap-2">
        <p className="text-xs font-medium uppercase tracking-[0.14em] text-muted-foreground">Acesso seguro</p>
        <h1 className="text-3xl font-semibold tracking-[-0.04em]">{dict.auth.login.title}</h1>
        <p className="text-sm leading-6 text-muted-foreground">
          {dict.auth.login.subtitle}
        </p>
      </div>

      {expired === '1' && (
        <div className="flex items-start gap-2.5 rounded-md border border-border bg-muted px-3.5 py-3 text-sm text-foreground">
          <AlertCircle className="mt-0.5 size-4 shrink-0" />
          <span>{dict.auth.login.sessionExpired}</span>
        </div>
      )}

      {verified === '1' && (
        <div className="flex items-start gap-2.5 rounded-md border border-emerald-500/25 bg-emerald-500/5 px-3.5 py-3 text-sm text-emerald-700">
          <AlertCircle className="mt-0.5 size-4 shrink-0" />
          <span>{dict.auth.login.emailVerified}</span>
        </div>
      )}

      {oauthError && (
        <div className="flex items-start gap-2.5 rounded-md border border-destructive/25 bg-destructive/5 px-3.5 py-3 text-sm text-destructive">
          <AlertCircle className="mt-0.5 size-4 shrink-0" />
          <span>{oauthError}</span>
        </div>
      )}

      <LoginForm initialEmail={email} />

      <p className="border-t border-border pt-6 text-center text-sm text-muted-foreground">
        {dict.auth.login.noAccount}{' '}
        <Link href={`/${lang}/register`} className="font-medium text-foreground underline-offset-4 hover:underline">
          {dict.auth.login.createAccount}
        </Link>
      </p>
    </div>
  )
}

function googleAuthErrorMessage(error: string | undefined, errors: Dictionary['auth']['login']['errors']) {
  if (error === 'google_unavailable') {
    return errors.googleUnavailable
  }
  if (error === 'google_account_not_registered') {
    return errors.googleAccountNotRegistered
  }
  if (error === 'oauth_failed') {
    return errors.oauthFailed
  }
  return null
}
