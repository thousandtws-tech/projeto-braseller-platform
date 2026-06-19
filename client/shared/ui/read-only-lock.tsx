import { LockKeyhole } from 'lucide-react'
import { cn } from '@/shared/lib/utils'

interface ReadOnlyLockProps {
  title?: string
  description?: string
  compact?: boolean
  className?: string
}

export function ReadOnlyLock({
  title = 'Somente visualizacao',
  description = 'Seu perfil de contador permite consultar os dados, mas nao alterar esta area.',
  compact = false,
  className,
}: ReadOnlyLockProps) {
  return (
    <div
      className={cn(
        'flex items-center gap-3 rounded-lg border border-amber-500/30 bg-amber-500/8 px-3 py-2 text-amber-800 dark:text-amber-300',
        compact && 'gap-2 px-2.5 py-1.5',
        className
      )}
    >
      <span className="relative flex size-8 shrink-0 items-center justify-center rounded-full bg-amber-500/15">
        <span className="absolute inline-flex size-full animate-ping rounded-full bg-amber-400/20" />
        <LockKeyhole className="relative size-4 animate-pulse" />
      </span>
      <div className="min-w-0">
        <p className={cn('font-medium', compact ? 'text-xs' : 'text-sm')}>{title}</p>
        {!compact && <p className="text-xs text-muted-foreground">{description}</p>}
      </div>
    </div>
  )
}

export function ReadOnlyLockInline({ className }: { className?: string }) {
  return (
    <span
      className={cn(
        'inline-flex items-center gap-1.5 rounded-md border border-amber-500/30 bg-amber-500/8 px-2 py-1 text-xs font-medium text-amber-800 dark:text-amber-300',
        className
      )}
    >
      <LockKeyhole className="size-3.5 animate-pulse" />
      Bloqueado
    </span>
  )
}
