package com.mehmtcan.brainscroll.ui.pro

/** The store this build sells through: it decides whose name the refund line of the store page carries. The desktop build has none. */
enum class StorePlatform { Apple, Google, None }

expect val storePlatform: StorePlatform
