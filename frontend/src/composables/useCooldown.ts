import { computed, onBeforeUnmount, ref } from 'vue'

/** A countdown that blocks repeating an action (e.g. "Resend" is blocked for 60 s after each send). */
export function useCooldown(seconds: number) {
  const remaining = ref(0)
  let timer: ReturnType<typeof setInterval> | undefined

  const active = computed(() => remaining.value > 0)
  /** `m:ss`, e.g. `0:45`. */
  const label = computed(() => {
    const minutes = Math.floor(remaining.value / 60)
    const secs = String(remaining.value % 60).padStart(2, '0')
    return `${minutes}:${secs}`
  })

  function start(): void {
    stop()
    remaining.value = seconds
    timer = setInterval(() => {
      remaining.value -= 1
      if (remaining.value <= 0) {
        stop()
      }
    }, 1000)
  }

  function stop(): void {
    if (timer) {
      clearInterval(timer)
      timer = undefined
    }
    remaining.value = 0
  }

  onBeforeUnmount(stop)

  return { remaining, active, label, start, stop }
}
