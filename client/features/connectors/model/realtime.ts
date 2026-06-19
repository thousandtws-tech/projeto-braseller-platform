'use client'

import { useEffect, useRef } from 'react'

import type { ConnectorRealtimeEvent } from '@/shared/types'

type Listener = (event: ConnectorRealtimeEvent) => void

interface RealtimeBootstrap {
  ticket: string
  expiresAt: string
  streamId: string
  websocketUrl: string | null
}

const STORAGE_PREFIX = 'braseller:connector-realtime:v1:'
const MAX_WEBSOCKET_FAILURES = 3

class ConnectorRealtimeClient {
  private listeners = new Set<Listener>()
  private abortController: AbortController | null = null
  private websocket: WebSocket | null = null
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null
  private stopTimer: ReturnType<typeof setTimeout> | null = null
  private running = false
  private reconnectAttempt = 0
  private websocketFailures = 0
  private cursor = 0
  private streamId = ''

  subscribe(listener: Listener) {
    this.listeners.add(listener)
    if (this.stopTimer) {
      clearTimeout(this.stopTimer)
      this.stopTimer = null
    }
    if (!this.running) void this.start()

    return () => {
      this.listeners.delete(listener)
      if (this.listeners.size === 0) {
        this.stopTimer = setTimeout(() => this.stop(), 1000)
      }
    }
  }

  private async start() {
    if (this.running || this.listeners.size === 0) return
    this.running = true

    try {
      const bootstrap = await this.bootstrap()
      if (!this.running) return

      this.streamId = bootstrap.streamId
      this.cursor = this.readCursor(bootstrap.streamId)

      if (bootstrap.websocketUrl && 'WebSocket' in window) {
        this.openWebSocket(bootstrap)
      } else {
        void this.openSse()
      }
    } catch {
      this.scheduleReconnect()
    }
  }

  private async bootstrap(): Promise<RealtimeBootstrap> {
    const response = await fetch('/api/realtime/connectors/ticket', {
      method: 'POST',
      cache: 'no-store',
    })
    if (!response.ok) throw new Error(`realtime_bootstrap_${response.status}`)
    return response.json() as Promise<RealtimeBootstrap>
  }

  private openWebSocket(bootstrap: RealtimeBootstrap) {
    const url = `${bootstrap.websocketUrl}/${encodeURIComponent(bootstrap.ticket)}/${this.cursor}`
    const socket = new WebSocket(url)
    this.websocket = socket

    socket.onopen = () => {
      this.reconnectAttempt = 0
      this.websocketFailures = 0
      socket.send(JSON.stringify({ type: 'ping', cursor: this.cursor }))
    }
    socket.onmessage = (message) => {
      if (typeof message.data !== 'string') return
      this.handleJson(message.data)
    }
    socket.onerror = () => socket.close()
    socket.onclose = () => {
      if (this.websocket === socket) this.websocket = null
      if (!this.running) return

      this.websocketFailures += 1
      if (this.websocketFailures >= MAX_WEBSOCKET_FAILURES) {
        void this.openSse()
      } else {
        this.scheduleReconnect()
      }
    }
  }

  private async openSse() {
    if (!this.running || this.abortController) return
    const controller = new AbortController()
    this.abortController = controller

    try {
      const response = await fetch(`/api/realtime/connectors?cursor=${this.cursor}`, {
        headers: {
          Accept: 'text/event-stream',
          'Last-Event-ID': String(this.cursor),
        },
        cache: 'no-store',
        signal: controller.signal,
      })
      if (!response.ok || !response.body) {
        throw new Error(`realtime_sse_${response.status}`)
      }

      this.reconnectAttempt = 0
      await this.consumeSse(response.body, controller.signal)
    } catch (error) {
      if (!(error instanceof DOMException && error.name === 'AbortError')) {
        this.scheduleReconnect(true)
      }
    } finally {
      if (this.abortController === controller) this.abortController = null
    }
  }

  private async consumeSse(stream: ReadableStream<Uint8Array>, signal: AbortSignal) {
    const reader = stream.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    while (!signal.aborted) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true }).replace(/\r\n/g, '\n')

      let boundary = buffer.indexOf('\n\n')
      while (boundary >= 0) {
        const frame = buffer.slice(0, boundary)
        buffer = buffer.slice(boundary + 2)
        this.handleSseFrame(frame)
        boundary = buffer.indexOf('\n\n')
      }
    }

    if (!signal.aborted) this.scheduleReconnect(true)
  }

  private handleSseFrame(frame: string) {
    const data: string[] = []
    for (const line of frame.split('\n')) {
      if (line.startsWith('data:')) data.push(line.slice(5).trimStart())
    }
    if (data.length > 0) this.handleJson(data.join('\n'))
  }

  private handleJson(value: string) {
    try {
      const parsed = JSON.parse(value) as Partial<ConnectorRealtimeEvent>
      if (typeof parsed.sequence !== 'number' || !parsed.event_type || !parsed.payload) {
        return
      }
      if (parsed.sequence <= this.cursor) return

      this.cursor = parsed.sequence
      this.writeCursor()
      const event = parsed as ConnectorRealtimeEvent
      for (const listener of this.listeners) listener(event)

      if (this.websocket?.readyState === WebSocket.OPEN) {
        this.websocket.send(JSON.stringify({ type: 'ack', cursor: this.cursor }))
      }
    } catch {
      // Invalid control or transport frames are ignored without moving the cursor.
    }
  }

  private scheduleReconnect(forceSse = false) {
    if (!this.running || this.reconnectTimer || this.listeners.size === 0) return
    const delay = Math.min(30_000, 750 * 2 ** this.reconnectAttempt)
    const jitter = Math.floor(Math.random() * 350)
    this.reconnectAttempt += 1

    this.reconnectTimer = setTimeout(() => {
      this.reconnectTimer = null
      if (!this.running) return
      if (forceSse || this.websocketFailures >= MAX_WEBSOCKET_FAILURES) {
        void this.openSse()
      } else {
        this.running = false
        void this.start()
      }
    }, delay + jitter)
  }

  private readCursor(streamId: string) {
    try {
      const value = Number(window.localStorage.getItem(`${STORAGE_PREFIX}${streamId}`))
      return Number.isSafeInteger(value) && value > 0 ? value : 0
    } catch {
      return 0
    }
  }

  private writeCursor() {
    if (!this.streamId) return
    try {
      window.localStorage.setItem(`${STORAGE_PREFIX}${this.streamId}`, String(this.cursor))
    } catch {
      // Reconnect still works for the current page when storage is unavailable.
    }
  }

  private stop() {
    this.running = false
    this.abortController?.abort()
    this.abortController = null
    this.websocket?.close(1000, 'no_subscribers')
    this.websocket = null
    if (this.reconnectTimer) clearTimeout(this.reconnectTimer)
    this.reconnectTimer = null
    this.reconnectAttempt = 0
    this.websocketFailures = 0
  }
}

const connectorRealtimeClient = new ConnectorRealtimeClient()

export function useConnectorRealtimeEvent(listener: Listener) {
  const listenerRef = useRef(listener)

  useEffect(() => {
    listenerRef.current = listener
  }, [listener])

  useEffect(
    () => connectorRealtimeClient.subscribe((event) => listenerRef.current(event)),
    []
  )
}
