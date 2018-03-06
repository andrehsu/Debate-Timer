package andre.debatetimer

import andre.debatetimer.timer.DebateTimer
import andre.debatetimer.timer.TimerOption

interface State

interface HasTimerOption : State {
	val timerOption: TimerOption
}

object WaitingToBegin : State

class WaitingToStart(override val timerOption: TimerOption) : State, HasTimerOption

class TimerStarted(override val timerOption: TimerOption, val timer: DebateTimer) : State, HasTimerOption {
	var running: Boolean = false
		private set
	var ended: Boolean = false
	
	
	fun setRunning(view: IMainView, value: Boolean) {
		if (running != value) {
			running = value
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