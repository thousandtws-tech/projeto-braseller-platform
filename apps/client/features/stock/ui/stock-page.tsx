import { Boxes, CircleDollarSign, FilePlus2, Package, ShoppingBag } from 'lucide-react'

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
import { formatCurrency, getPurchaseEntries, getStockItems } from '@/shared/api/gateway'
import { isReadOnlyAccountant } from '@/entities/session/model/permissions'
import { getDictionary } from '@/shared/i18n/get-dictionary'
import { formatMessage } from '@/shared/i18n/format'
import type { Locale } from '@/shared/i18n/config'
import { UploadNfeForm } from './upload-nfe-form'
import { StockItemForm } from './stock-item-form'

const LOCALE_MAP: Record<Locale, string> = { 'pt-BR': 'pt-BR', en: 'en-US', es: 'es-ES' }

function currentMonthBounds() {
  const now = new Date()
  return {
    from: new Date(now.getFullYear(), now.getMonth(), 1).toISOString().split('T')[0],
    to: new Date(now.getFullYear(), now.getMonth() + 1, 0).toISOString().split('T')[0],
  }
}

interface Props {
  params: Promise<{ lang: Locale }>
}

export async function generateMetadata({ params }: Props) {
  const { lang } = await params
  const dict = await getDictionary(lang)
  return { title: dict.stock.title }
}

