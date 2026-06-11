import { redirect } from 'next/navigation'
import { Header, Sidebar } from '@/widgets/app-shell'
import { getSession, getToken } from '@/entities/session/server/session'
import { getNotifications } from '@/shared/api/gateway'

export default async function AppLayout({ children }: { children: React.ReactNode }) {
  const session = await getSession()
  if (!session) redirect('/login')

  const token = (await getToken()) ?? ''
  const notifications = await getNotifications(token, session.tenantId)

  return (
    <div className="flex h-screen overflow-hidden">
      <Sidebar user={session} />
      <div className="flex flex-1 flex-col overflow-hidden">
        <Header notifications={notifications} user={session} />
        <main className="flex-1 overflow-y-auto p-6">
          {children}
        </main>
      </div>
    </div>
  )
}
