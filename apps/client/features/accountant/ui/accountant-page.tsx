import { Calculator, Info, UserRound } from 'lucide-react'
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/ui/card'
import { Badge } from '@/shared/ui/badge'
import { Avatar, AvatarImage, AvatarFallback } from '@/shared/ui/avatar'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/shared/ui/table'
import { getToken, getSession } from '@/entities/session/server/session'
import { getAccountants } from '@/shared/api/gateway'
import { isReadOnlyAccountant } from '@/entities/session/model/permissions'
import { getDictionary } from '@/shared/i18n/get-dictionary'
import type { Locale } from '@/shared/i18n/config'
import { AccountantForm } from './accountant-form'

interface Props {
  params: Promise<{ lang: Locale }>
}

export async function generateMetadata({ params }: Props) {
  const { lang } = await params
  const dict = await getDictionary(lang)
  return { title: dict.accountant.title }
}

const STATUS_VARIANT: Record<string, 'success' | 'secondary' | 'warning' | 'destructive'> = {
  ACTIVE: 'success',
  INACTIVE: 'secondary',
  PENDING: 'warning',
}

export default async function ContadorPage({ params }: Props) {
  const { lang } = await params
  const dict = await getDictionary(lang)
  const token = (await getToken()) ?? ''
  const session = await getSession()
  if (!session) return null
  const readOnly = isReadOnlyAccountant(session.roles)

  const accountants = await getAccountants(token, session.tenantId)

  const statusLabels = dict.accountant.status as Record<string, string>

  return (
    <div className="space-y-6 max-w-7xl">
      <div className="space-y-1">
        <h2 className="text-xl font-semibold">{dict.accountant.title}</h2>
        <p className="text-sm text-muted-foreground">
          {dict.accountant.subtitle}
        </p>
      </div>

  {/* Formulário de concessão */}
      <Card>
        <CardHeader className="flex-row items-center gap-3 pb-3">
          <div className="size-8 rounded-lg bg-primary/10 flex items-center justify-center">
            <Calculator className="size-4 text-primary" />
          </div>
          <CardTitle>{dict.accountant.grantForm.title}</CardTitle>
        </CardHeader>
        <CardContent>
          <AccountantForm readOnly={readOnly} dict={dict} />
        </CardContent>
      </Card>
      {/* Tabela de contadores ativos */}
      <Card>
        <CardHeader className="flex-row items-center gap-3 pb-3">
          <div className="size-8 rounded-lg bg-primary/10 flex items-center justify-center">
            <Calculator className="size-4 text-primary" />
          </div>
          <CardTitle>{dict.accountant.table.title}</CardTitle>
        </CardHeader>
        <CardContent className="p-0">
          {accountants.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-12 text-center px-6">
              <div className="size-12 rounded-full bg-muted flex items-center justify-center mb-3">
                <UserRound className="size-6 text-muted-foreground" />
              </div>
              <p className="text-sm font-medium">{dict.accountant.table.empty.title}</p>
              <p className="text-xs text-muted-foreground mt-1">
                {dict.accountant.table.empty.hint}
              </p>
            </div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead className="pl-6">{dict.accountant.table.columns.name}</TableHead>
                  <TableHead>{dict.accountant.table.columns.email}</TableHead>
<TableHead>{dict.accountant.table.columns.status}</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {accountants.map((accountant) => {
                  const displayName =
                    accountant.fullName ||
                    `${accountant.firstName ?? ''} ${accountant.lastName ?? ''}`.trim() ||
                    accountant.email

                  const initials = displayName
                    .split(' ')
                    .slice(0, 2)
                    .map((n) => n[0])
                    .join('')
                    .toUpperCase()

                  return (
                    <TableRow key={accountant.id}>
                      <TableCell className="pl-6">
                        <div className="flex items-center gap-3">
                          <Avatar className="size-8">
                            <AvatarImage src={accountant.pictureUrl} alt={displayName} />
                            <AvatarFallback className="bg-muted text-xs font-medium">
                              {initials || <UserRound className="size-4 text-muted-foreground" />}
                            </AvatarFallback>
                          </Avatar>
                          <div className="min-w-0">
                            <p className="text-sm font-medium truncate">{displayName}</p>
                            {accountant.preferredUsername && accountant.preferredUsername !== accountant.email && (
                              <p className="text-xs text-muted-foreground truncate">
                                @{accountant.preferredUsername}
                              </p>
                            )}
                          </div>
                        </div>
                      </TableCell>
                      <TableCell className="text-muted-foreground">
                        {accountant.email}
                      </TableCell>
<TableCell>
                        <Badge
                          variant={STATUS_VARIANT[accountant.status] ?? 'secondary'}
                          className="text-xs"
                        >
                          {statusLabels[accountant.status] ?? accountant.status}
                        </Badge>
                      </TableCell>
                    </TableRow>
                  )
                })}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>



      <div className="flex items-start gap-2.5 rounded-lg border border-border bg-muted/40 px-4 py-3 text-sm text-muted-foreground">
        <Info className="size-4 mt-0.5 shrink-0" />
        <p>
          {dict.accountant.accessNote.prefix}
          <strong className="text-foreground">{dict.accountant.accessNote.highlight}</strong>
          {dict.accountant.accessNote.suffix}
        </p>
      </div>
    </div>
  )
}
