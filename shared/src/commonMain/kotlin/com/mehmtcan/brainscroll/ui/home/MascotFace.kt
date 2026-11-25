package com.mehmtcan.brainscroll.ui.home

/**
 * One face of the mascot, as numbers: the same numbers the difficulty slider moves, plus a few more for the moods the slider does
 * not need (a tear, closed happy eyes, an open mouth). Any two faces can be blended with [lerp], which is how the mascot goes from
 * one mood to the next without a jump. The numbers are in the 120 x 120 drawing of the mascot ([BrainMascot]).
 */
data class MascotFace(
    /** How far the upper lid has come down over the eye: 0 open, 1 closed. */
    val lid: Float = 0f,
    /** Radius of the pupil. Small pupils read as startled, big ones as soft. */
    val pupil: Float = 4f,
    /** Height of the inner and the outer end of each brow (smaller is higher), and how far the middle of it bows up. */
    val browInner: Float = 50f,
    val browOuter: Float = 54f,
    val browLift: Float = -5f,
    /** The blush on the cheeks: 0 none, 1 the usual, more for a flushed face. */
    val cheek: Float = 1f,
    /** The mouth: the height of its two ends, how far its middle bows down (smile) or up (frown), a crooked end, and half its width. */
    val mouthEnd: Float = 77f,
    val mouthControl: Float = 88f,
    val mouthTilt: Float = 0f,
    val mouthHalf: Float = 8f,
    /** How far the lower lip drops: 0 a closed mouth (a line), more an open one. */
    val mouthOpen: Float = 0f,
    /** A drop of sweat by the head (0 to 1), and a tear under the left eye (0 to 1). */
    val sweat: Float = 0f,
    val tear: Float = 0f,
    /** The eyes turned into happy arcs: 0 round eyes, 1 closed smiling ones. */
    val eyeArc: Float = 0f,
    /** The tongue in an open mouth: 0 none, 1 shown. */
    val tongue: Float = 0f,
) {
    companion object {
        /** The usual smile: the easy face of the difficulty slider. */
        val Happy = MascotFace()

        /** Wide eyes with small pupils, brows up high, a small round mouth. */
        val Surprised = MascotFace(
            pupil = 2.7f, browInner = 45f, browOuter = 45f, browLift = -8f, cheek = 0.7f,
            mouthEnd = 79f, mouthControl = 73f, mouthHalf = 5.8f, mouthOpen = 9.5f,
        )

        /** Smiling closed eyes, high brows, a wide open smile and flushed cheeks: the face for good news. */
        val Joy = MascotFace(
            browInner = 48f, browOuter = 51f, browLift = -6f, cheek = 1.5f,
            mouthEnd = 74f, mouthControl = 76f, mouthHalf = 10.5f, mouthOpen = 13f, eyeArc = 1f, tongue = 1f,
        )

        /** Drooping lids, brows pulled up in the middle, a frown, and a tear. */
        val Sad = MascotFace(
            lid = 0.2f, pupil = 4.4f, browInner = 45f, browOuter = 57f, browLift = -1f, cheek = 0.2f,
            mouthEnd = 84f, mouthControl = 75f, mouthHalf = 7f, tear = 1f,
        )

        /** Brows pulled up in the middle, a wobbly little mouth, a drop of sweat. */
        val Worried = MascotFace(
            pupil = 3.4f, browInner = 46f, browOuter = 56f, browLift = -1f, cheek = 0.4f,
            mouthEnd = 82f, mouthControl = 79f, mouthTilt = 2f, mouthHalf = 6f, sweat = 1f,
        )

        /** Heavy lids, flat brows, a small flat mouth. */
        val Sleepy = MascotFace(
            lid = 0.62f, pupil = 3.6f, browInner = 53f, browOuter = 55f, browLift = 0f, cheek = 0.5f,
            mouthEnd = 82f, mouthControl = 83f, mouthHalf = 5f,
        )

        /** Raised brows over wide eyes and a half smile: the "I have an idea" face the hint bulb sits on. */
        val Idea = lerp(Happy, Surprised, 0.3f)
    }
}

/** The face [f] of the way from [a] to [b] (0 is [a], 1 is [b]). */
fun lerp(a: MascotFace, b: MascotFace, f: Float): MascotFace {
    fun m(x: Float, y: Float) = x + (y - x) * f
    return MascotFace(
        lid = m(a.lid, b.lid), pupil = m(a.pupil, b.pupil),
        browInner = m(a.browInner, b.browInner), browOuter = m(a.browOuter, b.browOuter), browLift = m(a.browLift, b.browLift),
        cheek = m(a.cheek, b.cheek),
        mouthEnd = m(a.mouthEnd, b.mouthEnd), mouthControl = m(a.mouthControl, b.mouthControl), mouthTilt = m(a.mouthTilt, b.mouthTilt),
        mouthHalf = m(a.mouthHalf, b.mouthHalf), mouthOpen = m(a.mouthOpen, b.mouthOpen),
        sweat = m(a.sweat, b.sweat), tear = m(a.tear, b.tear), eyeArc = m(a.eyeArc, b.eyeArc), tongue = m(a.tongue, b.tongue),
    )
}
