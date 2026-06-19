'use client'

import {
  CartesianGrid,
  Legend,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'

import { formatCurrency } from '@/shared/api/gateway'
import type { DashboardView } from '@/shared/types'

export function FinancialFlowChart({ data }: { data: DashboardView['monthlyEvolution'] }) {
  return (
    <div className="h-64 w-full">
      <ResponsiveContainer width="100%" height="100%">
        <LineChart data={data} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
          <CartesianGrid vertical={false} stroke="var(--border)" strokeDasharray="3 3" />
          <XAxis dataKey="month" tickLine={false} axisLine={false} tick={{ fill: 'var(--muted-foreground)', fontSize: 11 }} />
          <YAxis tickLine={false} axisLine={false} width={58} tick={{ fill: 'var(--muted-foreground)', fontSize: 11 }} tickFormatter={(value) => `${Math.round(value / 1000)}k`} />
          <Tooltip
            formatter={(value) => formatCurrency(Number(value))}
            contentStyle={{ border: '1px solid var(--border)', borderRadius: 6, boxShadow: 'none', fontSize: 12 }}
          />
          <Legend iconType="plainline" wrapperStyle={{ fontSize: 11, color: 'var(--muted-foreground)' }} />
          <Line name="Receita" type="monotone" dataKey="grossRevenue" stroke="var(--chart-1)" strokeWidth={2} dot={false} />
          <Line name="Recebido" type="monotone" dataKey="received" stroke="var(--chart-3)" strokeWidth={2} strokeDasharray="5 4" dot={false} />
        </LineChart>
      </ResponsiveContainer>
    </div>
  )
}
