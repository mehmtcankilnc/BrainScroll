package com.mehmtcan.brainscroll

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform