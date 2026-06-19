import { Eye, ShieldCheck, UserPlus, UserRound, Users } from 'lucide-react'

import { Avatar, AvatarFallback, AvatarImage } from '@/shared/ui/avatar'
import { Badge } from '@/shared/ui/badge'
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/ui/card'
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

interface Props { params: Promise<{ lang: Locale }> }
const STATUS_VARIANT: Record<string, 'success' | 'secondary' | 'warning' | 'destructive'> = { ACTIVE: 'success', INACTIVE: 'secondary', PENDING: 'warning' }

export async function generateMetadata({ params }: Props) {
  const { lang } = await params
  const dict = await getDictionary(lang)
  return { title: dict.accountant.title }
}

export default async function ContadorPage({ params }: Props) {
  const { lang } = await params
  const [dict, token, session] = await Promise.all([getDictionary(lang), getToken().then((value) => value ?? ''), getSession()])
  if (!session) return null
  const readOnly = isReadOnlyAccountant(session.roles)
  const accountants = await getAccountants(token, session.tenantId)
  const active = accountants.filter((accountant) => accountant.status === 'ACTIVE').length
  const pending = accountants.filter((accountant) => accountant.status === 'PENDING').length
  const statusLabels = dict.accountant.status as Record<string, string>

  return (
    <div className="flex w-full flex-col gap-6">
      <header>
        <h2 className="text-[1.8rem] font-semibold tracking-[-0.04em]">{dict.accountant.title}</h2>
        <p className="mt-1 text-sm text-muted-foreground">{dict.accountant.subtitle}</p>
      </header>

      <section className="grid grid-cols-2 overflow-hidden rounded-lg border border-border bg-card xl:grid-cols-4">
        <Metric label="Contadores" value={accountants.length} helper="Com acesso cadastrado" icon={Users} />
        <Metric label="Ativos" value={active} helper="Acesso disponível" icon={ShieldCheck} />
        <Metric label="Pendentes" value={pending} helper="Aguardando ativação" icon={UserPlus} />
        <Metric label="Permissão" value="Leitura" helper="Sem alterações financeiras" icon={Eye} />
      </section>

      <section className="grid gap-6 xl:grid-cols-[380px_minmax(0,1fr)]">
        <Card>
          <CardHeader><CardTitle>{dict.accountant.grantForm.title}</CardTitle><p className="text-xs leading-5 text-muted-foreground">Crie um acesso individual e temporário para o profissional responsável.</p></CardHeader>
          <CardContent><AccountantForm readOnly={readOnly} dict={dict} /></CardContent>
        </Card>

        <Card className="overflow-hidden">
          <CardHeader className="flex-row items-center justify-between gap-4"><div><CardTitle>{dict.accountant.table.title}</CardTitle><p className="mt-1 text-xs text-muted-foreground">Pessoas autorizadas a visualizar os dados contábeis.</p></div><Badge variant="secondary">{accountants.length}</Badge></CardHeader>
          <CardContent className="p-0">
            {accountants.length === 0 ? (
              <div className="flex min-h-72 flex-col items-center justify-center gap-3 px-6 text-center"><div className="flex size-11 items-center justify-center rounded-full border border-border bg-muted/40"><UserRound className="size-5 text-muted-foreground" /></div><div><p className="font-medium">{dict.accountant.table.empty.title}</p><p className="mt-1 max-w-sm text-sm text-muted-foreground">{dict.accountant.table.empty.hint}</p></div></div>
            ) : (
              <Table>
                <TableHeader className="bg-muted/70"><TableRow className="hover:bg-muted/70"><TableHead className="pl-5">{dict.accountant.table.columns.name}</TableHead><TableHead>{dict.accountant.table.columns.email}</TableHead><TableHead className="pr-5">{dict.accountant.table.columns.status}</TableHead></TableRow></TableHeader>
                <TableBody>
                  {accountants.map((accountant) => {
                    const name = accountant.fullName || `${accountant.firstName ?? ''} ${accountant.lastName ?? ''}`.trim() || accountant.email
                    const initials = name.split(' ').slice(0, 2).map((part) => part[0]).join('').toUpperCase()
                    return (
                      <TableRow key={accountant.id}>
                        <TableCell className="pl-5"><div className="flex items-center gap-3"><Avatar className="size-8"><AvatarImage src={accountant.pictureUrl} alt={name} /><AvatarFallback className="bg-foreground text-xs text-background">{initials}</AvatarFallback></Avatar><span className="font-medium">{name}</span></div></TableCell>
                        <TableCell className="text-muted-foreground">{accountant.email}</TableCell>
                        <TableCell className="pr-5"><Badge variant={STATUS_VARIANT[accountant.status] ?? 'secondary'}>{statusLabels[accountant.status] ?? accountant.status}</Badge></TableCell>
                      </TableRow>
                    )
                  })}
                </TableBody>
              </Table>
            )}
          </CardContent>
        </Card>
      </section>

      <div className="flex items-start gap-3 rounded-md border border-border bg-muted/35 p-4 text-sm text-muted-foreground"><ShieldCheck className="mt-0.5 size-4 shrink-0 text-foreground" /><p>{dict.accountant.accessNote.prefix}<strong className="text-foreground">{dict.accountant.accessNote.highlight}</strong>{dict.accountant.accessNote.suffix}</p></div>
    </div>
  )
}

function Metric({ label, value, helper, icon: Icon }: { label: string; value: number | string; helper: string; icon: React.ComponentType<{ className?: string }> }) {
  return <div className="flex min-h-32 flex-col justify-between gap-3 border-b border-r border-border p-5 even:border-r-0 [&:nth-last-child(-n+2)]:border-b-0 xl:min-h-28 xl:border-b-0 xl:even:border-r xl:last:border-r-0"><div className="flex items-center justify-between"><span className="text-xs text-muted-foreground">{label}</span><Icon className="size-4 text-muted-foreground" /></div><p className="text-2xl font-semibold tracking-[-0.035em]">{value}</p><p className="text-[11px] text-muted-foreground">{helper}</p></div>
}
