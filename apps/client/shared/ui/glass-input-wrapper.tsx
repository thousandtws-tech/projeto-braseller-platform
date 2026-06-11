import { cn } from '@/shared/lib/utils'

export function GlassInputWrapper({ children, className }: { children: React.ReactNode; className?: string }) {
  return (
    <div
      className={cn(
        'rounded-2xl border border-border bg-foreground/5 backdrop-blur-sm transition-colors focus-within:border-primary/70 focus-within:bg-primary/10',
        className
      )}
    >
      {children}
    </div>
  )
}
