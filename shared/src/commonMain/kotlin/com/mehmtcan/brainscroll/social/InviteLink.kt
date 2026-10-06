package com.mehmtcan.brainscroll.social

import com.mehmtcan.brainscroll.account.percentDecode

private const val INVITE_PREFIX = "com.mehmtcan.brainscroll://invite"

/** The link that carries an invite code: `com.mehmtcan.brainscroll://invite?code=ABCD2345`. */
fun inviteLink(code: String): String = "$INVITE_PREFIX?code=$code"

/** The code inside an invite link, or null if [url] is not an invite link (the login link, for example). */
fun parseInviteCode(url: String): String? {
    if (!url.startsWith(INVITE_PREFIX, ignoreCase = true)) return null
    val rest = url.substring(INVITE_PREFIX.length)
    // "…://inviteEVIL" must not match: the host has to end here.
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
