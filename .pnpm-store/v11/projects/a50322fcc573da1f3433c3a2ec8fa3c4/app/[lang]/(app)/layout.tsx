import { notFound, redirect } from 'next/navigation'
import { Header, Sidebar } from '@/widgets/app-shell'
import { getSession, getToken } from '@/entities/session/server/session'
import { getNotifications } from '@/shared/api/gateway'
import { getDictionary } from '@/shared/i18n/get-dictionary'
import { isLocale } from '@/shared/i18n/config'

interface Props {
  children: React.ReactNode
  params: Promise<{ lang: string }>
}

export default async function AppLayout({ children, params }: Props) {
  const { lang } = await params
  if (!isLocale(lang)) notFound()
  const session = await getSession()
  if (!session) redirect(`/${lang}/login`)

  const token = (await getToken()) ?? ''
  const [notifications, dict] = await Promise.all([
    getNotifications(token, session.tenantId),
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
