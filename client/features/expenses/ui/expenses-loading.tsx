import { Skeleton } from '@/shared/ui/skeleton'
import { Card, CardContent, CardHeader } from '@/shared/ui/card'

export default function DespesasLoading() {
  return (
    <div className="flex w-full flex-col gap-6">
      <div className="flex items-start justify-between"><div className="flex flex-col gap-2"><Skeleton className="h-8 w-32" /><Skeleton className="h-4 w-80" /></div><Skeleton className="h-10 w-36" /></div>
      <div className="grid grid-cols-2 overflow-hidden rounded-lg border border-border xl:grid-cols-4">{Array.from({ length: 4 }).map((_, index) => <div key={index} className="flex min-h-28 flex-col justify-between border-r border-border p-5"><Skeleton className="h-3 w-24" /><Skeleton className="h-7 w-32" /><Skeleton className="h-3 w-28" /></div>)}</div>
      <Skeleton className="h-20 w-full rounded-lg" />
      <Card><CardHeader><Skeleton className="h-4 w-40" /></CardHeader><CardContent className="flex flex-col gap-2">{Array.from({ length: 7 }).map((_, index) => <Skeleton key={index} className="h-11 w-full" />)}</CardContent></Card>
    </div>
  )
}
