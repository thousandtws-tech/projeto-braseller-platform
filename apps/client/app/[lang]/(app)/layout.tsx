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
  const notifications = await getNotifications(token, session.tenantId)
  const dict = await getDictionary(lang)

  return (
    <div className="flex h-screen overflow-hidden">
      <Sidebar user={session} dict={dict} lang={lang} />
      <div className="flex flex-1 flex-col overflow-hidden">
        <Header notifications={notifications} user={session} dict={dict} lang={lang} />
        <main className="flex-1 overflow-y-auto p-6">
          {children}
        </main>
      </div>
    </div>
  )
}
