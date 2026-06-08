import type { Metadata } from 'next'
import { User, Shield, Building2, Key, UserRound } from 'lucide-react'
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/ui/card'
import { Badge } from '@/shared/ui/badge'
import { Button } from '@/shared/ui/button'
import { Avatar, AvatarImage, AvatarFallback } from '@/shared/ui/avatar'
import { getToken, getSession } from '@/entities/session/server/session'
import { getCurrentUser, getFiscalProfile } from '@/shared/api/gateway'
import { isReadOnlyAccountant } from '@/entities/session/model/permissions'
import { ReadOnlyLock } from '@/shared/ui/read-only-lock'
import { FiscalProfileForm } from './fiscal-profile-form'

export const metadata: Metadata = { title: 'Configurações' }

export default async function ConfiguracoesPage() {
  const token = (await getToken()) ?? ''
  const session = await getSession()
  if (!session) return null
  const readOnly = isReadOnlyAccountant(session.roles)

  const [profile, fiscalProfile] = await Promise.all([
    getCurrentUser(token, session.tenantId, session.email),
    getFiscalProfile(token, session.tenantId),
  ])

  // Fallback: usa os dados do JWT quando a API não retornar o perfil
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
    <div className="space-y-6 max-w-3xl">
      <h2 className="text-xl font-semibold">Configurações</h2>

      {/* Profile */}
      <Card>
        <CardHeader className="flex-row items-center gap-3 pb-3">
          <div className="size-8 rounded-lg bg-primary/10 flex items-center justify-center">
            <User className="size-4 text-primary" />
          </div>
          <CardTitle>Perfil</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="flex items-center gap-4">
            <Avatar className="size-16">
              <AvatarImage src={user.pictureUrl} alt={user.fullName || user.email} />
              <AvatarFallback className="bg-muted">
                <UserRound className="size-8 text-muted-foreground" />
              </AvatarFallback>
            </Avatar>
            <div>
              <p className="font-semibold">{user.fullName || user.preferredUsername || user.email}</p>
              <p className="text-sm text-muted-foreground">{user.email}</p>
              <div className="flex gap-2 mt-1">
                {user.roles.map((r) => (
                  <Badge key={r} variant="secondary" className="text-xs">{r}</Badge>
                ))}
              </div>
            </div>
          </div>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 pt-2">
            <div className="space-y-1.5">
              <label className="text-xs font-medium text-muted-foreground uppercase">Nome completo</label>
              <input
                defaultValue={user.fullName || `${user.firstName ?? ''} ${user.lastName ?? ''}`.trim()}
                disabled={readOnly}
                className="flex h-8 w-full rounded-lg border border-input bg-transparent px-3 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
              />
            </div>
            <div className="space-y-1.5">
              <label className="text-xs font-medium text-muted-foreground uppercase">E-mail</label>
              <input
                defaultValue={user.email}
                type="email"
                disabled={readOnly}
                className="flex h-8 w-full rounded-lg border border-input bg-transparent px-3 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
              />
            </div>
          </div>
          {readOnly && <ReadOnlyLock />}
          <Button size="sm" disabled={readOnly}>Salvar alterações</Button>
        </CardContent>
      </Card>

      {/* Fiscal Profile */}
      <Card>
        <CardHeader className="flex-row items-center gap-3 pb-3">
          <div className="size-8 rounded-lg bg-primary/10 flex items-center justify-center">
            <Building2 className="size-4 text-primary" />
          </div>
          <CardTitle>Perfil Fiscal</CardTitle>
        </CardHeader>
        <CardContent>
          <FiscalProfileForm profile={fiscalProfile} readOnly={readOnly} />
        </CardContent>
      </Card>

      {/* Security */}
      <Card>
        <CardHeader className="flex-row items-center gap-3 pb-3">
          <div className="size-8 rounded-lg bg-primary/10 flex items-center justify-center">
            <Shield className="size-4 text-primary" />
          </div>
          <CardTitle>Segurança</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div className="space-y-1.5">
              <label className="text-xs font-medium text-muted-foreground uppercase">Nova senha</label>
              <input
                type="password"
                placeholder="••••••••"
                disabled={readOnly}
                className="flex h-8 w-full rounded-lg border border-input bg-transparent px-3 text-sm placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
              />
            </div>
            <div className="space-y-1.5">
              <label className="text-xs font-medium text-muted-foreground uppercase">Confirmar senha</label>
              <input
                type="password"
                placeholder="••••••••"
                disabled={readOnly}
                className="flex h-8 w-full rounded-lg border border-input bg-transparent px-3 text-sm placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
              />
            </div>
          </div>
          {readOnly && <ReadOnlyLock />}
          <Button size="sm" variant="outline" disabled={readOnly}>
            <Key className="size-3.5 mr-1.5" />
            Alterar senha
          </Button>
        </CardContent>
      </Card>
    </div>
  )
}
