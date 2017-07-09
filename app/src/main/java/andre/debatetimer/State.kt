package andre.debatetimer

import andre.debatetimer.timer.DebateTimer
import andre.debatetimer.timer.TimerOption

interface State

interface HasTimerOption : State {
	val timerOption: TimerOption
}

object WaitingToBegin : State

class WaitingToStart(override val timerOption: TimerOption) : State, HasTimerOption

class TimerStarted(val view: TimerView, override val timerOption: TimerOption, val timer: DebateTimer) : State, HasTimerOption {
	var running: Boolean = false
		set(value) {
			if (field != value) {
				field = value
				view.buttonsActive = value
				view.keepScreenOn = value
				if (value) {
					timer.resume()
				} else {
					timer.pause()
				}
			}
		}
}