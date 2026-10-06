package com.mehmtcan.brainscroll.social

import com.mehmtcan.brainscroll.account.percentDecode

/** The web link. It opens the app when installed (universal link / app link) and a page with the code when not. */
private const val WEB_INVITE_PREFIX = "https://playbrainscroll.com/invite"

/** The same invite with the app's own scheme. The page's "Open in the app" button uses it. */
private const val APP_INVITE_PREFIX = "com.mehmtcan.brainscroll://invite"

/** The link that carries an invite code: `https://playbrainscroll.com/invite?code=ABCD2345`. */
fun inviteLink(code: String): String = "$WEB_INVITE_PREFIX?code=$code"

/** The code inside an invite link (web or app scheme), or null if [url] is not one (the login link, for example). */
fun parseInviteCode(url: String): String? {
    val prefix = listOf(WEB_INVITE_PREFIX, APP_INVITE_PREFIX).firstOrNull { url.startsWith(it, ignoreCase = true) } ?: return null
    val rest = url.substring(prefix.length)
    // "…/inviteEVIL" must not match: the path has to end here.
    if (rest.isNotEmpty() && rest[0] != '/' && rest[0] != '?' && rest[0] != '#') return null
    val query = rest.substringAfter('?', "").substringBefore('#')
    val code = query.split('&')
        .map { it.substringBefore('=') to it.substringAfter('=', "") }
        .firstOrNull { it.first == "code" }
        ?.second
        ?.let(::percentDecode)
        ?.trim()
        ?.uppercase()
    return code?.takeIf { it.isNotEmpty() }
}
