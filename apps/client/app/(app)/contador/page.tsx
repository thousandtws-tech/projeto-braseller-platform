import type { Metadata } from 'next'
import { Calculator, Info, UserRound } from 'lucide-react'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Avatar, AvatarImage, AvatarFallback } from '@/components/ui/avatar'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { getToken, getSession } from '@/lib/auth'
import { getAccountants } from '@/lib/api'
import { AccountantForm } from '@/app/(app)/configuracoes/accountant-form'

export const metadata: Metadata = { title: 'Contador' }

const STATUS_LABEL: Record<string, string> = {
  ACTIVE: 'Ativo',
  INACTIVE: 'Inativo',
  PENDING: 'Pendente',
}

const STATUS_VARIANT: Record<string, 'success' | 'secondary' | 'warning' | 'destructive'> = {
  ACTIVE: 'success',
  INACTIVE: 'secondary',
  PENDING: 'warning',
}

export default async function ContadorPage() {
  const token = (await getToken()) ?? ''
  const session = await getSession()
  if (!session) return null

  const accountants = await getAccountants(token, session.tenantId)

  return (
    <div className="space-y-6 max-w-7xl">
      <div className="space-y-1">
        <h2 className="text-xl font-semibold">Contador</h2>
        <p className="text-sm text-muted-foreground">
          Gerencie o acesso do contador às informações financeiras do tenant.
        </p>
      </div>

  {/* Formulário de concessão */}
      <Card>
        <CardHeader className="flex-row items-center gap-3 pb-3">
          <div className="size-8 rounded-lg bg-primary/10 flex items-center justify-center">
            <Calculator className="size-4 text-primary" />
          </div>
          <CardTitle>Conceder acesso ao contador</CardTitle>
        </CardHeader>
        <CardContent>
          <AccountantForm />
        </CardContent>
      </Card>
      {/* Tabela de contadores ativos */}
      <Card>
        <CardHeader className="flex-row items-center gap-3 pb-3">
          <div className="size-8 rounded-lg bg-primary/10 flex items-center justify-center">
            <Calculator className="size-4 text-primary" />
          </div>
          <CardTitle>Contadores com acesso</CardTitle>
        </CardHeader>
        <CardContent className="p-0">
          {accountants.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-12 text-center px-6">
              <div className="size-12 rounded-full bg-muted flex items-center justify-center mb-3">
                <UserRound className="size-6 text-muted-foreground" />
              </div>
              <p className="text-sm font-medium">Nenhum contador com acesso</p>
              <p className="text-xs text-muted-foreground mt-1">
                Conceda acesso abaixo para que um contador visualize suas informações financeiras.
              </p>
            </div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead className="pl-6">Contador</TableHead>
                  <TableHead>E-mail</TableHead>
<TableHead>Status</TableHead>
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
                          {STATUS_LABEL[accountant.status] ?? accountant.status}
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
          O contador terá acesso com papel <strong className="text-foreground">CONTADOR</strong> e
          perfil somente leitura. Ele poderá visualizar lançamentos, despesas, DRE e relatórios,
          mas não poderá alterar dados nem configurações da conta.
        </p>
      </div>
    </div>
  )
}