export default async function EstoquePage({ params }: Props) {
  const { lang } = await params
  const [dict, token, session] = await Promise.all([
    getDictionary(lang),
    getToken().then((value) => value ?? ''),
    getSession(),
  ])
  const tenantId = session?.tenantId ?? ''
  const readOnly = isReadOnlyAccountant(session?.roles)
  const { from, to } = currentMonthBounds()

  const [items, purchases] = await Promise.all([
    tenantId ? getStockItems(token, tenantId) : Promise.resolve([]),
    tenantId ? getPurchaseEntries(token, tenantId, from, to) : Promise.resolve([]),
  ])

  const totalPurchaseCost = purchases.reduce((sum, purchase) => sum + purchase.total_cost, 0)
  const inventoryValue = items.reduce((sum, item) => sum + item.unit_cost * item.quantity, 0)
  const totalUnits = items.reduce((sum, item) => sum + Number(item.quantity), 0)
  const outOfStock = items.filter((item) => Number(item.quantity) <= 0).length

  return (
    <div className="flex w-full flex-col gap-6">
      <header>
        <h2 className="text-[1.8rem] font-semibold tracking-[-0.04em]">{dict.stock.header.title}</h2>
        <p className="mt-1 max-w-3xl text-sm text-muted-foreground">{dict.stock.header.subtitle}</p>
      </header>

      <section className="grid grid-cols-2 overflow-hidden rounded-lg border border-border bg-card xl:grid-cols-4">
        <Metric label={dict.stock.kpis.productsRegistered} value={String(items.length)} helper={`${totalUnits.toFixed(0)} unidades`} icon={Package} />
        <Metric label="Valor em estoque" value={formatCurrency(inventoryValue)} helper="Custo atual estimado" icon={CircleDollarSign} />
        <Metric label={dict.stock.kpis.purchasesThisMonth} value={formatCurrency(totalPurchaseCost)} helper={`${purchases.length} entradas`} icon={ShoppingBag} />
        <Metric label="Sem estoque" value={String(outOfStock)} helper={outOfStock > 0 ? 'Produtos que exigem atenção' : 'Nenhuma ruptura'} icon={Boxes} />
      </section>

      <section className="grid gap-6 xl:grid-cols-[1.05fr_0.95fr]">
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2"><FilePlus2 className="size-4" />{dict.stock.importNfe.title}</CardTitle>
            <p className="text-xs leading-5 text-muted-foreground">{dict.stock.importNfe.hint}</p>
          </CardHeader>
          <CardContent><UploadNfeForm readOnly={readOnly} dict={dict} /></CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>{dict.stock.manualForm.title}</CardTitle>
            <p className="text-xs leading-5 text-muted-foreground">{dict.stock.manualForm.hint}</p>
          </CardHeader>
          <CardContent><StockItemForm readOnly={readOnly} dict={dict} /></CardContent>
        </Card>
      </section>

      <Card className="overflow-hidden">
        <CardHeader className="flex-row items-center justify-between gap-4">
          <div>
            <CardTitle>{formatMessage(dict.stock.registeredProducts.title, { count: items.length })}</CardTitle>
            <p className="mt-1 text-xs text-muted-foreground">Custos e quantidades utilizados no cálculo do CMV.</p>
          </div>
          {outOfStock > 0 ? <Badge variant="warning">{outOfStock} sem estoque</Badge> : <Badge variant="secondary">Estoque regular</Badge>}
        </CardHeader>
        <CardContent className="p-0">
          {items.length === 0 ? (
            <EmptyState icon={Package} title="Nenhum produto cadastrado" description={dict.stock.registeredProducts.empty} />
          ) : (
            <Table>
              <TableHeader className="bg-muted/70">
                <TableRow className="hover:bg-muted/70">
                  <TableHead className="pl-5">{dict.stock.registeredProducts.columns.sku}</TableHead>
                  <TableHead>{dict.stock.registeredProducts.columns.description}</TableHead>
                  <TableHead className="text-right">{dict.stock.registeredProducts.columns.unitCost}</TableHead>
                  <TableHead className="text-right">{dict.stock.registeredProducts.columns.stock}</TableHead>
                  <TableHead className="pr-5 text-right">Valor total</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {items.map((item) => (
                  <TableRow key={item.id}>
                    <TableCell className="pl-5 font-mono text-xs">{item.sku}</TableCell>
                    <TableCell className="font-medium">{item.description}</TableCell>
                    <TableCell className="text-right font-medium">{formatCurrency(item.unit_cost)}</TableCell>
                    <TableCell className="text-right">
                      <Badge variant={item.quantity > 0 ? 'secondary' : 'warning'}>
                        {formatMessage(dict.stock.registeredProducts.units, { qty: Number(item.quantity).toFixed(0) })}
                      </Badge>
                    </TableCell>
                    <TableCell className="pr-5 text-right font-semibold">{formatCurrency(item.unit_cost * item.quantity)}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>

      <Card className="overflow-hidden">
        <CardHeader className="flex-row items-center justify-between gap-4">
          <div>
            <CardTitle>{formatMessage(dict.stock.monthlyEntries.title, { count: purchases.length })}</CardTitle>
            <p className="mt-1 text-xs text-muted-foreground">Notas importadas no mês atual.</p>
          </div>
          <span className="text-sm font-semibold tabular-nums">{formatCurrency(totalPurchaseCost)}</span>
        </CardHeader>
        <CardContent className="p-0">
          {purchases.length === 0 ? (
            <EmptyState icon={FilePlus2} title="Nenhuma entrada neste mês" description="Importe uma NF-e para atualizar custos e quantidades automaticamente." />
          ) : (
            <Table>
              <TableHeader className="bg-muted/70">
                <TableRow className="hover:bg-muted/70">
                  <TableHead className="pl-5">{dict.stock.monthlyEntries.columns.invoice}</TableHead>
                  <TableHead>{dict.stock.monthlyEntries.columns.supplier}</TableHead>
                  <TableHead>{dict.stock.monthlyEntries.columns.date}</TableHead>
                  <TableHead className="pr-5 text-right">{dict.stock.monthlyEntries.columns.total}</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {purchases.map((purchase) => (
                  <TableRow key={purchase.id}>
                    <TableCell className="pl-5 font-mono text-xs">{purchase.nfe_number ?? '—'}</TableCell>
                    <TableCell className="font-medium">{purchase.supplier_name ?? '—'}</TableCell>
                    <TableCell className="text-xs text-muted-foreground">{new Date(purchase.issue_date).toLocaleDateString(LOCALE_MAP[lang])}</TableCell>
                    <TableCell className="pr-5 text-right font-semibold">{formatCurrency(purchase.total_cost)}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>
    </div>
  )
}

function Metric({ label, value, helper, icon: Icon }: { label: string; value: string; helper: string; icon: React.ComponentType<{ className?: string }> }) {
  return (
    <div className="flex min-h-32 flex-col justify-between gap-3 border-b border-r border-border p-5 even:border-r-0 [&:nth-last-child(-n+2)]:border-b-0 xl:min-h-28 xl:border-b-0 xl:even:border-r xl:last:border-r-0">
      <div className="flex items-center justify-between"><span className="text-xs text-muted-foreground">{label}</span><Icon className="size-4 text-muted-foreground" /></div>
      <p className="text-2xl font-semibold tracking-[-0.035em] tabular-nums">{value}</p>
      <p className="text-[11px] text-muted-foreground">{helper}</p>
    </div>
  )
}

function EmptyState({ icon: Icon, title, description }: { icon: React.ComponentType<{ className?: string }>; title: string; description: string }) {
  return (
    <div className="flex min-h-56 flex-col items-center justify-center gap-3 px-6 text-center">
      <div className="flex size-11 items-center justify-center rounded-full border border-border bg-muted/40"><Icon className="size-5 text-muted-foreground" /></div>
      <div><p className="font-medium">{title}</p><p className="mt-1 max-w-md text-sm leading-6 text-muted-foreground">{description}</p></div>
    </div>
  )
}
