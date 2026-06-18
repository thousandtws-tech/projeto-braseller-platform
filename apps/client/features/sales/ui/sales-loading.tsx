import { Skeleton } from '@/shared/ui/skeleton'
import { Card, CardContent, CardHeader } from '@/shared/ui/card'

export default function LancamentosLoading() {
  return (
    <div className="flex w-full flex-col gap-6">
      <div className="flex items-start justify-between">
        <div className="flex flex-col gap-2">
          <Skeleton className="h-8 w-40" />
          <Skeleton className="h-4 w-72" />
        </div>
        <Skeleton className="h-10 w-32" />
      </div>

      <div className="grid overflow-hidden rounded-lg border border-border md:grid-cols-2 xl:grid-cols-4">
        {Array.from({ length: 4 }).map((_, index) => (
          <div key={index} className="flex min-h-28 flex-col justify-between gap-3 border-border p-5 md:border-r">
            <Skeleton className="h-3 w-24" />
            <Skeleton className="h-7 w-32" />
            <Skeleton className="h-3 w-28" />
          </div>
        ))}
      </div>

      <Card>
        <CardContent className="flex flex-col gap-4 p-4">
          <Skeleton className="h-10 w-full" />
          <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-6">
            {Array.from({ length: 6 }).map((_, index) => (
              <Skeleton key={index} className="h-10 w-full" />
            ))}
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader><Skeleton className="h-4 w-40" /></CardHeader>
        <CardContent className="flex flex-col gap-1 p-0">
          {Array.from({ length: 8 }).map((_, index) => (
            <Skeleton key={index} className="h-12 w-full rounded-none" />
          ))}
        </CardContent>
      </Card>
    </div>
  )
}
