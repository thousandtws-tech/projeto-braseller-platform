import Link from 'next/link'
import { AlertCircle } from 'lucide-react'
import { LoginForm } from '@/features/auth'
import { getDictionary } from '@/shared/i18n/get-dictionary'
import type { Dictionary } from '@/shared/i18n/get-dictionary'
import type { Locale } from '@/shared/i18n/config'

interface Props {
  params: Promise<{ lang: Locale }>
  searchParams: Promise<{ expired?: string; error?: string }>
}

export async function generateMetadata({ params }: Props) {
  const { lang } = await params
  const dict = await getDictionary(lang)
  return { title: dict.auth.login.title }
}

export default async function LoginPage({ params, searchParams }: Props) {
  const { lang } = await params
  const { expired, error } = await searchParams
  const dict = await getDictionary(lang)
  const oauthError = googleAuthErrorMessage(error, dict.auth.login.errors)

  return (
    <div className="space-y-8">
      <div className="animate-element mb-2 flex items-center justify-center gap-2 lg:hidden">
        <div className="flex size-8 items-center justify-center rounded-lg bg-primary">
          <span className="font-bold text-primary-foreground">B</span>
        </div>
        <span className="text-lg font-semibold">Brasaller</span>
      </div>

      <div className="animate-element animate-delay-100 space-y-2">
        <h1 className="text-2xl font-semibold tracking-tight">{dict.auth.login.title}</h1>
        <p className="text-sm text-muted-foreground">
          {dict.auth.login.subtitle}
        </p>
      </div>

      {expired === '1' && (
        <div className="animate-element flex items-start gap-2.5 rounded-lg border border-amber-500/30 bg-amber-500/8 px-3.5 py-3 text-sm text-amber-700 dark:text-amber-400">
          <AlertCircle className="mt-0.5 size-4 shrink-0" />
          <span>{dict.auth.login.sessionExpired}</span>
        </div>
      )}

      {oauthError && (
        <div className="animate-element flex items-start gap-2.5 rounded-lg border border-destructive/30 bg-destructive/8 px-3.5 py-3 text-sm text-destructive">
          <AlertCircle className="mt-0.5 size-4 shrink-0" />
          <span>{oauthError}</span>
        </div>
      )}

      <LoginForm dict={dict} />

      <p className="animate-element animate-delay-900 text-center text-sm text-muted-foreground">
        {dict.auth.login.noAccount}{' '}
        <Link href={`/${lang}/register`} className="font-medium text-primary hover:underline">
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
