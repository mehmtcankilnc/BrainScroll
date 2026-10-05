package com.mehmtcan.brainscroll.ui.feed

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Stops the feed from moving to the next page while [blocked] is true, so the player never sees the page
 * slide away and bounce back. Moving backwards stays possible.
 *
 * Pointer events travel parent -> child first (the Initial pass). Consuming a forward drag here means the
 * pager below never sees it as a drag, so it does not scroll. A small movement is let through, so taps on
 * keys (which always wiggle a little) still work. [onAttempt] is called once per blocked gesture.
 */
fun Modifier.blockForwardScroll(blocked: Boolean, onAttempt: () -> Unit): Modifier = this
    // Touch / mouse drag: a finger moving up means "go to the next page".
    .pointerInput(blocked) {
        if (!blocked) return@pointerInput
        val slop = viewConfiguration.touchSlop
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            var movedUp = 0f
            var notified = false
            do {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull() ?: break
                movedUp -= change.position.y - change.previousPosition.y
                if (movedUp > slop) {
                    event.changes.forEach { it.consume() }
                    if (!notified) {
                        notified = true
                        onAttempt()
                    }
                }
            } while (event.changes.any { it.pressed })
        }
    }
    // Mouse wheel (desktop): scrolling down means "go to the next page".
    .pointerInput(blocked) {
        if (!blocked) return@pointerInput
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (event.type == PointerEventType.Scroll && event.changes.any { it.scrollDelta.y > 0f }) {
                    event.changes.forEach { it.consume() }
                    onAttempt()
                }
            }
        }
    }
