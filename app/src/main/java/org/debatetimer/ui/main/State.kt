package org.debatetimer.ui.main

import org.debatetimer.timer.DebateTimer
import org.debatetimer.timer.TimerConfiguration

sealed class State

object Initial : State()

class TimerActive(val timer: DebateTimer) : State() {
    var started = timer.started
    val timerConfig: TimerConfiguration
        get() = timer.timerConfig
    val running = timer.running
}