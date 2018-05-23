package andre.debatetimer

import andre.debatetimer.livedata.BooleanLiveData
import andre.debatetimer.livedata.NLiveData
import andre.debatetimer.timer.DebateTimer
import andre.debatetimer.timer.TimerOption

interface State

interface HasTimerOption : State {
	val timerOption: TimerOption
}

object InitState : State

class WaitingToStart(override val timerOption: TimerOption) : State, HasTimerOption

class TimerStarted(override val timerOption: TimerOption, val timer: DebateTimer) : State, HasTimerOption {
	var running = BooleanLiveData(false)
	var ended = BooleanLiveData(false)
	
	
	fun setRunning(value: Boolean) {
		if (running.value != value) {
			running.value = value
			if (value) {
				timer.resume()
			} else {
				timer.pause()
			}
		}
	}
}


class LiveState(state: State) : NLiveData<State>(state)

class LiveDebateTimer(debateTimer: DebateTimer) : NLiveData<DebateTimer>(debateTimer)