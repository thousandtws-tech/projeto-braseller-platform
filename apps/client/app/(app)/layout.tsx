import { redirect } from 'next/navigation'
import { Sidebar } from '@/components/layout/sidebar'
import { Header } from '@/components/layout/header'
import { getSession } from '@/lib/auth'
import { getToken } from '@/lib/auth'
import { getNotifications } from '@/lib/api'

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
