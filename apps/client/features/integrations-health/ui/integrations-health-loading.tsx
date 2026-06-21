import { Skeleton } from '@/shared/ui/skeleton'
import { Card, CardContent, CardHeader } from '@/shared/ui/card'

export default function IntegrationsHealthLoading() {
  return (
    <div className="space-y-6">
      <Skeleton className="h-7 w-56" />
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {Array.from({ length: 6 }).map((_, i) => (
          <Card key={i}>
            <CardHeader className="flex-row items-center gap-3">
              <Skeleton className="size-8 rounded-lg" />
              <Skeleton className="h-5 w-32" />
            </CardHeader>
            <CardContent className="space-y-3">
              <Skeleton className="h-4 w-full" />
              <Skeleton className="h-4 w-full" />
              <Skeleton className="h-4 w-3/4" />
            </CardContent>
          </Card>
        ))}
      </div>
      <Card>
        <CardContent className="p-6">
          <Skeleton className="h-40 w-full" />
        </CardContent>
      </Card>
    </div>
  )
}
