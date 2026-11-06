package com.mehmtcan.brainscroll.ui.topicgroups

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.text.font.FontWeight
import com.mehmtcan.brainscroll.game.wordle.Language
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.a11y_topicgroups_clear
import brainscroll.shared.generated.resources.a11y_topicgroups_found
import brainscroll.shared.generated.resources.a11y_topicgroups_group
import brainscroll.shared.generated.resources.a11y_topicgroups_mistakes
import brainscroll.shared.generated.resources.a11y_topicgroups_shuffle
import brainscroll.shared.generated.resources.a11y_topicgroups_word
import brainscroll.shared.generated.resources.a11y_topicgroups_word_selected
import brainscroll.shared.generated.resources.topicgroups_clear
import brainscroll.shared.generated.resources.topicgroups_found
import brainscroll.shared.generated.resources.topicgroups_hint
import brainscroll.shared.generated.resources.topicgroups_mistakes
import brainscroll.shared.generated.resources.topicgroups_result
import brainscroll.shared.generated.resources.topicgroups_shuffle
import brainscroll.shared.generated.resources.topicgroups_won
import com.mehmtcan.brainscroll.daily.formatDuration
import com.mehmtcan.brainscroll.game.topicgroups.TOPIC_GROUP_COUNT
import com.mehmtcan.brainscroll.game.topicgroups.TOPIC_WORDS_PER_GROUP
import com.mehmtcan.brainscroll.game.topicgroups.TopicGroupsResult
import com.mehmtcan.brainscroll.game.topicgroups.TopicGroupsSnapshot
import com.mehmtcan.brainscroll.game.topicgroups.TopicGroupsStatus
import com.mehmtcan.brainscroll.game.wordle.WordleStatus
import com.mehmtcan.brainscroll.ui.components.Chip
import com.mehmtcan.brainscroll.ui.components.HeartButton
import com.mehmtcan.brainscroll.ui.components.LanguageFlagButton
import com.mehmtcan.brainscroll.ui.feed.GuideMark
import com.mehmtcan.brainscroll.ui.feed.isGuided
import com.mehmtcan.brainscroll.ui.feed.swipeHintText
import com.mehmtcan.brainscroll.ui.haptics.HapticKind
import com.mehmtcan.brainscroll.ui.haptics.LocalHaptics
import com.mehmtcan.brainscroll.ui.haptics.OutcomeHaptics
import com.mehmtcan.brainscroll.ui.motion.LocalReduceMotion
import com.mehmtcan.brainscroll.ui.motion.Motion
import com.mehmtcan.brainscroll.ui.motion.rememberJustFinished
import com.mehmtcan.brainscroll.ui.motion.rememberSettledStatus
import com.mehmtcan.brainscroll.ui.motion.bounceIn
import com.mehmtcan.brainscroll.ui.theme.BrainScrollColors
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.LocalHighContrast
import com.mehmtcan.brainscroll.ui.theme.Radius
import com.mehmtcan.brainscroll.ui.components.ResultStats
import com.mehmtcan.brainscroll.ui.components.ResultStatData
import brainscroll.shared.generated.resources.result_stat_time
import brainscroll.shared.generated.resources.result_stat_mistakes
import com.mehmtcan.brainscroll.ui.theme.resultSurface
import com.mehmtcan.brainscroll.ui.theme.world
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.ui.theme.Spacing
import com.mehmtcan.brainscroll.ui.wordle.LanguageSwitch
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

/** Height reserved for the bottom area, the same as the other games, so the board does not jump between them. */
private val BottomAreaHeight = 152.dp
private val MaxBoard = 440.dp
private val MaxTileHeight = 84.dp
private val Gap = 8.dp
private const val COLUMNS = TOPIC_WORDS_PER_GROUP

/** How long a picked word takes to reach its row, and how the topic of the group then shows over its words. */
private const val FLY_MILLIS = 320
private const val BANNER_DELAY_MILLIS = 280
private const val BANNER_MILLIS = 200

/** The group that is found last comes by itself: it waits for the one before it to be shown, then shows the same way. */
private const val LAST_GROUP_DELAY_MILLIS = BANNER_DELAY_MILLIS + BANNER_MILLIS
private const val WIN_MILLIS = (LAST_GROUP_DELAY_MILLIS + BANNER_DELAY_MILLIS + BANNER_MILLIS + 50).toLong()

/** How long rejected words stay red. */
private const val FLASH_MILLIS = 450L

