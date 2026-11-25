// Checks the apple-token function with plain node: the client secret Apple wants (a real ES256 JWT), the two calls to Apple, and the
// rules of the function (who may call, link keeps a token, revoke revokes then forgets, and a refusal from Apple stops deletion).
// Run: npm test   (from this folder; needs node 22.18 or newer, which runs .ts files as they are)
import { generateKeyPairSync, verify } from 'node:crypto'
import { clientSecret, exchangeCode, revokeToken } from '../functions/apple-token/apple.ts'
import { handleAppleToken } from '../functions/apple-token/handler.ts'

let failures = 0
const check = (name, ok, extra = '') => { console.log((ok ? 'PASS ' : 'FAIL ') + name + (extra ? '  ' + extra : '')); if (!ok) failures++ }
const throws = async (fn) => { try { await fn(); return false } catch { return true } }

// A key like the .p8 file from Apple (PKCS8, P-256).
const { privateKey, publicKey } = generateKeyPairSync('ec', { namedCurve: 'P-256' })
const pem = privateKey.export({ type: 'pkcs8', format: 'pem' })
const cfg = { teamId: 'TEAM123456', keyId: 'KEY1234567', clientId: 'com.example.app', privateKeyPem: pem }
const b64urlJson = (s) => JSON.parse(Buffer.from(s, 'base64url').toString('utf8'))

// ---- the client secret ----
const jwt = await clientSecret(cfg, 1_800_000_000)
const [h, c, s] = jwt.split('.')
const header = b64urlJson(h)
const claims = b64urlJson(c)
check('the secret is an ES256 JWT with our key id', header.alg === 'ES256' && header.kid === cfg.keyId && header.typ === 'JWT', JSON.stringify(header))
check('it says who calls and for which app', claims.iss === cfg.teamId && claims.sub === cfg.clientId && claims.aud === 'https://appleid.apple.com', JSON.stringify(claims))
check('it is valid for five minutes', claims.iat === 1_800_000_000 && claims.exp === 1_800_000_300)
check('Apple could check its signature with our public key',
  verify('sha256', Buffer.from(`${h}.${c}`), { key: publicKey, dsaEncoding: 'ieee-p1363' }, Buffer.from(s, 'base64url')))
check('a key stored with \\n written out still works', (await clientSecret({ ...cfg, privateKeyPem: pem.replace(/\n/g, '\\n') })).split('.').length === 3)
check('a broken key is an error, not a bad secret', await throws(() => clientSecret({ ...cfg, privateKeyPem: 'not a key' })))

// ---- the calls to Apple ----
const seen = []
const apple = (status, body) => async (url, init) => { seen.push({ url, init }); return new Response(JSON.stringify(body), { status, headers: { 'content-type': 'application/json' } }) }
const form = (i) => Object.fromEntries(new URLSearchParams(seen[i].init.body))

check('a code becomes a refresh token', (await exchangeCode(cfg, 'one-time-code', apple(200, { refresh_token: 'r-1', access_token: 'a' }))) === 'r-1')
check('the exchange goes to Apple\'s token address with the code', seen[0].url === 'https://appleid.apple.com/auth/token'
  && form(0).code === 'one-time-code' && form(0).grant_type === 'authorization_code' && form(0).client_id === cfg.clientId && form(0).client_secret.split('.').length === 3)
check('Apple refusing the code is an error', await throws(() => exchangeCode(cfg, 'bad', apple(400, { error: 'invalid_grant' }))))
check('an answer without a refresh token is an error', await throws(() => exchangeCode(cfg, 'x', apple(200, { access_token: 'a' }))))

await revokeToken(cfg, 'r-1', apple(200, {}))
check('revoking goes to Apple\'s revoke address with the token', seen.at(-1).url === 'https://appleid.apple.com/auth/revoke'
  && form(seen.length - 1).token === 'r-1' && form(seen.length - 1).token_type_hint === 'refresh_token' && form(seen.length - 1).client_id === cfg.clientId)
check('Apple refusing to revoke is an error', await throws(() => revokeToken(cfg, 'r-1', apple(400, { error: 'invalid_request' }))))

// ---- the function ----
const USER = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'
const store = new Map()
let appleDown = false
let revoked = []
const deps = () => ({
  userIdOf: async (a) => (a === 'Bearer good' ? USER : null),
  saveToken: async (u, t) => { store.set(u, t) },
  loadToken: async (u) => store.get(u) ?? null,
  deleteToken: async (u) => { store.delete(u) },
  exchange: async (code) => { if (appleDown) throw new Error('apple down'); return `refresh-for-${code}` },
  revoke: async (t) => { if (appleDown) throw new Error('apple down'); revoked.push(t) },
})
const call = (body, headers = { authorization: 'Bearer good' }, method = 'POST') =>
  handleAppleToken(new Request('https://x/f', { method, headers, ...(method === 'POST' ? { body: typeof body === 'string' ? body : JSON.stringify(body) } : {}) }), deps())

let res = await call({ action: 'link', code: 'abcdefghij' }, {})
check('without a session the answer is 401', res.status === 401 && store.size === 0)
res = await call({ action: 'link', code: 'abcdefghij' }, { authorization: 'Bearer forged' })
check('a forged token is 401 too', res.status === 401 && store.size === 0)
res = await call({}, { authorization: 'Bearer good' }, 'GET')
check('only POST is accepted', res.status === 405)
res = await call('not json')
check('a body that is not json is refused (400)', res.status === 400)
res = await call({ action: 'dance' })
check('an unknown action is refused (400)', res.status === 400)
res = await call({ action: 'link', code: 'short' })
check('a missing or tiny code is refused (400)', res.status === 400 && store.size === 0)

res = await call({ action: 'link', code: 'abcdefghij' })
check('link keeps the refresh token for that player', res.status === 200 && store.get(USER) === 'refresh-for-abcdefghij')
res = await call({ action: 'link', code: 'klmnopqrst' })
check('a second sign-in replaces the kept token', res.status === 200 && store.get(USER) === 'refresh-for-klmnopqrst' && store.size === 1)

appleDown = true
res = await call({ action: 'link', code: 'uvwxyzabcd' })
check('Apple failing on link is 502 and keeps the old token', res.status === 502 && store.get(USER) === 'refresh-for-klmnopqrst')
res = await call({ action: 'revoke' })
check('Apple failing on revoke is 502, the token is kept, so deletion can wait', res.status === 502 && store.has(USER) && revoked.length === 0)
appleDown = false

res = await call({ action: 'revoke' })
check('revoke revokes the kept token and forgets it', res.status === 200 && (await res.json()).status === 'revoked'
  && revoked.length === 1 && revoked[0] === 'refresh-for-klmnopqrst' && !store.has(USER))
res = await call({ action: 'revoke' })
check('a player with no kept token has nothing to revoke (200, none)', res.status === 200 && (await res.json()).status === 'none' && revoked.length === 1)

console.log(failures ? `\n${failures} CHECK(S) FAILED` : '\nALL CHECKS PASSED')
process.exit(failures ? 1 : 0)
