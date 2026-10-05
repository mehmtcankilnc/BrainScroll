package com.mehmtcan.brainscroll.account

/**
 * What came back through the login link, for example
 * `com.mehmtcan.brainscroll://login-callback?code=abc` on success or
 * `com.mehmtcan.brainscroll://login-callback?error=server_error&error_code=identity_already_exists&...` on failure.
 * Parameters can be in the query (`?`) or the fragment (`#`).
 */
data class CallbackUrl(
    val code: String?,
    val error: String?,
    val errorCode: String?,
    val errorDescription: String?,
) {
    val isError: Boolean get() = error != null || errorCode != null

    companion object {
        /** The server's code for "this Google/Apple account already belongs to another user". */
        const val IDENTITY_ALREADY_EXISTS = "identity_already_exists"
    }
}

/** Reads a login link. Returns null if [url] is not our login link (other scheme or host). */
fun parseCallbackUrl(url: String, scheme: String, host: String): CallbackUrl? {
    val prefix = "$scheme://$host"
    if (!url.startsWith(prefix, ignoreCase = true)) return null
    val rest = url.substring(prefix.length)
    // "com.x://login-callbackEVIL" must not match: the host has to end here.
    if (rest.isNotEmpty() && rest[0] != '/' && rest[0] != '?' && rest[0] != '#') return null

    val fragmentStart = rest.indexOf('#')
    val beforeFragment = if (fragmentStart >= 0) rest.substring(0, fragmentStart) else rest
    val fragment = if (fragmentStart >= 0) rest.substring(fragmentStart + 1) else ""
    val queryStart = beforeFragment.indexOf('?')
    val query = if (queryStart >= 0) beforeFragment.substring(queryStart + 1) else ""

    val params = (query.split('&') + fragment.split('&'))
        .filter { it.isNotEmpty() }
        .associate { pair ->
            val eq = pair.indexOf('=')
            if (eq < 0) percentDecode(pair) to "" else percentDecode(pair.substring(0, eq)) to percentDecode(pair.substring(eq + 1))
        }

    return CallbackUrl(
        code = params["code"],
        error = params["error"],
        errorCode = params["error_code"],
        errorDescription = params["error_description"],
    )
}

/** `%XX` escapes (UTF-8) and `+` for a space. Malformed escapes are kept as they are. */
internal fun percentDecode(text: String): String {
    if ('%' !in text && '+' !in text) return text
    val bytes = ArrayList<Byte>(text.length)
    var i = 0
    while (i < text.length) {
        val c = text[i]
        when {
            c == '+' -> { bytes += ' '.code.toByte(); i++ }
            c == '%' && i + 2 < text.length && hex(text[i + 1]) >= 0 && hex(text[i + 2]) >= 0 -> {
                bytes += (hex(text[i + 1]) * 16 + hex(text[i + 2])).toByte()
                i += 3
            }
            else -> { bytes += c.toString().encodeToByteArray().toList(); i++ }
        }
    }
    return bytes.toByteArray().decodeToString()
}

private fun hex(c: Char): Int = when (c) {
    in '0'..'9' -> c - '0'
    in 'a'..'f' -> c - 'a' + 10
    in 'A'..'F' -> c - 'A' + 10
    else -> -1
}
