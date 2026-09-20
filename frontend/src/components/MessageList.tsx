import { useLayoutEffect, useRef } from 'react'
import type { Message } from '../lib/chat'

type MessageListProps = {
  /** The conversation so far, oldest first. */
  messages: Message[]
  /** The reader's own id, to decide which side each message sits on. */
  myId?: number
  /** Named in the empty state, so an empty chat says who to talk to. */
  otherUsername: string
  /**
   * Whether to put a name above other people's messages. A group needs it —
   * the side a bubble sits on only says whether it is yours — while a chat
   * with one person would just repeat the name in the header on every line.
   */
  showSenders?: boolean
  loading: boolean
  /** Why the messages could not be loaded. Undefined means they could. */
  error?: string
  hasOlder: boolean
  loadingOlder: boolean
  onLoadOlder: () => void
}

/**
 * The scrolling middle of the chat. Besides drawing the messages, it owns the
 * two scroll corrections a chat needs, because both depend on measuring this
 * exact box.
 */
function MessageList({
  messages,
  myId,
  otherUsername,
  showSenders = false,
  loading,
  error,
  hasOlder,
  loadingOlder,
  onLoadOlder,
}: MessageListProps) {
  const scrollRef = useRef<HTMLDivElement>(null)
  // Where the view sat before older messages were asked for, so the reader can
  // be put back afterwards.
  const restoreTo = useRef<{ height: number; top: number } | null>(null)
  // Whether the reader is at the bottom. Someone reading back through history
  // should not be yanked down every time a message is added.
  const atBottom = useRef(true)

  function handleScroll() {
    const element = scrollRef.current
    if (!element) {
      return
    }

    atBottom.current = element.scrollHeight - element.scrollTop - element.clientHeight < 40

    // The position is only recorded when a load actually starts. Recording it
    // on a scroll that fetched nothing would apply it to the next unrelated
    // change — a sent message — and throw the view somewhere strange.
    if (element.scrollTop < 80 && hasOlder && !loadingOlder) {
      restoreTo.current = { height: element.scrollHeight, top: element.scrollTop }
      onLoadOlder()
    }
  }

  /**
   * Runs after the list changes but before the browser paints, so neither
   * correction below is ever visible as a jump.
   *
   * Adding older messages above makes the content taller, which would otherwise
   * push whatever was being read off the bottom of the view.
   */
  useLayoutEffect(() => {
    const element = scrollRef.current
    if (!element) {
      return
    }

    if (restoreTo.current) {
      const { height, top } = restoreTo.current
      element.scrollTop = element.scrollHeight - height + top
      restoreTo.current = null
    } else if (atBottom.current) {
      element.scrollTop = element.scrollHeight
    }
  }, [messages.length])

  return (
    // flex-1 takes whatever height the header and the input box leave, and
    // overflow-y-auto makes this the only part of the screen that scrolls.
    <div ref={scrollRef} onScroll={handleScroll} className="flex-1 overflow-y-auto px-6 py-4">
      <div className="mx-auto w-full max-w-2xl space-y-2">
        {loadingOlder && (
          <p className="py-2 text-center text-xs text-slate-500">Loading older messages…</p>
        )}

        {!hasOlder && messages.length > 0 && (
          <p className="py-2 text-center text-xs text-slate-400">This is the beginning.</p>
        )}

        {loading && <p className="py-2 text-center text-sm text-slate-500">Loading…</p>}

        {error && <p className="py-2 text-center text-sm text-red-600">{error}</p>}

        {!loading && !error && messages.length === 0 && (
          <p className="py-8 text-center text-sm text-slate-500">
            No messages yet. Say something to {otherUsername}.
          </p>
        )}

        {messages.map((message) => {
          const mine = message.senderId === myId
          return (
            <Bubble
              key={message.id}
              message={message}
              mine={mine}
              // Never above your own: you know who you are, and the bubble is
              // already on your side of the screen.
              showSender={showSenders && !mine}
            />
          )
        })}
      </div>
    </div>
  )
}

/**
 * One message. Yours sit on the right in dark, theirs on the left in white —
 * the side is what tells the two apart at a glance, the colour just reinforces
 * it.
 */
function Bubble({
  message,
  mine,
  showSender = false,
}: {
  message: Message
  mine: boolean
  showSender?: boolean
}) {
  return (
    <div className={mine ? 'flex justify-end' : 'flex justify-start'}>
      <div
        className={[
          'max-w-[75%] rounded-2xl px-4 py-2',
          mine ? 'bg-slate-900 text-white' : 'bg-white text-slate-900 shadow-sm',
        ].join(' ')}
      >
        {showSender && (
          <p className="mb-0.5 text-xs font-medium text-slate-500">{message.senderUsername}</p>
        )}

        {/* break-words stops one long unbroken string from widening the bubble
            past the screen; whitespace-pre-wrap keeps the sender's line breaks. */}
        <p className="whitespace-pre-wrap break-words text-sm">{message.body}</p>
        <p className="mt-1 text-[11px] text-slate-400">
          {new Date(message.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
        </p>
      </div>
    </div>
  )
}

export default MessageList
