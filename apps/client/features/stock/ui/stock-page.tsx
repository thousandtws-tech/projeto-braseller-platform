import { Package, ShoppingBag } from 'lucide-react'
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/ui/card'
import { Badge } from '@/shared/ui/badge'
import { getToken, getSession } from '@/entities/session/server/session'
import { getStockItems, getPurchaseEntries, formatCurrency } from '@/shared/api/gateway'
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
  const dict = await getDictionary(lang)
  const token = (await getToken()) ?? ''
  const session = await getSession()
  const tenantId = session?.tenantId ?? ''
  const readOnly = isReadOnlyAccountant(session?.roles)
  const { from, to } = currentMonthBounds()

  const [items, purchases] = await Promise.all([
    tenantId ? getStockItems(token, tenantId) : Promise.resolve([]),
    tenantId ? getPurchaseEntries(token, tenantId, from, to) : Promise.resolve([]),
  ])

  const totalSkus = items.length
  const totalPurchaseCost = purchases.reduce((sum, p) => sum + p.total_cost, 0)

  return (
    <div className="space-y-6 max-w-5xl">
      <div>
        <h2 className="text-xl font-semibold">{dict.stock.header.title}</h2>
        <p className="text-sm text-muted-foreground">
          {dict.stock.header.subtitle}
        </p>
      </div>

      {/* KPIs */}
      <div className="grid grid-cols-2 gap-4">
        <Card>
          <CardContent className="pt-6 flex items-center gap-4">
            <div className="size-10 rounded-full bg-blue-500/10 flex items-center justify-center">
              <Package className="size-5 text-blue-600" />
            </div>
            <div>
              <p className="text-2xl font-bold">{totalSkus}</p>
              <p className="text-xs text-muted-foreground">{dict.stock.kpis.productsRegistered}</p>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6 flex items-center gap-4">
            <div className="size-10 rounded-full bg-amber-500/10 flex items-center justify-center">
              <ShoppingBag className="size-5 text-amber-600" />
            </div>
            <div>
              <p className="text-2xl font-bold">{formatCurrency(totalPurchaseCost)}</p>
              <p className="text-xs text-muted-foreground">{dict.stock.kpis.purchasesThisMonth}</p>
            </div>
          </CardContent>
        </Card>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Import NF-e */}
        <Card>
          <CardHeader>
            <CardTitle>{dict.stock.importNfe.title}</CardTitle>
          </CardHeader>
          <CardContent>
            <UploadNfeForm readOnly={readOnly} dict={dict} />
            <p className="mt-3 text-xs text-muted-foreground">
              {dict.stock.importNfe.hint}
            </p>
          </CardContent>
        </Card>

        {/* Manual product registration */}
        <Card>
          <CardHeader>
            <CardTitle>{dict.stock.manualForm.title}</CardTitle>
          </CardHeader>
          <CardContent>
            <StockItemForm readOnly={readOnly} dict={dict} />
            <p className="mt-3 text-xs text-muted-foreground">
              {dict.stock.manualForm.hint}
            </p>
          </CardContent>
        </Card>
      </div>

      {/* Stock items list */}
      <Card>
        <CardHeader>
          <CardTitle>{formatMessage(dict.stock.registeredProducts.title, { count: totalSkus })}</CardTitle>
        </CardHeader>
        <CardContent>
          {items.length === 0 ? (
            <div className="py-8 text-center text-sm text-muted-foreground">
              {dict.stock.registeredProducts.empty}
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-border text-muted-foreground">
                    <th className="text-left py-2 pr-4 font-medium">{dict.stock.registeredProducts.columns.sku}</th>
                    <th className="text-left py-2 pr-4 font-medium">{dict.stock.registeredProducts.columns.description}</th>
                    <th className="text-right py-2 pr-4 font-medium">{dict.stock.registeredProducts.columns.unitCost}</th>
                    <th className="text-right py-2 font-medium">{dict.stock.registeredProducts.columns.stock}</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-border">
                  {items.map((item) => (
                    <tr key={item.id} className="hover:bg-muted/30 transition-colors">
                      <td className="py-2.5 pr-4 font-mono text-xs">{item.sku}</td>
                      <td className="py-2.5 pr-4 text-muted-foreground">{item.description}</td>
                      <td className="py-2.5 pr-4 text-right tabular-nums font-medium">
                        {formatCurrency(item.unit_cost)}
                      </td>
                      <td className="py-2.5 text-right tabular-nums">
                        <Badge variant={item.quantity > 0 ? 'secondary' : 'outline'}>
                          {formatMessage(dict.stock.registeredProducts.units, { qty: Number(item.quantity).toFixed(0) })}
                        </Badge>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Purchase entries */}
      {purchases.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle>{formatMessage(dict.stock.monthlyEntries.title, { count: purchases.length })}</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-border text-muted-foreground">
                    <th className="text-left py-2 pr-4 font-medium">{dict.stock.monthlyEntries.columns.invoice}</th>
                    <th className="text-left py-2 pr-4 font-medium">{dict.stock.monthlyEntries.columns.supplier}</th>
                    <th className="text-left py-2 pr-4 font-medium">{dict.stock.monthlyEntries.columns.date}</th>
                    <th className="text-right py-2 font-medium">{dict.stock.monthlyEntries.columns.total}</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-border">
                  {purchases.map((p) => (
                    <tr key={p.id} className="hover:bg-muted/30 transition-colors">
                      <td className="py-2.5 pr-4 font-mono text-xs">{p.nfe_number ?? '—'}</td>
                      <td className="py-2.5 pr-4 text-muted-foreground">{p.supplier_name ?? '—'}</td>
                      <td className="py-2.5 pr-4 text-muted-foreground">
                        {new Date(p.issue_date).toLocaleDateString(LOCALE_MAP[lang])}
                      </td>
                      <td className="py-2.5 text-right font-medium tabular-nums">
                        {formatCurrency(p.total_cost)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  )
}
