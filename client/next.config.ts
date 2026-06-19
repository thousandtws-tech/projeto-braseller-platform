import type { NextConfig } from 'next'

function getAllowedDevOrigins(): string[] {
  const appUrl = process.env.NEXT_PUBLIC_APP_URL?.trim()
  if (!appUrl) return []

  try {
    const hostname = new URL(appUrl).hostname
    return hostname === 'localhost' || hostname === '127.0.0.1' ? [] : [hostname]
  } catch {
    return []
  }
}

const nextConfig: NextConfig = {
  output: 'standalone',
  allowedDevOrigins: getAllowedDevOrigins(),
  experimental: {
    serverActions: {
      bodySizeLimit: '5mb',
    },
    proxyClientMaxBodySize: '5mb',
  },
  images: {
    remotePatterns: [
      { protocol: 'https', hostname: 'res.cloudinary.com' },
    ],
  },
}

export default nextConfig
