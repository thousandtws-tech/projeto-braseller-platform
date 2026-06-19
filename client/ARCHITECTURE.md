# Client Architecture

The client uses Next.js App Router with an incremental Feature-Sliced Design layout and Clean Architecture boundaries.

## Layers

`app/`
: Route layer only. Keep URL structure, layouts, route handlers, loading states, and page orchestration here.

`features/`
: User-facing use cases. A feature owns its UI, server actions, and API orchestration for one interaction, such as authentication.

`entities/`
: Business entities and domain rules that can be reused across features, such as session parsing and role permissions.

`widgets/`
: Composed UI blocks that combine entities/features/shared UI into larger screen regions, such as the authenticated app shell.

`shared/`
: Framework-neutral or business-neutral building blocks: config, generic UI primitives, utilities, and low-level adapters.

## Import Direction

Allowed direction:

```text
app -> widgets -> features -> entities -> shared
app -> features -> entities -> shared
app -> entities -> shared
```

Avoid importing from `app/` inside `features`, `entities`, `widgets`, or `shared`.

## Current Compatibility Bridges

These files remain as temporary adapters for older imports:

- `app/actions/auth.ts`
- `components/layout/header.tsx`
- `components/layout/sidebar.tsx`
- `components/read-only-lock.tsx`
- `lib/auth.ts`
- `lib/permissions.ts`
- `lib/utils.ts`
- `lib/api.ts`
- `types/index.ts`
- `components/shadcn-space/*`

New code should import from the FSD layers directly.

## Business Domains

| Feature | Route(s) | Owns |
| --- | --- | --- |
| `features/auth` | `/login`, `/register`, `/auth/callback`, auth API routes | Login, signup, Google OAuth, auth server actions |
| `features/dashboard` | `/dashboard` | Dashboard view and loading UI |
| `features/sales` | `/lancamentos` | Sales/report entries view |
| `features/expenses` | `/despesas` | Expense list, filters, row actions and forms |
| `features/reports` | `/dre` | DRE, fiscal profile saving, profit distribution, batch report actions |
| `features/stock` | `/estoque` | Stock item form, NFe upload, stock page |
| `features/bank` | `/extrato` | OFX upload and bank transactions |
| `features/connectors` | `/conectores`, `/conectores/callback` | Marketplace connector cards, OAuth callback, connector actions |
| `features/notifications` | `/notificacoes` | Notification list/preferences/actions |
| `features/accountant` | `/contador` | Accountant access and accountant actions |
| `features/bpo` | `/bpo` | Multi-client BPO dashboard and batch closing UI |

`app/(app)/*` files now re-export domain views from these features, keeping App Router as the routing layer.
