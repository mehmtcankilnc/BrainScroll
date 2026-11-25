// apple-token: keeps and revokes the Sign in with Apple token of a player (App Store guideline 5.1.1(v)).
// The rules are in handler.ts, the calls to Apple in apple.ts. Secrets (never in the repository), set once with `supabase secrets set`:
//   APPLE_TEAM_ID      Apple Developer > Membership > Team ID
//   APPLE_KEY_ID       Apple Developer > Keys: the Key ID of a key with "Sign in with Apple" turned on
//   APPLE_PRIVATE_KEY  the whole content of that key's AuthKey_XXXX.p8 file (the BEGIN/END lines included)
//   APPLE_CLIENT_ID    optional, the app's bundle id; defaults to com.mehmtcan.brainscroll
// SUPABASE_URL and SUPABASE_SERVICE_ROLE_KEY are provided to every function by Supabase itself.
import { exchangeCode, revokeToken, type AppleConfig } from './apple.ts'
import { handleAppleToken } from './handler.ts'

declare const Deno: {
  env: { get(name: string): string | undefined }
  serve(handler: (req: Request) => Response | Promise<Response>): void
}

const url = Deno.env.get('SUPABASE_URL')!
const serviceKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!

const apple = (): AppleConfig => ({
  teamId: Deno.env.get('APPLE_TEAM_ID') ?? '',
  keyId: Deno.env.get('APPLE_KEY_ID') ?? '',
  clientId: Deno.env.get('APPLE_CLIENT_ID') ?? 'com.mehmtcan.brainscroll',
  privateKeyPem: Deno.env.get('APPLE_PRIVATE_KEY') ?? '',
})

const rest = (path: string, init: RequestInit = {}) =>
  fetch(`${url}/rest/v1/apple_token${path}`, {
    ...init,
    headers: { apikey: serviceKey, authorization: `Bearer ${serviceKey}`, 'content-type': 'application/json', ...init.headers },
  })

Deno.serve((req) =>
  handleAppleToken(req, {
    userIdOf: async (authorization) => {
      if (!authorization) return null
      const res = await fetch(`${url}/auth/v1/user`, { headers: { apikey: serviceKey, authorization } })
      if (!res.ok) return null
      const user = await res.json()
      return typeof user?.id === 'string' ? user.id : null
    },
    saveToken: async (userId, refreshToken) => {
      const res = await rest('?on_conflict=user_id', {
        method: 'POST',
        headers: { prefer: 'resolution=merge-duplicates,return=minimal' },
        body: JSON.stringify({ user_id: userId, refresh_token: refreshToken }),
      })
      if (!res.ok) throw new Error(`save token ${res.status}: ${await res.text()}`)
    },
    loadToken: async (userId) => {
      const res = await rest(`?user_id=eq.${encodeURIComponent(userId)}&select=refresh_token`)
      if (!res.ok) throw new Error(`load token ${res.status}: ${await res.text()}`)
      const rows = await res.json()
      return rows[0]?.refresh_token ?? null
    },
    deleteToken: async (userId) => {
      const res = await rest(`?user_id=eq.${encodeURIComponent(userId)}`, { method: 'DELETE', headers: { prefer: 'return=minimal' } })
      if (!res.ok) throw new Error(`delete token ${res.status}: ${await res.text()}`)
    },
    exchange: (code) => exchangeCode(apple(), code),
    revoke: (token) => revokeToken(apple(), token),
  }),
)
