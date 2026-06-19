import { getToken } from '@/entities/session/server/session'

export const dynamic = 'force-dynamic'
export const runtime = 'nodejs'

function coreServiceUrl() {
  return (process.env.CORE_SERVICE_URL ?? 'http://localhost:8081')
    .trim()
    .replace(/^["']|["']$/g, '')
    .replace(/\/+$/, '')
}

export async function GET(request: Request) {
  const token = await getToken()
  if (!token) {
    return Response.json({ message: 'missing_session' }, { status: 401 })
  }

  const requestUrl = new URL(request.url)
  const cursor = requestUrl.searchParams.get('cursor') ?? '0'
  const upstream = await fetch(
    `${coreServiceUrl()}/core/connectors/events?cursor=${encodeURIComponent(cursor)}`,
    {
      headers: {
        Accept: 'text/event-stream',
        Authorization: `Bearer ${token}`,
        'Last-Event-ID': request.headers.get('Last-Event-ID') ?? cursor,
      },
      cache: 'no-store',
      signal: request.signal,
    }
  )

  if (!upstream.ok || !upstream.body) {
    const body = await upstream.text().catch(() => '')
    return new Response(body, {
      status: upstream.status,
      headers: { 'Content-Type': upstream.headers.get('Content-Type') ?? 'application/json' },
    })
  }

  return new Response(upstream.body, {
    status: 200,
    headers: {
      'Cache-Control': 'no-cache, no-store, must-revalidate',
      Connection: 'keep-alive',
      'Content-Type': 'text/event-stream',
      'X-Accel-Buffering': 'no',
    },
  })
}
