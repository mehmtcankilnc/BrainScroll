// The two calls to Apple that Sign in with Apple needs for account deletion, with nothing from Deno or Supabase in them so they
// can be tested with plain node (supabase/tests/verify_apple_token.mjs). Apple's rules: developer.apple.com/documentation/sign_in_with_apple
//
// Both calls are made with a "client secret": a short JWT signed with the .p8 key from Apple Developer > Keys (ES256), which says
// the call comes from our team, with our key, for our app.

export interface AppleConfig {
  /** Apple Developer > Membership: the 10 character Team ID. */
  teamId: string
  /** The Key ID of the "Sign in with Apple" key. */
  keyId: string
  /** The app's bundle id: the native sign-in uses it as the client id. */
  clientId: string
  /** The contents of the AuthKey_XXXX.p8 file. */
  privateKeyPem: string
}

const APPLE = 'https://appleid.apple.com'

const b64url = (bytes: Uint8Array | string): string => {
  const data = typeof bytes === 'string' ? new TextEncoder().encode(bytes) : bytes
  let s = ''
  for (const b of data) s += String.fromCharCode(b)
  return btoa(s).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
}

async function signingKey(pem: string): Promise<CryptoKey> {
  // The secret may have been stored with the line breaks written as \n.
  const body = pem.replace(/\\n/g, '\n').replace(/-----(BEGIN|END) PRIVATE KEY-----/g, '').replace(/\s+/g, '')
  const der = Uint8Array.from(atob(body), (c) => c.charCodeAt(0))
  return await crypto.subtle.importKey('pkcs8', der, { name: 'ECDSA', namedCurve: 'P-256' }, false, ['sign'])
}

/** The JWT Apple wants as client_secret. Valid for five minutes: it is made fresh for every call. */
export async function clientSecret(cfg: AppleConfig, nowSeconds: number = Math.floor(Date.now() / 1000)): Promise<string> {
  const header = b64url(JSON.stringify({ alg: 'ES256', kid: cfg.keyId, typ: 'JWT' }))
  const claims = b64url(JSON.stringify({ iss: cfg.teamId, iat: nowSeconds, exp: nowSeconds + 300, aud: APPLE, sub: cfg.clientId }))
  const signed = `${header}.${claims}`
  // WebCrypto gives the signature as the plain r||s pair, which is the form a JWT uses.
  const signature = new Uint8Array(await crypto.subtle.sign({ name: 'ECDSA', hash: 'SHA-256' }, await signingKey(cfg.privateKeyPem), new TextEncoder().encode(signed)))
  return `${signed}.${b64url(signature)}`
}

async function postForm(path: string, fields: Record<string, string>, fetchFn: typeof fetch): Promise<Response> {
  return await fetchFn(`${APPLE}${path}`, {
    method: 'POST',
    headers: { 'content-type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams(fields).toString(),
  })
}

/** Turns the one-time authorization code from the sign-in into a refresh token. Throws if Apple refuses. */
export async function exchangeCode(cfg: AppleConfig, code: string, fetchFn: typeof fetch = fetch): Promise<string> {
  const res = await postForm('/auth/token', {
    client_id: cfg.clientId,
    client_secret: await clientSecret(cfg),
    code,
    grant_type: 'authorization_code',
  }, fetchFn)
  const body = await res.json().catch(() => ({}))
  if (!res.ok || typeof body.refresh_token !== 'string' || !body.refresh_token) {
    throw new Error(`apple token ${res.status}: ${String(body.error ?? 'no refresh token')}`)
  }
  return body.refresh_token
}

/** Disconnects the account from the app at Apple. Throws if Apple refuses (the caller must not go on to delete the account). */
export async function revokeToken(cfg: AppleConfig, refreshToken: string, fetchFn: typeof fetch = fetch): Promise<void> {
  const res = await postForm('/auth/revoke', {
    client_id: cfg.clientId,
    client_secret: await clientSecret(cfg),
    token: refreshToken,
    token_type_hint: 'refresh_token',
  }, fetchFn)
  if (!res.ok) throw new Error(`apple revoke ${res.status}`)
}
