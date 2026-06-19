import { getToken } from '@/entities/session/server/session'

export const dynamic = 'force-dynamic'
export const runtime = 'nodejs'

function coreServiceUrl() {
  return (process.env.CORE_SERVICE_URL ?? 'http://localhost:8081')
    .trim()
    .replace(/^["']|["']$/g, '')
    .replace(/\/+$/, '')
}

function websocketUrl() {
  const configured = process.env.CORE_REALTIME_WS_PUBLIC_URL?.trim()
  if (configured) return configured.replace(/\/+$/, '')
  return process.env.NODE_ENV === 'development'
    ? 'ws://localhost:8081/core/connectors/events/ws'
    : null
}

export async function POST() {
  const token = await getToken()
  if (!token) {
    return Response.json({ message: 'missing_session' }, { status: 401 })
  }

  const upstream = await fetch(`${coreServiceUrl()}/core/connectors/realtime-ticket`, {
    method: 'POST',
    headers: {
      Accept: 'application/json',
      Authorization: `Bearer ${token}`,
    },
    cache: 'no-store',
  })

  const payload = (await upstream.json().catch(() => ({}))) as Record<string, unknown>
  if (!upstream.ok) {
    return Response.json(payload, { status: upstream.status })
  }

  return Response.json({
    ticket: payload.ticket,
    expiresAt: payload.expiresAt,
    streamId: payload.streamId,
    websocketUrl: websocketUrl(),
  })
}
