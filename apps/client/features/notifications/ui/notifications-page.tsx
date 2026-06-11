import { Settings2 } from 'lucide-react'
import { Card, CardContent } from '@/shared/ui/card'
import { Separator } from '@/shared/ui/separator'
import { getToken, getSession } from '@/entities/session/server/session'
import { getNotifications, getNotificationPreferences } from '@/shared/api/gateway'
import { getDictionary } from '@/shared/i18n/get-dictionary'
import { formatMessage } from '@/shared/i18n/format'
import type { Locale } from '@/shared/i18n/config'
import { NotificationList } from './notification-list'
import { NotificationPrefsForm } from './notification-prefs-form'

interface Props {
  params: Promise<{ lang: Locale }>
}

export async function generateMetadata({ params }: Props) {
  const { lang } = await params
  const dict = await getDictionary(lang)
  return { title: dict.notifications.title }
}

export default async function NotificacoesPage({ params }: Props) {
  const { lang } = await params
  const dict = await getDictionary(lang)
  const token = (await getToken()) ?? ''
  const session = await getSession()
  const tenantId = session?.tenantId ?? ''

  const [notifications, prefs] = await Promise.all([
    getNotifications(token, tenantId),
    getNotificationPreferences(token, tenantId),
  ])

  const unread = notifications.filter((n) => n.status === 'UNREAD')
  const read   = notifications.filter((n) => n.status === 'READ')

  return (
    <div className="flex justify-center px-4 py-2">
      <div className="w-full max-w-2xl space-y-8">

        {/* Header */}
        <div className="flex items-start justify-between">
          <div>
            <h2 className="text-2xl font-semibold tracking-tight">{dict.notifications.title}</h2>
            <p className="text-sm text-muted-foreground mt-1">
              {unread.length > 0
                ? formatMessage(unread.length === 1 ? dict.notifications.unreadOne : dict.notifications.unreadMany, { count: unread.length })
                : dict.notifications.allCaughtUp}
            </p>
          </div>
          {unread.length > 0 && (
            <span className="inline-flex items-center justify-center size-8 rounded-full bg-primary text-primary-foreground text-sm font-semibold">
              {unread.length > 9 ? '9+' : unread.length}
            </span>
          )}
        </div>

        {/* Notification list */}
        <NotificationList unread={unread} read={read} dict={dict} lang={lang} />

        <Separator />

        {/* Preferences */}
        <div>
          <div className="flex items-center gap-2 mb-6">
            <div className="size-8 rounded-lg bg-primary/10 flex items-center justify-center">
              <Settings2 className="size-4 text-primary" />
            </div>
            <div>
              <h3 className="text-base font-semibold">{dict.notifications.preferences.title}</h3>
              <p className="text-xs text-muted-foreground">{dict.notifications.preferences.subtitle}</p>
            </div>
          </div>
          <Card>
            <CardContent className="pt-6">
              <NotificationPrefsForm prefs={prefs} dict={dict} />
            </CardContent>
          </Card>
        </div>

      </div>
    </div>
  )
}
