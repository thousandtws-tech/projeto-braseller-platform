import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { DollarSign, TrendingUp, Receipt, Clock } from "lucide-react"

const stats = [
  {
    title: "Receita Bruta",
    value: "R$ 45.231,00",
    description: "Este mês",
    icon: DollarSign,
  },
  {
    title: "Valor Recebido",
    value: "R$ 38.450,00",
    description: "Já liberado",
    icon: TrendingUp,
  },
  {
    title: "Taxas",
    value: "R$ 4.523,10",
    description: "10% média",
    icon: Receipt,
  },
  {
    title: "A Receber",
    value: "R$ 6.781,00",
    description: "Pendente liberação",
    icon: Clock,
  },
]

const recentOrders = [
  { id: "ML-001234", platform: "Mercado Livre", value: "R$ 150,00", status: "Pago", date: "Hoje" },
  { id: "ML-001233", platform: "Mercado Livre", value: "R$ 89,90", status: "Pendente", date: "Hoje" },
  { id: "ML-001232", platform: "Mercado Livre", value: "R$ 245,00", status: "Pago", date: "Ontem" },
  { id: "ML-001231", platform: "Mercado Livre", value: "R$ 67,50", status: "Pago", date: "Ontem" },
  { id: "ML-001230", platform: "Mercado Livre", value: "R$ 320,00", status: "Pago", date: "22/05" },
]

export default function DashboardPage() {
  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div>
        <h1 className="text-2xl font-semibold text-foreground">Dashboard</h1>
        <p className="text-muted-foreground">Visão geral das suas vendas</p>
      </div>

      {/* Stats Grid */}
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        {stats.map((stat) => (
          <Card key={stat.title}>
            <CardHeader className="flex flex-row items-center justify-between pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">
                {stat.title}
              </CardTitle>
              <stat.icon className="w-4 h-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold text-foreground">{stat.value}</div>
              <p className="text-xs text-muted-foreground mt-1">{stat.description}</p>
            </CardContent>
          </Card>
        ))}
      </div>

      {/* Content */}
      <div className="grid gap-4 lg:grid-cols-2">
        {/* Recent Orders */}
        <Card>
          <CardHeader>
            <CardTitle>Últimos Lançamentos</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {recentOrders.map((order) => (
                <div key={order.id} className="flex items-center justify-between py-2 border-b border-border last:border-0">
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 rounded-md bg-muted border border-border flex items-center justify-center text-xs font-medium">
                      ML
                    </div>
                    <div>
                      <p className="text-sm font-medium text-foreground">{order.id}</p>
                      <p className="text-xs text-muted-foreground">{order.platform}</p>
                    </div>
                  </div>
                  <div className="text-right">
                    <p className="text-sm font-medium text-foreground">{order.value}</p>
                    <p className="text-xs text-muted-foreground">{order.date}</p>
                  </div>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>

        {/* Chart Placeholder */}
        <Card>
          <CardHeader>
            <CardTitle>Evolução Mensal</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="h-[280px] flex items-center justify-center border-2 border-dashed border-border rounded-lg">
              <p className="text-muted-foreground text-sm">Gráfico de Evolução</p>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Platform Comparison */}
      <Card>
        <CardHeader>
          <CardTitle>Comparativo por Plataforma</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="h-[200px] flex items-center justify-center border-2 border-dashed border-border rounded-lg">
            <p className="text-muted-foreground text-sm">Gráfico de Comparativo por Marketplace</p>
          </div>
        </CardContent>
      </Card>
    </div>
  )
}
