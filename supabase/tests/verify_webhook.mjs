// Checks the rules of the RevenueCat webhook (supabase/functions/revenuecat-webhook/handler.ts) with plain node:
// who may call it, what it ignores, what it adds, and that a database failure is retried by RevenueCat.
// Run: npm test   (from this folder; needs node 22.18 or newer, which runs .ts files as they are)
import { handleWebhook, PACKS } from '../functions/revenuecat-webhook/handler.ts'

let failures = 0
const check = (name, ok, extra = '') => { console.log((ok ? 'PASS ' : 'FAIL ') + name + (extra ? '  ' + extra : '')); if (!ok) failures++ }

const USER = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'
const calls = []
let dbDown = false
const deps = (secret = 'hunter2') => ({
  secret,
  credit: async (u, t, a) => {
    if (dbDown) throw new Error('db down')
    calls.push({ u, t, a })
    return { status: 'credited', balance: a }
  },
})
const post = (event, headers = { authorization: 'hunter2' }, d = deps()) =>
  handleWebhook(new Request('https://x/f', { method: 'POST', headers, body: JSON.stringify({ api_version: '1.0', event }) }), d)
const purchase = (extra = {}) => ({ type: 'NON_RENEWING_PURCHASE', app_user_id: USER, product_id: 'brainscroll_hints_20', transaction_id: '2000001', store: 'APP_STORE', ...extra })

check('the packs are 20 and 75', PACKS.brainscroll_hints_20 === 20 && PACKS.brainscroll_hints_75 === 75)

let res = await post(purchase())
check('a pack purchase adds its hints', res.status === 200 && calls.length === 1 && calls[0].u === USER && calls[0].a === 20 && calls[0].t === 'APP_STORE:2000001', JSON.stringify(calls))
res = await post(purchase({ product_id: 'brainscroll_hints_75', transaction_id: '2000002', store: 'PLAY_STORE' }))
check('the big pack adds 75', calls.length === 2 && calls[1].a === 75 && calls[1].t === 'PLAY_STORE:2000002')
res = await post(purchase({ app_user_id: USER.toUpperCase() }))
check('the account id is used in lower case', calls[2].u === USER)

const before = calls.length
check('the one-time Pro purchase is ignored', (await post(purchase({ product_id: 'brainscroll_pro' }))).status === 200 && calls.length === before)
check('a renewal, a refund and a test event are ignored', (await Promise.all(['INITIAL_PURCHASE', 'CANCELLATION', 'TEST', 'RENEWAL'].map(t => post(purchase({ type: t }))))).every(r => r.status === 200) && calls.length === before)
check('a player without an account id (RevenueCat anonymous) is ignored, not retried',
  (await post(purchase({ app_user_id: '$RCAnonymousID:abc123' }))).status === 200 && calls.length === before)

check('no secret header is refused (401)', (await post(purchase(), {})).status === 401 && calls.length === before)
check('a wrong secret is refused (401)', (await post(purchase(), { authorization: 'hunter3' })).status === 401
  && (await post(purchase(), { authorization: 'hunter22' })).status === 401 && calls.length === before)
check('the secret may come with Bearer', (await post(purchase({ transaction_id: '2000003' }), { authorization: 'Bearer hunter2' })).status === 200 && calls.length === before + 1)
check('with no secret set, everything is refused (500)', (await post(purchase(), { authorization: 'anything' }, deps(null))).status === 500 && calls.length === before + 1)
check('only POST is accepted', (await handleWebhook(new Request('https://x/f'), deps())).status === 405)
check('a body that is not json is refused (400)', (await handleWebhook(new Request('https://x/f', { method: 'POST', headers: { authorization: 'hunter2' }, body: 'nope' }), deps())).status === 400)
check('a body with no event is refused (400)', (await handleWebhook(new Request('https://x/f', { method: 'POST', headers: { authorization: 'hunter2' }, body: '{}' }), deps())).status === 400)
check('a purchase with no transaction id is refused (400)', (await post(purchase({ transaction_id: undefined, id: undefined }))).status === 400)

dbDown = true
check('a database failure answers 500, so RevenueCat sends it again', (await post(purchase({ transaction_id: '2000004' }))).status === 500)

console.log(failures === 0 ? '\nALL CHECKS PASSED' : `\n${failures} CHECK(S) FAILED`)
process.exit(failures === 0 ? 0 : 1)
