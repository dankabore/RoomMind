import { Client, type StompSubscription } from '@stomp/stompjs'
import { api } from './api'
import { getToken } from './auth'

/**
 * The one live connection to the backend, shared by every screen for as long as
 * you are signed in.
 *
 * The connection speaks STOMP, a small protocol that runs inside a websocket.
 * @stomp/stompjs is the library that speaks it: it writes and reads the frames,
 * and when the connection drops it opens a new one on its own, every five
 * seconds until one succeeds.
 *
 * What it does not do is remember what you were subscribed to. A new connection
 * starts with no subscriptions at all, so this file keeps its own list of what
 * each open screen wants to hear, and subscribes to all of it again every time
 * a connection comes up.
 */

const API_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080'

type Listener = {
  destination: string
  onMessage: (body: string) => void
  onReconnect: () => void
  /** The live subscription, or null while there is no connection to hold one. */
  subscription: StompSubscription | null
}

const listeners = new Set<Listener>()

const client = new Client({
  // http://localhost:8080 becomes ws://localhost:8080/ws.
  brokerURL: API_URL.replace(/^http/, 'ws') + '/ws',

  // Runs before every attempt, reconnects included, so each one carries the
  // token stored right now rather than whichever one existed when the page
  // loaded. With no token there is nobody to connect as, so it stops trying.
  beforeConnect: async (stomp) => {
    const token = getToken()
    if (!token) {
      await stomp.deactivate()
      return
    }
    stomp.connectHeaders = { Authorization: `Bearer ${token}` }
  },

  onConnect: () => {
    for (const listener of listeners) {
      listener.subscription = subscribeNow(listener)
      // Anything sent while this screen had no connection never reached it.
      listener.onReconnect()
    }
  },

  onWebSocketClose: () => {
    // The subscriptions died with the connection. Forgetting them here stops
    // anything from trying to unsubscribe over a connection that is gone.
    for (const listener of listeners) {
      listener.subscription = null
    }
  },

  // The backend refused something and is about to close the connection. It
  // does not say why, and the likely reason is an expired token. Asking the API
  // who we are settles it: a 401 there goes through the same "send them back to
  // login" handling as any other request. If the token is fine, nothing happens
  // and the library simply connects again.
  onStompError: () => {
    api.get('/api/auth/me').catch(() => {})
  },
})

/** Opens the connection. Calling it again while it is already open does nothing. */
export function connectSocket(): void {
  client.activate()
}

/** Closes the connection and stops it reconnecting. Used on logout. */
export function disconnectSocket(): void {
  void client.deactivate()
}

/**
 * Listens to one address until the returned function is called.
 *
 * If there is no connection yet, the listener waits in the list and is
 * subscribed as soon as there is. `onReconnect` runs each time that happens,
 * so the caller can fetch whatever it missed while it was waiting.
 */
export function subscribe(
  destination: string,
  onMessage: (body: string) => void,
  onReconnect: () => void,
): () => void {
  const listener: Listener = { destination, onMessage, onReconnect, subscription: null }
  listeners.add(listener)

  if (client.connected) {
    listener.subscription = subscribeNow(listener)
  }

  return () => {
    listeners.delete(listener)
    if (listener.subscription && client.connected) {
      listener.subscription.unsubscribe()
    }
  }
}

function subscribeNow(listener: Listener): StompSubscription {
  return client.subscribe(listener.destination, (frame) => listener.onMessage(frame.body))
}
