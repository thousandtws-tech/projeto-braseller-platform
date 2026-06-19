import { Building2, Key, Shield, UserRound } from 'lucide-react'

import { Avatar, AvatarFallback, AvatarImage } from '@/shared/ui/avatar'
import { Badge } from '@/shared/ui/badge'
import { Button } from '@/shared/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/ui/card'
import { Input } from '@/shared/ui/input'
import { Label } from '@/shared/ui/label'
import { getToken, getSession } from '@/entities/session/server/session'
import { getCurrentUser, getFiscalProfile } from '@/shared/api/gateway'
import { isReadOnlyAccountant } from '@/entities/session/model/permissions'
import { ReadOnlyLock } from '@/shared/ui/read-only-lock'
import { getDictionary } from '@/shared/i18n/get-dictionary'
import type { Locale } from '@/shared/i18n/config'
import { FiscalProfileForm } from './fiscal-profile-form'

interface PageProps { params: Promise<{ lang: Locale }> }

export async function generateMetadata({ params }: PageProps) {
  const { lang } = await params
  const dict = await getDictionary(lang)
  return { title: dict.settings.title }
}

export default async function ConfiguracoesPage({ params }: PageProps) {
  const { lang } = await params
  const [dict, token, session] = await Promise.all([getDictionary(lang), getToken().then((value) => value ?? ''), getSession()])
  if (!session) return null
  const readOnly = isReadOnlyAccountant(session.roles)
  const [profile, fiscalProfile] = await Promise.all([
    getCurrentUser(token, session.tenantId, session.email),
    getFiscalProfile(token, session.tenantId),
  ])
  const user = profile ?? {
    ...session,
    id: session.userId,
    fullName: session.fullName,
    firstName: '',
    lastName: '',
    preferredUsername: session.email,
    pictureUrl: undefined,
    emailVerified: true,
    provider: '',
    providerSubject: '',
    status: 'ACTIVE',
  }

  return (
    <div className="flex w-full flex-col gap-6">
      <header>
        <h2 className="text-[1.8rem] font-semibold tracking-[-0.04em]">{dict.settings.title}</h2>
        <p className="mt-1 text-sm text-muted-foreground">Gerencie identidade, dados fiscais e segurança da conta.</p>
      </header>

      <section className="grid grid-cols-3 overflow-hidden rounded-lg border border-border bg-card">
        <Summary label="Perfil" value={user.emailVerified ? 'Verificado' : 'Pendente'} icon={UserRound} />
        <Summary label="Fiscal" value={fiscalProfile?.tax_regime ? 'Configurado' : 'Pendente'} icon={Building2} />
        <Summary label="Acesso" value={readOnly ? 'Somente leitura' : 'Administrador'} icon={Shield} />
      </section>

      <div className="grid gap-6 xl:grid-cols-[240px_minmax(0,1fr)]">
        <aside className="h-fit rounded-lg border border-border bg-card p-2 xl:sticky xl:top-6">
          <SettingsAnchor href="#perfil" icon={UserRound} label={dict.settings.profile.title} />
          <SettingsAnchor href="#fiscal" icon={Building2} label={dict.settings.fiscalProfile.title} />
          <SettingsAnchor href="#seguranca" icon={Shield} label={dict.settings.security.title} />
        </aside>

        <div className="flex min-w-0 flex-col gap-6">
          <Card id="perfil">
            <CardHeader><CardTitle>{dict.settings.profile.title}</CardTitle><p className="text-xs text-muted-foreground">Informações usadas para identificar sua conta.</p></CardHeader>
            <CardContent className="flex flex-col gap-6">
              <div className="flex items-center gap-4">
                <Avatar className="size-14"><AvatarImage src={user.pictureUrl} alt={user.fullName || user.email} /><AvatarFallback className="bg-foreground text-background"><UserRound className="size-6" /></AvatarFallback></Avatar>
                <div className="min-w-0"><p className="truncate font-semibold">{user.fullName || user.preferredUsername || user.email}</p><p className="truncate text-sm text-muted-foreground">{user.email}</p><div className="mt-2 flex flex-wrap gap-2">{user.roles.map((role) => <Badge key={role} variant="secondary">{role}</Badge>)}</div></div>
              </div>
              <div className="grid gap-4 sm:grid-cols-2">
                <div className="flex flex-col gap-2"><Label htmlFor="settings-name">{dict.settings.profile.fullName}</Label><Input id="settings-name" defaultValue={user.fullName || `${user.firstName ?? ''} ${user.lastName ?? ''}`.trim()} disabled={readOnly} /></div>
                <div className="flex flex-col gap-2"><Label htmlFor="settings-email">{dict.settings.profile.email}</Label><Input id="settings-email" defaultValue={user.email} type="email" disabled={readOnly} /></div>
              </div>
              {readOnly ? <ReadOnlyLock /> : null}
              <Button size="lg" className="w-full sm:w-fit" disabled={readOnly}>{dict.common.saveChanges}</Button>
            </CardContent>
          </Card>

          <Card id="fiscal">
            <CardHeader><CardTitle>{dict.settings.fiscalProfile.title}</CardTitle><p className="text-xs text-muted-foreground">Dados usados na estimativa tributária e nos demonstrativos.</p></CardHeader>
            <CardContent><FiscalProfileForm profile={fiscalProfile} readOnly={readOnly} /></CardContent>
          </Card>

          <Card id="seguranca">
            <CardHeader><CardTitle>{dict.settings.security.title}</CardTitle><p className="text-xs text-muted-foreground">Atualize sua senha de acesso com segurança.</p></CardHeader>
            <CardContent className="flex flex-col gap-4">
              <div className="grid gap-4 sm:grid-cols-2">
                <div className="flex flex-col gap-2"><Label htmlFor="new-password">{dict.settings.security.newPassword}</Label><Input id="new-password" type="password" placeholder="••••••••" disabled={readOnly} /></div>
                <div className="flex flex-col gap-2"><Label htmlFor="confirm-password">{dict.settings.security.confirmPassword}</Label><Input id="confirm-password" type="password" placeholder="••••••••" disabled={readOnly} /></div>
              </div>
              {readOnly ? <ReadOnlyLock /> : null}
              <Button size="lg" variant="outline" className="w-full sm:w-fit" disabled={readOnly}><Key />{dict.settings.security.changePassword}</Button>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  )
}

function Summary({ label, value, icon: Icon }: { label: string; value: string; icon: React.ComponentType<{ className?: string }> }) {
  return <div className="flex min-h-24 flex-col justify-between gap-3 border-r border-border p-5 last:border-r-0"><div className="flex items-center justify-between"><span className="text-xs text-muted-foreground">{label}</span><Icon className="size-4 text-muted-foreground" /></div><p className="text-lg font-semibold">{value}</p></div>
}

function SettingsAnchor({ href, icon: Icon, label }: { href: string; icon: React.ComponentType<{ className?: string }>; label: string }) {
  return <a href={href} className="flex items-center gap-3 rounded-md px-3 py-2.5 text-sm text-muted-foreground transition hover:bg-muted hover:text-foreground"><Icon className="size-4" />{label}</a>
}
