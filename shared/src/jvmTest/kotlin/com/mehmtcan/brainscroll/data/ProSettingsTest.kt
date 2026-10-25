package com.mehmtcan.brainscroll.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.mehmtcan.brainscroll.db.BrainScrollDatabase
import com.mehmtcan.brainscroll.game.guide.GuideUse
import com.mehmtcan.brainscroll.ui.theme.Accent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** What Pro keeps on the device: the free guide steps used today, the cached answer, the rescue marks and the look. */
class ProSettingsTest {
    private fun newRepository(): GameRepository {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        BrainScrollDatabase.Schema.create(driver)
        return GameRepository(BrainScrollDatabase(driver))
    }

    @Test
    fun theGuideUseIsKept() {
        val repo = newRepository()
        assertEquals(GuideUse.None, repo.guideUse())
        repo.setGuideUse(GuideUse(day = 20_000, used = 3))
        assertEquals(GuideUse(20_000, 3), repo.guideUse())
    }

    @Test
    fun theProAnswerIsKeptAndStartsAsFree() {
        val repo = newRepository()
        assertFalse(repo.cached())
        repo.setCached(true)
        assertTrue(repo.cached())
        repo.setCached(false)
        assertFalse(repo.cached())
    }

    @Test
    fun theLookIsKept() {
        val repo = newRepository()
        assertEquals(Accent.Green, repo.savedAccent())
        assertFalse(repo.savedAmoled())
        repo.saveAccent(Accent.Orange)
        repo.saveAmoled(true)
        assertEquals(Accent.Orange, repo.savedAccent())
        assertTrue(repo.savedAmoled())
    }

    @Test
    fun deletingTheAccountKeepsTheFreeStepsAndTheProAnswerSoNoFreeStepsAreHandedOut() {
        val repo = newRepository()
        repo.setGuideUse(GuideUse(day = 20_000, used = 5))
        repo.setCached(true)
        repo.saveAccent(Accent.Blue)
        repo.setRescueShownFor(8)
        repo.setAutoPaywallDay(20_000)

        repo.wipeLocalData()

        assertEquals(GuideUse(20_000, 5), repo.guideUse(), "deleting the account must not reset the free steps")
        assertTrue(repo.cached(), "the purchase belongs to the store account, not to the app account")
        assertEquals(Accent.Blue, repo.savedAccent())
        // What is about the old history goes with it.
        assertNull(repo.rescueShownFor())
        assertNull(repo.autoPaywallDay())
    }
}
