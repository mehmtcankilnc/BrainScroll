package com.mehmtcan.brainscroll.account

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CallbackUrlTest {

    private fun parse(url: String) = parseCallbackUrl(url, "com.mehmtcan.brainscroll", "login-callback")

    @Test
    fun aSuccessfulLoginCarriesACode() {
        val url = parse("com.mehmtcan.brainscroll://login-callback?code=abc123")!!
        assertEquals("abc123", url.code)
        assertFalse(url.isError)
    }

    @Test
    fun anErrorCarriesItsCodeAndADecodedDescription() {
        val url = parse(
            "com.mehmtcan.brainscroll://login-callback?error=server_error&error_code=identity_already_exists" +
                "&error_description=Identity+is+already+linked+to+another+user",
        )!!
        assertTrue(url.isError)
        assertEquals(CallbackUrl.IDENTITY_ALREADY_EXISTS, url.errorCode)
        assertEquals("Identity is already linked to another user", url.errorDescription)
        assertNull(url.code)
    }

    @Test
    fun parametersInTheFragmentAreReadToo() {
        val url = parse("com.mehmtcan.brainscroll://login-callback#error=access_denied&error_code=bad_oauth_callback")!!
        assertEquals("access_denied", url.error)
        assertEquals("bad_oauth_callback", url.errorCode)
    }

    @Test
    fun aTrailingSlashBeforeTheQueryIsAccepted() {
        assertEquals("x", parse("com.mehmtcan.brainscroll://login-callback/?code=x")!!.code)
    }

    @Test
    fun otherSchemesAndHostsAreNotOurs() {
        assertNull(parse("https://evil.example/login-callback?code=x"))
        assertNull(parse("com.mehmtcan.brainscroll://other?code=x"))
        assertNull(parse("com.mehmtcan.brainscroll://login-callbackEVIL?code=x"))
        assertNull(parse("something-else"))
    }

    @Test
    fun theSchemeAndHostMatchRegardlessOfCase() {
        assertEquals("x", parse("COM.MEHMTCAN.BRAINSCROLL://LOGIN-CALLBACK?code=x")!!.code)
    }

    @Test
    fun aLinkWithoutParametersIsOursButHasNothingInIt() {
        val url = parse("com.mehmtcan.brainscroll://login-callback")!!
        assertNull(url.code)
        assertFalse(url.isError)
    }

    @Test
    fun percentEscapesAreDecodedAsUtf8() {
        assertEquals("çiçek", percentDecode("%C3%A7i%C3%A7ek"))
        assertEquals("a b", percentDecode("a+b".replace('+', ' ')))
        assertEquals("a b", percentDecode("a%20b"))
    }

    @Test
    fun brokenEscapesAreKeptAsTheyAre() {
        assertEquals("100%", percentDecode("100%"))
        assertEquals("%zz", percentDecode("%zz"))
        assertEquals("%4", percentDecode("%4"))
    }
}
