package andre.debatetimer

import andre.debatetimer.timer.DebateTimer
import andre.debatetimer.timer.TimerConfiguration

sealed class State

interface HasTimerOption {
    val timerConfig: TimerConfiguration
}

object Initial : State()

class WaitingToStart(override val timerConfig: TimerConfiguration) : State(), HasTimerOption

class TimerActive(val timer: DebateTimer) : State(), HasTimerOption {
    override val timerConfig: TimerConfiguration
        get() = timer.timerConfig
    val running = timer.running
    val overTime = timer.overTime
}