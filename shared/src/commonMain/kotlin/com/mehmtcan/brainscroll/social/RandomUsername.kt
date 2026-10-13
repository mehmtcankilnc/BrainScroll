package com.mehmtcan.brainscroll.social

import kotlin.random.Random

/**
 * A name for a player who has not chosen one, like "redpanda051": a color, an animal and three digits. It follows the
 * rules of [isValidUsername] (at most 16 letters and digits: the longest color and animal and the digits make 16). The
 * player can change it later.
 */
object RandomUsername {
    private val colors = listOf(
        "red", "blue", "green", "gold", "pink", "teal", "gray", "lime", "navy", "mint", "rose", "jade", "ruby", "sky", "sand",
        "plum", "peach", "amber", "coral", "ivory", "olive", "azure",
    )
    private val animals = listOf(
        "panda", "fox", "owl", "wolf", "otter", "koala", "tiger", "lion", "bear", "hawk", "seal", "lynx", "deer", "crow", "swan",
        "hare", "ibis", "gecko", "moose", "bison", "heron", "finch", "whale", "camel",
    )

    fun next(random: Random = Random.Default): String =
        colors.random(random) + animals.random(random) + random.nextInt(1000).toString().padStart(3, '0')
}
