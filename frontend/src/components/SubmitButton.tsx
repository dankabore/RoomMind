import { useEffect, useRef, useState, type MouseEvent, type ReactNode } from 'react'

type SubmitButtonProps = {
  submitting: boolean
  /** What the button says while the request is in flight. */
  busyLabel: string
  /** True while the form is not filled in enough to be worth sending. */
  incomplete?: boolean
  children: ReactNode
}

/** Where the button sits inside its lane, in percentages of that lane. */
const CENTRE = { x: 50, y: 50 }

/** How close the pointer may get, in the same percentage units, before it bolts. */
const PERSONAL_SPACE = 35

/**
 * The button at the foot of a form. It goes flat for two different reasons: a
 * request is already in flight, so a second click would send a second one, or
 * the form is not filled in yet and there is nothing worth sending.
 *
 * In that second case it also shrinks and dodges the pointer. The chase is the
 * joke; the field messages underneath are what actually explain the problem, so
 * nobody is left guessing why the button will not stay still.
 */
function SubmitButton({ submitting, busyLabel, incomplete = false, children }: SubmitButtonProps) {
  const lane = useRef<HTMLDivElement>(null)
  const [position, setPosition] = useState(CENTRE)

  // Once the form is filled in the button has no reason to keep hiding in a
  // corner, so it walks back to the middle and stays there.
  useEffect(() => {
    if (!incomplete) {
      setPosition(CENTRE)
    }
  }, [incomplete])

  // A disabled button fires no mouse events of its own, which is why this
  // listens on the lane around it and works out the distance itself.
  function handleMouseMove(event: MouseEvent<HTMLDivElement>) {
    if (!incomplete || submitting || lane.current === null) {
      return
    }

    const area = lane.current.getBoundingClientRect()
    const pointer = {
      x: ((event.clientX - area.left) / area.width) * 100,
      y: ((event.clientY - area.top) / area.height) * 100,
    }

    const distance = Math.hypot(pointer.x - position.x, pointer.y - position.y)
    if (distance > PERSONAL_SPACE) {
      return
    }

    // Away from whichever side the pointer came from, with enough randomness
    // that it cannot be cornered by approaching from the same angle twice.
    setPosition({
      x: pointer.x < 50 ? 72 + Math.random() * 20 : 8 + Math.random() * 20,
      y: pointer.y < 50 ? 70 + Math.random() * 20 : 10 + Math.random() * 20,
    })
  }

  return (
    // A fixed-height lane, so the form below does not shuffle about as the
    // button changes size or runs off to a corner.
    <div ref={lane} className="relative h-12" onMouseMove={handleMouseMove}>
      {incomplete ? (
        <button
          type="submit"
          disabled
          style={{ left: `${position.x}%`, top: `${position.y}%` }}
          className="absolute -translate-x-1/2 -translate-y-1/2 cursor-not-allowed rounded-lg bg-slate-900 px-3 py-1 text-xs font-medium text-white opacity-50 transition-all duration-200 ease-out"
        >
          {children}
        </button>
      ) : (
        <button
          type="submit"
          disabled={submitting}
          className="absolute inset-x-0 top-1/2 -translate-y-1/2 rounded-lg bg-slate-900 px-4 py-2 font-medium text-white transition-all duration-200 hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-50 disabled:hover:bg-slate-900"
        >
          {submitting ? busyLabel : children}
        </button>
      )}
    </div>
  )
}

export default SubmitButton
