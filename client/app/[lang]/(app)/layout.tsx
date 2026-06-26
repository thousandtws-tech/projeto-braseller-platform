import { notFound, redirect } from 'next/navigation'
import { Header, Sidebar } from '@/widgets/app-shell'
import { getSessionFromToken, getToken } from '@/entities/session/server/session'
import { getNotifications } from '@/shared/api/gateway'
import { getDictionary } from '@/shared/i18n/get-dictionary'
import { isLocale } from '@/shared/i18n/config'

const NOTIFICATIONS_BOOT_TIMEOUT_MS = 1200

function withTimeout<T>(promise: Promise<T>, fallback: T, timeoutMs: number): Promise<T> {
  return Promise.race([
    promise,
    new Promise<T>((resolve) => setTimeout(() => resolve(fallback), timeoutMs)),
  ])
}

interface Props {
  children: React.ReactNode
  params: Promise<{ lang: string }>
}

export default async function AppLayout({ children, params }: Props) {
  const { lang } = await params
  if (!isLocale(lang)) notFound()
  const token = (await getToken()) ?? ''
  const session = getSessionFromToken(token)
  if (!session) redirect(`/${lang}/login`)

  const [notifications, dict] = await Promise.all([
    withTimeout(getNotifications(token, session.tenantId), [], NOTIFICATIONS_BOOT_TIMEOUT_MS),
    getDictionary(lang),
  ])

  return (
    <div className="flex h-dvh overflow-hidden bg-background">
      <Sidebar user={session} dict={dict} lang={lang} />
      <div className="flex min-w-0 flex-1 flex-col overflow-hidden">
        <Header notifications={notifications} user={session} dict={dict} lang={lang} />
        <main className="scroll-thin flex-1 overflow-y-auto">
          <div className="app-page px-4 py-5 sm:px-6 lg:px-8 lg:py-7">
            {children}
          </div>
        </main>
      </div>
    </div>
  )
}
