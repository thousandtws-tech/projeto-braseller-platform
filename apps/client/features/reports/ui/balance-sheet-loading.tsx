import { Skeleton } from '@/shared/ui/skeleton'

export default function BalanceSheetLoading() {
  return (
    <div className="flex w-full flex-col gap-6">
      <div className="flex items-start justify-between"><div className="flex flex-col gap-2"><Skeleton className="h-8 w-56" /><Skeleton className="h-4 w-72" /></div><Skeleton className="h-8 w-36" /></div>
      <div className="grid grid-cols-2 overflow-hidden rounded-lg border border-border xl:grid-cols-4">{Array.from({ length: 4 }).map((_, index) => <div key={index} className="flex min-h-28 flex-col justify-between border-r border-border p-5"><Skeleton className="h-3 w-24" /><Skeleton className="h-7 w-32" /><Skeleton className="h-3 w-28" /></div>)}</div>
      <div className="grid gap-6 xl:grid-cols-2"><Skeleton className="h-80 w-full rounded-lg" /><Skeleton className="h-80 w-full rounded-lg" /></div>
      <Skeleton className="h-40 w-full rounded-lg" />
    </div>
  )
}
