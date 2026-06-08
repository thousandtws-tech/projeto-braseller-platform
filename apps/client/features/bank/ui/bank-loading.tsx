export default function Loading() {
  return (
    <div className="space-y-6 max-w-5xl animate-pulse">
      <div className="h-6 w-48 bg-muted rounded" />
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 space-y-4">
          <div className="h-48 bg-muted rounded-xl" />
          <div className="h-64 bg-muted rounded-xl" />
        </div>
        <div className="space-y-4">
          <div className="h-28 bg-muted rounded-xl" />
          <div className="h-48 bg-muted rounded-xl" />
        </div>
      </div>
    </div>
  )
}
