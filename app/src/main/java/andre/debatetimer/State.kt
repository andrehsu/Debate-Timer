package andre.debatetimer

import andre.debatetimer.timer.DebateTimer
import andre.debatetimer.timer.TimerConfiguration

sealed class State

object Initial : State()

class TimerActive(val timer: DebateTimer) : State() {
    var started = timer.started
    val timerConfig: TimerConfiguration
        get() = timer.timerConfig
    val running = timer.running
}