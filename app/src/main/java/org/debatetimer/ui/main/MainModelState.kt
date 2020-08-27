package org.debatetimer.ui.main

import org.debatetimer.timer.DebateTimer
import org.debatetimer.timer.TimerConfiguration

sealed class MainModelState

object Initial : MainModelState()

class TimerActive(val timer: DebateTimer) : MainModelState() {
    var started = timer.started
    val timerConfig: TimerConfiguration
        get() = timer.timerConfig
    val running = timer.running
}