/** For tests: the number of a word (its group is the number divided by four). It is on the tile of every word, found or not. */
internal val TopicGroupsWord = SemanticsPropertyKey<Int>("TopicGroupsWord")
private var SemanticsPropertyReceiver.wordNumber by TopicGroupsWord

/** The colors of the four groups, by group number. */
internal fun topicGroupColors(colors: BrainScrollColors): List<Color> = listOf(colors.streakAnswer, colors.correctFill, colors.pendingFill, colors.streakDay)

/**
 * One full-screen page of the feed: how many groups are found and how many mistakes were made, sixteen words, and the shuffle and
 * clear buttons or the result. A tap picks a word; with the fourth the four are checked: a group is found and flies to the top
 * with its topic, or it is a mistake and the four shake and turn red for a moment (they stay picked, so one can be swapped).
 */
@Composable
fun TopicGroupsPage(
    page: TopicGroupsSnapshot,
    onTap: (Int) -> TopicGroupsResult,
    onShuffle: () -> Boolean,
    onClear: () -> Boolean,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
    /** The language flag in the top right corner; null when the page has none. */
    languageSwitch: LanguageSwitch? = null,
) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    val haptics = LocalHaptics.current
    val reduceMotion = LocalReduceMotion.current
    val state = page.state
    val won = state.status == TopicGroupsStatus.Won

    // The last group is shown a moment after the one before it: the result, the buzz and the card wait for it.
    val status = rememberSettledStatus(if (won) WordleStatus.Won else WordleStatus.Playing, WIN_MILLIS)
    OutcomeHaptics(status, page.id)
    val justFinished = rememberJustFinished(status)

    // When the language is switched on this page the board is made again: it grows in. A page that is merely shown does not.
    val firstId = remember { page.id }
    val renew = remember(page.id) { Animatable(if (page.id == firstId) 1f else if (reduceMotion) 0.3f else 0f) }
    LaunchedEffect(page.id) {
        if (renew.value < 1f) renew.animateTo(1f, if (reduceMotion) tween(Motion.FAST) else tween(Motion.SLOW, easing = Motion.Overshoot))
    }

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                Chip(
                    text = stringResource(Res.string.topicgroups_found, state.foundCount),
                    textColor = colors.textSecondary,
                    description = stringResource(Res.string.a11y_topicgroups_found, state.foundCount),
                )
                Chip(
                    text = stringResource(Res.string.topicgroups_mistakes, state.mistakes),
                    textColor = if (state.mistakes > 0) colors.error else colors.textSecondary,
                    description = stringResource(Res.string.a11y_topicgroups_mistakes, state.mistakes),
                )
            }
            if (languageSwitch != null) LanguageFlagButton(languageSwitch.language, languageSwitch.description, languageSwitch.onSwitch)
        }

        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            val boardWidth = minOf(MaxBoard, maxWidth)
            val tileWidth = (boardWidth - Gap * (COLUMNS - 1)) / COLUMNS
            val tileHeight = minOf(tileWidth * 0.92f, MaxTileHeight, (maxHeight - Gap * (TOPIC_GROUP_COUNT - 1)) / TOPIC_GROUP_COUNT)
            val boardHeight = tileHeight * TOPIC_GROUP_COUNT + Gap * (TOPIC_GROUP_COUNT - 1)
            key(page.id) {
                Board(
                    page = page,
                    tileWidth = tileWidth,
                    tileHeight = tileHeight,
                    modifier = Modifier
                        .size(boardWidth, boardHeight)
                        .testTag("topicBoard")
                        .graphicsLayer {
                            val s = 0.85f + 0.15f * renew.value
                            scaleX = s
                            scaleY = s
                            alpha = renew.value.coerceIn(0f, 1f)
                        },
                    onTap = { word ->
                        when (onTap(word)) {
                            TopicGroupsResult.Selected, TopicGroupsResult.Deselected -> haptics.perform(HapticKind.Light)
                            TopicGroupsResult.GroupSolved -> haptics.perform(HapticKind.Medium)
                            TopicGroupsResult.Wrong, TopicGroupsResult.Repeated -> haptics.perform(HapticKind.Warning)
                            else -> Unit
                        }
                    },
                )
            }
        }

        Spacer(Modifier.height(Spacing.md))
        Box(modifier = Modifier.fillMaxWidth().height(BottomAreaHeight), contentAlignment = Alignment.Center) {
            if (status != WordleStatus.Playing) {
                TopicGroupsResultCard(page, isFavorite, onToggleFavorite, Modifier.bounceIn(justFinished))
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    BasicText(
                        text = stringResource(Res.string.topicgroups_hint),
                        style = type.body.copy(color = colors.textSecondary, textAlign = TextAlign.Center),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
                        ActionChip(
                            text = stringResource(Res.string.topicgroups_shuffle),
                            description = stringResource(Res.string.a11y_topicgroups_shuffle),
                            enabled = state.canShuffle,
                            tag = "topicShuffle",
                            onClick = { if (onShuffle()) haptics.perform(HapticKind.Light) },
                        )
                        ActionChip(
                            text = stringResource(Res.string.topicgroups_clear),
                            description = stringResource(Res.string.a11y_topicgroups_clear),
                            enabled = state.canClear,
                            tag = "topicClear",
                            onClick = { if (onClear()) haptics.perform(HapticKind.Light) },
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(Spacing.md))
    }
}

/** A small button with words, for the two actions under the board. Dim and not tappable when there is nothing for it to do. */
@Composable
private fun ActionChip(text: String, description: String, enabled: Boolean, tag: String, onClick: () -> Unit) {
    Box(
        Modifier
            .testTag(tag)
            .alpha(if (enabled) 1f else 0.35f)
            .semantics { if (!enabled) disabled() },
    ) {
        Chip(
            text = text,
            textColor = BrainScrollTheme.colors.textPrimary,
            onClick = if (enabled) onClick else null,
            raised = true,
            description = description,
        )
    }
}

/**
 * The sixteen words, each in its place of a grid of four rows: the rows of the groups that are found come first (a group in the
 * order it was found), the words that are left follow in the order of the board. A word that changes place slides there. A
 * found group has its topic shown over its four words.
 */
@Composable
private fun Board(page: TopicGroupsSnapshot, tileWidth: Dp, tileHeight: Dp, modifier: Modifier, onTap: (Int) -> Unit) {
    val colors = BrainScrollTheme.colors
    val reduce = LocalReduceMotion.current
    val density = LocalDensity.current
    val state = page.state
    val playing = state.status == TopicGroupsStatus.Playing
    val groupColors = remember(colors) { topicGroupColors(colors) }

    // Words that were rejected shake and turn red for a moment. A page that comes back with its mistakes does not.
    var seenRejections by remember { mutableIntStateOf(state.rejections) }
    var flashWords by remember { mutableStateOf(emptySet<Int>()) }
    val shake = remember { Animatable(0f) }
    LaunchedEffect(state.rejections) {
        if (state.rejections > seenRejections) {
            seenRejections = state.rejections
            flashWords = state.lastRejected.toSet()
            if (!reduce) {
                for (x in listOf(-10f, 10f, -7f, 7f, 0f)) shake.animateTo(x, tween(45))
            }
            delay(FLASH_MILLIS)
            flashWords = emptySet()
        } else {
            seenRejections = state.rejections
        }
    }

    val stepX = with(density) { (tileWidth + Gap).roundToPx() }
    val stepY = with(density) { (tileHeight + Gap).roundToPx() }

    // How much of its topic each group shows: the topic comes over the words once they have reached their row, and the words
    // fade as it does. A group that was found before the page was shown has it from the start; one that is found now fades it in.
    val banners = List(TOPIC_GROUP_COUNT) { group ->
        val rank = state.solved.indexOf(group)
        val lateStart = if (rank == TOPIC_GROUP_COUNT - 1) LAST_GROUP_DELAY_MILLIS else 0
        animateFloatAsState(
            if (rank >= 0) 1f else 0f,
            if (reduce) snap() else tween(BANNER_MILLIS, delayMillis = if (rank >= 0) BANNER_DELAY_MILLIS + lateStart else 0, easing = Motion.Standard),
        ).value
    }

    Box(modifier) {
        for (word in state.words.indices) {
            val group = word / TOPIC_WORDS_PER_GROUP
            val rank = state.solved.indexOf(group)
            val slot = if (rank >= 0) rank * COLUMNS + word % COLUMNS else state.solved.size * COLUMNS + state.board.indexOf(word)
            val target = IntOffset((slot % COLUMNS) * stepX, (slot / COLUMNS) * stepY)
            WordTile(
                text = state.words[word],
                word = word,
                target = target,
                tileWidth = tileWidth,
                tileHeight = tileHeight,
                color = groupColors[group],
                rank = rank,
                topicShown = banners[group],
                picked = word in state.selected,
                rejected = word in flashWords,
                shakeX = if (word in flashWords) shake.value else 0f,
                playing = playing,
                onClick = { onTap(word) },
            )
        }
        for (group in 0 until TOPIC_GROUP_COUNT) {
            GroupBanner(
                title = state.titles[group],
                language = page.language,
                words = state.wordsOf(group).map { state.words[it] },
                rank = state.solved.indexOf(group),
                shown = banners[group],
                color = groupColors[group],
                width = tileWidth * COLUMNS + Gap * (COLUMNS - 1),
                height = tileHeight,
                stepY = stepY,
            )
        }
    }
}

/**
 * The text size at which [text] fits [widthPx] on one line, never above [maxPx]: it is measured with the real font, so a long word
 * is as wide as the tile and never wider. The size follows the tile, not the text size setting: the word has to fit.
 */
internal fun TextMeasurer.fitFontPx(text: String, style: TextStyle, density: Density, widthPx: Float, maxPx: Float): Float {
    fun widthAt(px: Float) = measure(text, style.copy(fontSize = with(density) { px.toSp() }), maxLines = 1, softWrap = false).size.width.toFloat()
    var px = maxPx
    val width = widthAt(px)
    if (width > widthPx) px = px * widthPx / width
    // Rounding in the layout can leave a pixel or two: step down until it fits.
    while (px > 1f && widthAt(px) > widthPx) px *= 0.97f
    return px
}

@Composable
private fun fitFontSize(text: String, style: TextStyle, widthPx: Float, maxPx: Float): androidx.compose.ui.unit.TextUnit {
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val px = remember(text, style, widthPx, maxPx, density) { measurer.fitFontPx(text, style, density, widthPx, maxPx) }
    return with(density) { px.toSp() }
}

/**
 * One word. Picked, it turns light (the colors swap) and grows a little. Found, it goes to its row, in the color of its group.
 * Rejected, it turns red. It is the Compose state of one word that moves, so the same tile is seen in the old place, in
 * between, and in the new one.
 */
@Composable
private fun WordTile(
    text: String,
    word: Int,
    target: IntOffset,
    tileWidth: Dp,
    tileHeight: Dp,
    color: Color,
    rank: Int,
    /** How much of the topic of its group is over the word (0 to 1): the word fades as it comes. */
    topicShown: Float,
    picked: Boolean,
    rejected: Boolean,
    shakeX: Float,
    playing: Boolean,
    onClick: () -> Unit,
) {
    val colors = BrainScrollTheme.colors
    val reduce = LocalReduceMotion.current
    val density = LocalDensity.current
    val extraBorder = if (LocalHighContrast.current) 1f else 0f
    val found = rank >= 0
    // The last group comes by itself, a moment after the one before it.
    val lateStart = if (rank == TOPIC_GROUP_COUNT - 1) LAST_GROUP_DELAY_MILLIS else 0

    val offset by animateIntOffsetAsState(target, if (reduce) snap() else tween(FLY_MILLIS, easing = Motion.Standard))
    val fill by animateColorAsState(
        when {
            found -> color
            rejected -> colors.error
            picked -> colors.textPrimary
            else -> colors.bgRaised
        },
        if (reduce) snap() else tween(if (found) FLY_MILLIS else Motion.FAST, delayMillis = if (found) lateStart else 0, easing = Motion.Standard),
    )
    val ink by animateColorAsState(
        when {
            found -> colors.bgPage
            rejected -> colors.textPrimary
            picked -> colors.bgPage
            else -> colors.textPrimary
        },
        if (reduce) snap() else tween(if (found) FLY_MILLIS else Motion.FAST, delayMillis = if (found) lateStart else 0, easing = Motion.Standard),
    )
    val grow by animateFloatAsState(if (picked && !found) 1.05f else 1f, if (reduce) snap() else tween(Motion.FAST, easing = Motion.Standard))
    val description = stringResource(if (picked) Res.string.a11y_topicgroups_word_selected else Res.string.a11y_topicgroups_word, text)

    // The outer box moves and shakes; the ring of the guide is on the box inside it, round the tile (which is one node for a screen reader).
    Box(
        modifier = Modifier
            .offset { offset }
            .zIndex(if (found) 2f else if (picked) 1f else 0f)
            .size(tileWidth, tileHeight)
            .graphicsLayer {
                scaleX = grow
                scaleY = grow
                translationX = with(density) { shakeX.dp.toPx() }
                transformOrigin = TransformOrigin.Center
            },
    ) {
        GuideMark(isGuided(word) && !found, Radius.control) {
            Box(
                modifier = Modifier
                    .size(tileWidth, tileHeight)
                    .testTag(if (found) "topicFoundTile" else "topicTile")
                    .then(
                        // A word of a found group is spoken once, by the bar over it: the tile keeps only its number, for tests.
                        if (found) Modifier.clearAndSetSemantics { wordNumber = word }
                        else Modifier.semantics(mergeDescendants = true) {
                            contentDescription = description
                            role = Role.Button
                            selected = picked
                            wordNumber = word
                        },
                    )
                    .background(fill, Radius.control)
                    .border((1.5f + extraBorder).dp, colors.borderStrong.copy(alpha = if (picked || found) 0f else 0.35f), Radius.control)
                    .then(
                        if (playing && !found) Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onClick)
                        else Modifier,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                val widthPx = with(density) { (tileWidth - 10.dp).toPx() }
                val base = BrainScrollTheme.typography.label.copy(fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                BasicText(
                    text = text,
                    maxLines = 1,
                    softWrap = false,
                    style = base.copy(
                        color = ink.copy(alpha = ink.alpha * (1f - topicShown)),
                        fontSize = fitFontSize(text, base, widthPx, with(density) { (tileHeight * 0.27f).toPx() }),
                    ),
                )
            }
        }
    }
}

/**
 * The topic of a found group over its four words: a bar in the color of the group with the topic and the words. It is there from
 * the start for a group that was found before the page was shown, and fades in after the words have reached their row for one that
 * is found now. A group that is not found has none.
 */
@Composable
private fun GroupBanner(title: String, language: Language, words: List<String>, rank: Int, shown: Float, color: Color, width: Dp, height: Dp, stepY: Int) {
    val colors = BrainScrollTheme.colors
    val density = LocalDensity.current
    if (shown <= 0f) return
    val joined = words.joinToString(", ")
    val description = stringResource(Res.string.a11y_topicgroups_group, title, joined)
    // Capitals by the rules of the language of the words: Turkish i is İ, not I.
    val shownTitle = language.uppercase(title)
    Box(
        modifier = Modifier
            .offset { IntOffset(0, maxOf(rank, 0) * stepY) }
            .zIndex(3f)
            .size(width, height)
            .testTag("topicBanner")
            .graphicsLayer { alpha = shown }
            .semantics(mergeDescendants = true) { contentDescription = description }
            .background(color, Radius.control),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            val widthPx = with(density) { (width - 16.dp).toPx() }
            val titleStyle = BrainScrollTheme.typography.label.copy(fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            val wordsStyle = BrainScrollTheme.typography.label.copy(fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
            BasicText(
                text = shownTitle,
                maxLines = 1,
                softWrap = false,
                style = titleStyle.copy(color = colors.bgPage, fontSize = fitFontSize(shownTitle, titleStyle, widthPx, with(density) { (height * 0.27f).toPx() })),
            )
            BasicText(
                text = joined,
                maxLines = 1,
                softWrap = false,
                style = wordsStyle.copy(color = colors.bgPage, fontSize = fitFontSize(joined, wordsStyle, widthPx, with(density) { (height * 0.2f).toPx() })),
            )
        }
    }
}

@Composable
private fun TopicGroupsResultCard(page: TopicGroupsSnapshot, isFavorite: Boolean, onToggleFavorite: () -> Unit, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography

    Box(modifier = modifier.fillMaxWidth().resultSurface(colors.bgSurface, GameKind.TopicGroups.world, Radius.card).padding(Spacing.lg)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            BasicText(
                text = stringResource(Res.string.topicgroups_won),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = type.title.copy(color = colors.correctFill, textAlign = TextAlign.Center),
            )
            ResultStats(
    summary = stringResource(Res.string.topicgroups_result, page.state.mistakes, formatDuration(page.durationMs)),
    stats = listOf(
        ResultStatData(page.state.mistakes.toString(), stringResource(Res.string.result_stat_mistakes), if (page.state.mistakes == 0) colors.correctFill else colors.pendingFill),
        ResultStatData(formatDuration(page.durationMs), stringResource(Res.string.result_stat_time), colors.streakAnswer),
    ),
)
            BasicText(
                text = swipeHintText(),
                style = type.caption.copy(color = colors.textSecondary, textAlign = TextAlign.Center),
            )
        }
        HeartButton(filled = isFavorite, onClick = onToggleFavorite, modifier = Modifier.align(Alignment.TopEnd))
    }
}
