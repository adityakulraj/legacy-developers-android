package com.legacydevelopers.app

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LegacyAppE2eTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun startWithEmptyLocalData() {
        composeRule.activity
            .getSharedPreferences("legacy_inventory", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        composeRule.activityRule.scenario.recreate()
        composeRule.waitForIdle()
    }

    @Test
    fun logoAndPrimaryNavigation_areVisibleAndWorking() {
        composeRule.onNodeWithContentDescription("Legacy Developers logo").assertIsDisplayed()
        composeRule.onNodeWithText("LEGACY DEVELOPERS").assertIsDisplayed()
        composeRule.onNodeWithText("Project Overview").assertIsDisplayed()

        composeRule.onNodeWithText("All Flats").performClick()
        composeRule.onNodeWithText("Flat number and super built-up area").assertIsDisplayed()

        composeRule.onNodeWithText("Bookings").performClick()
        composeRule.onNodeWithText("Booked payment details and unsold inventory").assertIsDisplayed()

        composeRule.onNodeWithText("GST Report").performClick()
        composeRule.onNodeWithText("Monthly GST Payment Report").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Add customer payment").assertIsDisplayed()
    }

    @Test
    fun userCanAddAnUnsoldFlat_andSeeItInInventory() {
        composeRule.onNodeWithContentDescription("Add flat").performClick()
        composeRule.onNodeWithText("Flat number").performTextInput("TEST-101")
        composeRule.onNodeWithText("Super built-up area (sq ft)").performTextInput("1250")
        composeRule.onNodeWithText("Save Flat").performClick()

        composeRule.onNodeWithText("All Flats").performClick()
        composeRule.onNodeWithText("FLAT NO. TEST-101").assertIsDisplayed()
        composeRule.onNodeWithText("1250 sq ft super built-up").assertIsDisplayed()
    }

    @Test
    fun gstTab_opensTheCustomerPaymentDialog() {
        composeRule.onNodeWithText("GST Report").performClick()
        composeRule.onNodeWithContentDescription("Add customer payment").performClick()

        composeRule.onNodeWithText("Record Customer Payment").assertIsDisplayed()
        composeRule.onNodeWithText("Payment received date (YYYY-MM-DD)").assertIsDisplayed()
        composeRule.onNodeWithText("Save Payment").assertIsDisplayed()
    }
}
