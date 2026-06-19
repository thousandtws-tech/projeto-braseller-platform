export function resolveGatewayUrl() {
  const raw =
    process.env.GATEWAY_URL ??
    process.env.NEXT_PUBLIC_GATEWAY_URL ??
    'http://localhost:8080'

  return raw.trim().replace(/^["']|["']$/g, '').replace(/\/+$/, '')
}
