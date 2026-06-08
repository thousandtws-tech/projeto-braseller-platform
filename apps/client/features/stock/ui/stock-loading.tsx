export default function Loading() {
  return (
    <div className="space-y-6 max-w-5xl animate-pulse">
      <div className="h-6 w-40 bg-muted rounded" />
      <div className="grid grid-cols-2 gap-4">
        <div className="h-24 bg-muted rounded-xl" />
        <div className="h-24 bg-muted rounded-xl" />
      </div>
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="h-48 bg-muted rounded-xl" />
        <div className="h-48 bg-muted rounded-xl" />
      </div>
      <div className="h-64 bg-muted rounded-xl" />
    </div>
  )
}
