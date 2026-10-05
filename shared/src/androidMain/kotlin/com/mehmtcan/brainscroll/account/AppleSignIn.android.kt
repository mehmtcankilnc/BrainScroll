package com.mehmtcan.brainscroll.account

/** Apple sign-in is native on iOS only. Android players use Google. */
actual fun platformAppleSignIn(): AppleSignIn? = null
