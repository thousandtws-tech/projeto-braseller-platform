import { Bell, BellRing, CheckCheck, Settings2 } from 'lucide-react'

import { Badge } from '@/shared/ui/badge'
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/ui/card'
import { getToken, getSession } from '@/entities/session/server/session'
import { getNotifications, getNotificationPreferences } from '@/shared/api/gateway'
import { getDictionary } from '@/shared/i18n/get-dictionary'
import { formatMessage } from '@/shared/i18n/format'
import type { Locale } from '@/shared/i18n/config'
import { NotificationList } from './notification-list'
import { NotificationPrefsForm } from './notification-prefs-form'

interface Props { params: Promise<{ lang: Locale }> }

export async function generateMetadata({ params }: Props) {
  const { lang } = await params
  const dict = await getDictionary(lang)
  return { title: dict.notifications.title }
}

export default async function NotificacoesPage({ params }: Props) {
  const { lang } = await params
  const [dict, token, session] = await Promise.all([getDictionary(lang), getToken().then((value) => value ?? ''), getSession()])
  const tenantId = session?.tenantId ?? ''
  const [notifications, prefs] = await Promise.all([getNotifications(token, tenantId), getNotificationPreferences(token, tenantId)])
  const unread = notifications.filter((notification) => notification.status === 'UNREAD')
  const read = notifications.filter((notification) => notification.status === 'READ')

  return (
    <div className="flex w-full flex-col gap-6">
      <header>
        <h2 className="text-[1.8rem] font-semibold tracking-[-0.04em]">{dict.notifications.title}</h2>
        <p className="mt-1 text-sm text-muted-foreground">
          {unread.length > 0 ? formatMessage(unread.length === 1 ? dict.notifications.unreadOne : dict.notifications.unreadMany, { count: unread.length }) : dict.notifications.allCaughtUp}
        </p>
      </header>

      <section className="grid grid-cols-3 overflow-hidden rounded-lg border border-border bg-card">
        <Metric label="Não lidas" value={unread.length} icon={BellRing} />
        <Metric label="Anteriores" value={read.length} icon={CheckCheck} />
        <Metric label="Total" value={notifications.length} icon={Bell} />
      </section>

      <div className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_400px]">
        <section>
          <div className="mb-3"><h3 className="text-sm font-semibold">Caixa de entrada</h3><p className="mt-1 text-xs text-muted-foreground">Alertas operacionais e financeiros em ordem de prioridade.</p></div>
          <NotificationList unread={unread} read={read} dict={dict} lang={lang} />
        </section>
        <Card className="h-fit xl:sticky xl:top-6">
          <CardHeader className="flex-row items-center justify-between gap-4"><div><CardTitle>{dict.notifications.preferences.title}</CardTitle><p className="mt-1 text-xs text-muted-foreground">{dict.notifications.preferences.subtitle}</p></div><Settings2 className="size-4 text-muted-foreground" /></CardHeader>
          <CardContent><NotificationPrefsForm prefs={prefs} dict={dict} /></CardContent>
        </Card>
      </div>
    </div>
  )
}

function Metric({ label, value, icon: Icon }: { label: string; value: number; icon: React.ComponentType<{ className?: string }> }) {
  return <div className="flex min-h-24 flex-col justify-between gap-3 border-r border-border p-5 last:border-r-0"><div className="flex items-center justify-between"><span className="text-xs text-muted-foreground">{label}</span><Icon className="size-4 text-muted-foreground" /></div><div className="flex items-end justify-between"><p className="text-2xl font-semibold">{value}</p>{value > 0 ? <Badge variant="secondary">Atualizado</Badge> : null}</div></div>
}
