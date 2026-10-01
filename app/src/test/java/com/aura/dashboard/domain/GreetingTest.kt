package com.aura.dashboard.domain

import com.aura.dashboard.ui.home.Greeting
import com.aura.dashboard.ui.home.HomeViewModel
import org.junit.Test
import kotlin.test.assertEquals

class GreetingTest {
    @Test
    fun `greeting follows the time of day`() {
        assertEquals(Greeting.EVENING, HomeViewModel.greetingFor(2))
        assertEquals(Greeting.MORNING, HomeViewModel.greetingFor(5))
        assertEquals(Greeting.MORNING, HomeViewModel.greetingFor(11))
        assertEquals(Greeting.AFTERNOON, HomeViewModel.greetingFor(12))
        assertEquals(Greeting.AFTERNOON, HomeViewModel.greetingFor(17))
        assertEquals(Greeting.EVENING, HomeViewModel.greetingFor(18))
        assertEquals(Greeting.EVENING, HomeViewModel.greetingFor(23))
    }
}
