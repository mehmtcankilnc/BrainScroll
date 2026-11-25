package com.mehmtcan.brainscroll.account

import android.app.Activity
import java.lang.ref.WeakReference

/** The activity that is on screen, which the phone's account chooser needs to show itself over. Set by the activity itself. */
object CurrentActivity {
    private var ref: WeakReference<Activity>? = null

    fun set(activity: Activity?) {
        ref = activity?.let { WeakReference(it) }
    }

    fun get(): Activity? = ref?.get()
}
