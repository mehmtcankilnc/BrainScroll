package com.mehmtcan.brainscroll.account

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * The door the platforms use to hand a login link to the shared code. Android calls [deliver] from `MainActivity`
 * and iOS from its `onOpenURL`. A link that arrives before the app code is listening (a cold start through the
 * link) is kept until someone collects it.
 */
object DeepLinkInbox {
    private val _urls = MutableSharedFlow<String>(replay = 1, extraBufferCapacity = 8)
    val urls: SharedFlow<String> = _urls

    fun deliver(url: String) {
        _urls.tryEmit(url)
    }

    /** Called after a link was handled, so it is not handled again by a later collector. */
    fun consumed() {
        _urls.resetReplayCache()
    }
}
