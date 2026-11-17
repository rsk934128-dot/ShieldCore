package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SecurityDatabase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class ShieldCoreRobolectricTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `app name matches ShieldCore`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("ShieldCore", context.getString(R.string.app_name))
    }

    @Test
    fun `room signature database matches eicar sha256`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, SecurityDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val dao = db.securityDao()
            dao.insertSignatures(SecurityDatabase.DEFAULT_SIGNATURES)
            val match = dao.findSignatureByHash(
                "275a021bbfb6489e54d471899f7db9d1663fc695ec2fe2a2c4538aabf651fd0f"
            )
            assertNotNull(match)
            assertEquals("EICAR-Standard-AV-Test", match?.threatName)
        } finally {
            db.close()
        }
    }

    @Test
    fun `dashboard renders key controls and supports navigation`() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("scan_now_button").assertIsDisplayed()
        composeRule.onNodeWithTag("dashboard_list")
            .performScrollToNode(hasTestTag("realtime_shield_switch"))
        composeRule.onNodeWithTag("realtime_shield_switch").assertIsDisplayed()

        composeRule.onNodeWithTag("dashboard_list")
            .performScrollToNode(hasTestTag("card_deep_scan"))
        composeRule.onNodeWithTag("card_deep_scan").assertIsDisplayed()
        composeRule.onNodeWithTag("card_junk_cleaner").assertIsDisplayed()

        // Navigate to Deep Scan tab
        composeRule.onNodeWithTag("nav_scanner").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("pick_folder_button").assertIsDisplayed()
        composeRule.onNodeWithTag("inspect_file_button").assertIsDisplayed()

        // Navigate to Cleaner tab
        composeRule.onNodeWithTag("nav_cleaner").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("reanalyze_junk_button").assertIsDisplayed()
    }
}
