// RevenueCat webhook: adds a bought hint pack to the player's balance (docs/pro-plan.md section 11, Asama B).
// The rules are in handler.ts. Secrets (never in the repository), set once with `supabase secrets set`:
//   REVENUECAT_WEBHOOK_SECRET  the same value as "Authorization header value" in RevenueCat > Integrations > Webhooks
// SUPABASE_URL and SUPABASE_SERVICE_ROLE_KEY are provided to every function by Supabase itself.
import { handleWebhook, type CreditResult } from './handler.ts'

declare const Deno: {
  env: { get(name: string): string | undefined }
  serve(handler: (req: Request) => Response | Promise<Response>): void
}

const url = Deno.env.get('SUPABASE_URL')
const serviceKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')

async function credit(userId: string, transactionId: string, amount: number): Promise<CreditResult> {
  const res = await fetch(`${url}/rest/v1/rpc/credit_hints`, {
    method: 'POST',
    headers: { apikey: serviceKey!, authorization: `Bearer ${serviceKey}`, 'content-type': 'application/json' },
    body: JSON.stringify({ p_user: userId, p_transaction: transactionId, p_amount: amount }),
  })
  if (!res.ok) throw new Error(`credit_hints ${res.status}: ${await res.text()}`)
  return await res.json()
}

Deno.serve((req) => handleWebhook(req, { secret: Deno.env.get('REVENUECAT_WEBHOOK_SECRET'), credit }))
