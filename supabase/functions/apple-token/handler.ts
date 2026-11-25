// The rules of the apple-token function, with nothing from Deno or Supabase in them so they can be tested with plain node
// (supabase/tests/verify_apple_token.mjs). index.ts wires them to the real request, the database and Apple.
//
// Two actions, both for the signed-in player only:
//   link    after "Sign in with Apple": the app sends the one-time authorization code; it becomes a refresh token that is kept.
//   revoke  before the account is deleted: the kept token is revoked at Apple, then forgotten. If Apple refuses, the answer is an
//           error and the app does NOT delete the account (so no account is left half connected). No kept token (a player who
//           signed in with Apple before this existed) is not an error: there is nothing to revoke.

export interface Deps {
  /** The verified user id behind the Authorization header, or null when it is missing or not valid. */
  userIdOf: (authorization: string | null) => Promise<string | null>
  saveToken: (userId: string, refreshToken: string) => Promise<void>
  loadToken: (userId: string) => Promise<string | null>
  deleteToken: (userId: string) => Promise<void>
  /** Apple: authorization code to refresh token. Throws when Apple refuses. */
  exchange: (code: string) => Promise<string>
  /** Apple: revoke a refresh token. Throws when Apple refuses. */
  revoke: (refreshToken: string) => Promise<void>
}

const json = (status: number, body: unknown) =>
  new Response(JSON.stringify(body), { status, headers: { 'content-type': 'application/json' } })

/** 200 done (or nothing to do), 400 a bad request, 401 not signed in, 502 Apple or the database said no (the app may try again). */
export async function handleAppleToken(req: Request, deps: Deps): Promise<Response> {
  if (req.method !== 'POST') return json(405, { error: 'POST only' })

  const userId = await deps.userIdOf(req.headers.get('authorization')).catch(() => null)
  if (!userId) return json(401, { error: 'unauthorized' })

  let body: any
  try {
    body = await req.json()
  } catch {
    return json(400, { error: 'body is not json' })
  }

  if (body?.action === 'link') {
    const code = body.code
    if (typeof code !== 'string' || code.length < 10 || code.length > 2000) return json(400, { error: 'no authorization code' })
    try {
      await deps.saveToken(userId, await deps.exchange(code))
    } catch (e) {
      console.error('apple link failed', e)
      return json(502, { error: 'apple_link_failed' })
    }
    return json(200, { status: 'linked' })
  }

  if (body?.action === 'revoke') {
    try {
      const token = await deps.loadToken(userId)
      if (!token) return json(200, { status: 'none' })
      await deps.revoke(token)
      await deps.deleteToken(userId)
    } catch (e) {
      console.error('apple revoke failed', e)
      return json(502, { error: 'apple_revoke_failed' })
    }
    return json(200, { status: 'revoked' })
  }

  return json(400, { error: 'unknown action' })
}
