package com.example.medicalhomevisit

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class LoginScreenTest {

    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun loginFlow_success_navigatesToNextScreen() {
        val email = "patient.test@example.com"
        val password = "password123"

        composeTestRule.onNodeWithText("Email").performTextInput(email)

        composeTestRule.onNodeWithText("Пароль").performTextInput(password)

        composeTestRule.onNodeWithText("Войти").performClick()

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule
                .onAllNodesWithText("Мои заявки")
                .fetchSemanticsNodes().size == 1
        }

        composeTestRule.onNodeWithText("Мои заявки").assertIsDisplayed()
    }
}