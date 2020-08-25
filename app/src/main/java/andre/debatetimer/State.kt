package andre.debatetimer

import andre.debatetimer.timer.DebateTimer
import andre.debatetimer.timer.TimerOption

sealed class State

interface HasTimerOption {
    val timerOption: TimerOption
}

object Initial : State()

class WaitingToStart(override val timerOption: TimerOption) : State(), HasTimerOption

class TimerActive(val timer: DebateTimer) : State(), HasTimerOption {
    override val timerOption: TimerOption
        get() = timer.timerOption
    val running = timer.running
    val overTime = timer.overTime
}