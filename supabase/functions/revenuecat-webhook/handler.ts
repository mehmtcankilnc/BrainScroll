// The rules of the RevenueCat webhook, with nothing from Deno or Supabase in them so they can be tested with plain node
// (supabase/tests/verify_webhook.mjs). index.ts wires them to the real request, secrets and database.
//
// RevenueCat reports every purchase here. A hint pack is a consumable, which RevenueCat reports as NON_RENEWING_PURCHASE
// (so is the one-time Pro; it is not in PACKS and is ignored here, Pro is read from RevenueCat by the app itself).
// The webhook adds the hints to the player's balance on the server (public.credit_hints). The app never adds anything.

/** Hints in each pack, by store product id (docs/pro-plan.md section 11). The same ids as in ProConfig.kt and in both stores. */
export const PACKS: Record<string, number> = {
  brainscroll_hints_20: 20,
  brainscroll_hints_75: 75,
}

export type CreditResult = { status: 'credited' | 'duplicate' | 'unknown_user'; balance?: number | null }

export interface Deps {
  /** The secret RevenueCat is told to send in the Authorization header. Unset: every request is refused. */
  secret: string | undefined
  /** Calls public.credit_hints with the service role. Throws when the database could not be reached or said no. */
  credit: (userId: string, transactionId: string, amount: number) => Promise<CreditResult>
}

const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i

const json = (status: number, body: unknown) =>
  new Response(JSON.stringify(body), { status, headers: { 'content-type': 'application/json' } })

/** Same answer in the same time whether the first or the last character is wrong. */
function sameSecret(given: string, expected: string): boolean {
  const a = new TextEncoder().encode(given)
  const b = new TextEncoder().encode(expected)
  let diff = a.length ^ b.length
  const n = Math.max(a.length, b.length)
  for (let i = 0; i < n; i++) diff |= (a[i] ?? 0) ^ (b[i] ?? 0)
  return diff === 0
}

/**
 * 200: handled, or nothing to do (RevenueCat must not retry these). 401: wrong or missing secret. 500: the database
 * failed, so RevenueCat tries again later (adding is idempotent, a retry cannot add twice).
 */
export async function handleWebhook(req: Request, deps: Deps): Promise<Response> {
  if (req.method !== 'POST') return json(405, { error: 'POST only' })
  if (!deps.secret) return json(500, { error: 'webhook secret is not set' })

  const given = (req.headers.get('authorization') ?? '').replace(/^Bearer\s+/i, '')
  if (!sameSecret(given, deps.secret.replace(/^Bearer\s+/i, ''))) return json(401, { error: 'unauthorized' })

  let body: any
  try {
    body = await req.json()
  } catch {
    return json(400, { error: 'not json' })
  }
  const event = body?.event
  if (!event || typeof event !== 'object') return json(400, { error: 'no event' })

  if (event.type !== 'NON_RENEWING_PURCHASE') return json(200, { status: 'ignored', reason: 'event type' })
  // Google Play can send the product id with the purchase option after a colon ("brainscroll_hints_20:buy"); the pack is the part before it.
  const amount = PACKS[String(event.product_id ?? '').split(':')[0]]
  if (!amount) return json(200, { status: 'ignored', reason: 'not a hint pack' })

  const user = String(event.app_user_id ?? '')
  if (!UUID.test(user)) {
    // A purchase made before the app told RevenueCat who the player is. The app does not sell packs before that; if it ever
    // happens there is nobody to credit, and a retry would not help.
    console.error('hint pack bought by a player without an account id', event.transaction_id)
    return json(200, { status: 'ignored', reason: 'no account' })
  }
  const transaction = String(event.transaction_id ?? event.id ?? '')
  if (!transaction) return json(400, { error: 'no transaction id' })

  try {
    const result = await deps.credit(user.toLowerCase(), `${event.store ?? 'store'}:${transaction}`, amount)
    return json(200, { status: result.status })
  } catch (e) {
    console.error('credit_hints failed', e)
    return json(500, { error: 'could not add the hints' })
  }
}
