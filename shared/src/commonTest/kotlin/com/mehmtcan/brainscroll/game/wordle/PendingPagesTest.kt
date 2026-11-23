package com.mehmtcan.brainscroll.game.wordle

import com.mehmtcan.brainscroll.game.Difficulty
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.PendingSnapshot
import com.mehmtcan.brainscroll.game.fences.FenceRound
import com.mehmtcan.brainscroll.game.fences.FenceStatus
import com.mehmtcan.brainscroll.game.lamps.LampRound
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** A feed that makes its boards later (the app does it on a background thread): a page is pending until its board is filled in. */
class PendingPagesTest {
    private val words = listOf("crane", "slate", "ghost", "pound", "apple", "brick")
    private val en = WordList(Language.EN, 5, valid = words, answers = words)
    private val tr = WordList(Language.TR, 5, valid = listOf("çiçek", "kırık", "kitap"), answers = listOf("çiçek", "kırık", "kitap"))

    private fun feed(
        games: List<GameKind> = listOf(GameKind.Fences, GameKind.Lamps),
        random: Random = Random(5),
        store: FeedStore = NoFeedStore,
        languages: List<Language> = listOf(Language.EN),
    ) = EndlessFeed(mapOf(Language.EN to en, Language.TR to tr), Language.EN, random, store = store, games = games, startLanguages = languages, buildInBackground = true)

    @Test
    fun newPagesArePendingAndTheirPlansAreGivenOutOnce() {
        val f = feed()
        f.ensureSize(3)
        assertEquals(3, f.size)
        assertTrue(f.hasPending)
        val pages = f.snapshot().pages
        assertTrue(pages.all { it is PendingSnapshot })
        assertEquals(listOf(GameKind.Fences, GameKind.Lamps, GameKind.Fences), pages.map { it.kind })
        val plans = f.takePlans()
        assertEquals(listOf(0, 1, 2), plans.map { it.index })
        assertEquals(pages.map { it.id }, plans.map { it.id })
        assertTrue(f.takePlans().isEmpty(), "each plan is given out once")
    }

    @Test
    fun aBuiltBoardTakesThePlaceOfItsPendingPage() {
        val f = feed()
        f.ensureSize(2)
        val plans = f.takePlans()
        val round = f.build(plans[0])
        assertIs<FenceRound>(round)
        assertEquals(plans[0].id, round.id)
        assertTrue(f.fill(plans[0], round))
        val pages = f.snapshot().pages
        assertIs<com.mehmtcan.brainscroll.game.fences.FenceSnapshot>(pages[0])
        assertIs<PendingSnapshot>(pages[1])
        assertTrue(f.hasPending)
        assertTrue(f.fill(plans[1], f.build(plans[1])))
        assertFalse(f.hasPending)
        assertIs<LampRound>(f.build(plans[1]))
    }

    @Test
    fun theBoardOfAPlanDoesNotDependOnAnythingTheFeedChangesLater() {
        // Making the board twice from one plan gives the same board, wherever and whenever it is made.
        val f = feed()
        f.ensureSize(1)
        val plan = f.takePlans().single()
        f.ensureSize(5) // the feed goes on being used meanwhile
        val a = f.build(plan) as FenceRound
        val b = f.build(plan) as FenceRound
        assertEquals(a.toStored().answer, b.toStored().answer)
    }

    @Test
    fun aBoardForAPageThatWasDrawnAgainIsDropped() {
        val f = feed(listOf(GameKind.Fences))
        f.ensureSize(2)
        val old = f.takePlans()
        // The level changes: the pending pages are planned again, so the first boards are out of date.
        f.setDifficulty(GameKind.Fences, Difficulty.Hard, fromIndex = 0)
        val fresh = f.takePlans()
        assertEquals(2, fresh.size)
        assertTrue(fresh.all { it.level == Difficulty.Hard })
        assertFalse(f.fill(old[0], f.build(old[0])), "its page is a new plan now")
        assertTrue(f.fill(fresh[0], f.build(fresh[0])))
        assertEquals(Difficulty.Hard, f.snapshot().difficulties[0])
    }

    @Test
    fun aPageThatIsSwitchedOffWhileItWaitsIsReplaced() {
        val f = feed()
        f.ensureSize(2)
        f.takePlans()
        f.setEnabledGames(setOf(GameKind.Lamps), fromIndex = 0)
        val pages = f.snapshot().pages
        assertTrue(pages.all { it is PendingSnapshot })
        val plans = f.takePlans()
        assertTrue(plans.all { it.kind == GameKind.Lamps }, "the pages that wait are of a game that is on")
    }

    @Test
    fun aWordPageWaitingInALanguageThatIsOffIsPlannedAgain() {
        val f = feed(listOf(GameKind.Word), languages = listOf(Language.EN, Language.TR))
        f.ensureSize(4)
        f.takePlans()
        f.setWordLanguages(setOf(Language.TR), fromIndex = 0)
        val plans = f.takePlans()
        assertTrue(plans.isNotEmpty())
        assertTrue(plans.all { it.wordLanguage == Language.TR })
        for (plan in plans) assertTrue(f.fill(plan, f.build(plan)))
        assertTrue(f.snapshot().rounds.all { it.language == Language.TR })
    }

    @Test
    fun aPendingPageCannotBePlayedGuidedOrSaved() {
        val saved = mutableListOf<String>()
        val store = object : FeedStore by NoFeedStore {
            override fun saveProgress(round: StoredRound) {
                saved += round.id
            }
        }
        val f = feed(store = store)
        f.ensureSize(1)
        assertNull(f.guideTask(0), "there is no board to guide on")
        f.markGuided(0)
        f.pageShown(0)
        f.pagesHidden()
        assertTrue(saved.isEmpty(), "a page that is still being made is not saved")
        assertEquals(null, f.firstUnfinishedIndex())
        assertFalse(f.hasStarted)
        assertIs<PendingSnapshot>(f.snapshot().pages.single())
    }

    @Test
    fun theClockOfAPageThatWasOnShowStartsWhenItsBoardArrives() {
        var now = 0L
        val f = EndlessFeed(
            mapOf(Language.EN to en), Language.EN, Random(2), games = listOf(GameKind.Fences), buildInBackground = true, now = { now },
        )
        f.ensureSize(1)
        val plan = f.takePlans().single()
        f.pageShown(0) // the player is already on the page while it waits
        now = 5_000
        val round = f.build(plan) as FenceRound
        assertTrue(f.fill(plan, round))
        now = 9_000
        f.pagesHidden()
        assertEquals(4_000L, round.snapshot().durationMs, "the clock ran from the moment the board was there")
        assertEquals(FenceStatus.Playing, round.snapshot().state.status)
    }

    @Test
    fun aFeedThatBuildsOnTheSpotNeverHasPendingPages() {
        val f = EndlessFeed(mapOf(Language.EN to en), Language.EN, Random(1), games = listOf(GameKind.Fences, GameKind.Lamps))
        f.ensureSize(3)
        assertFalse(f.hasPending)
        assertTrue(f.takePlans().isEmpty())
        assertTrue(f.snapshot().pages.none { it is PendingSnapshot })
    }
}
