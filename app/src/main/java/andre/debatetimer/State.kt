package andre.debatetimer

import andre.debatetimer.livedata.BooleanLiveData
import andre.debatetimer.livedata.NonNullLiveData
import andre.debatetimer.livedata.StringLiveData
import andre.debatetimer.timer.DebateTimer
import andre.debatetimer.timer.TimerOption

sealed class State {
    abstract val countMode: NonNullLiveData<CountMode>
    abstract val enableBells: BooleanLiveData
    abstract val selectedButtonStr: StringLiveData
    
    fun toWaitingToStart(countMode: NonNullLiveData<CountMode> = this.countMode,
                         enableBells: BooleanLiveData = this.enableBells,
                         selectedButtonStr: StringLiveData = this.selectedButtonStr,
                         timerOption: TimerOption): WaitingToStart {
        return WaitingToStart(countMode, enableBells, selectedButtonStr, timerOption)
    }
    
    fun toTimerStarted(countMode: NonNullLiveData<CountMode> = this.countMode,
                       enableBells: BooleanLiveData = this.enableBells,
                       timerOption: TimerOption,
                       selectedButtonStr: StringLiveData = this.selectedButtonStr,
                       debateTimer: DebateTimer): TimerStarted {
        return TimerStarted(countMode, enableBells, selectedButtonStr, timerOption, debateTimer)
    }
}

interface HasTimerOption {
    val timerOption: TimerOption
}

class InitState(override val countMode: NonNullLiveData<CountMode>,
                override val enableBells: NonNullLiveData<Boolean>,
                override val selectedButtonStr: StringLiveData) : State()

class WaitingToStart(override val countMode: NonNullLiveData<CountMode>,
                     override val enableBells: BooleanLiveData,
                     override val selectedButtonStr: StringLiveData,
                     override val timerOption: TimerOption) : State(), HasTimerOption

class TimerStarted(override val countMode: NonNullLiveData<CountMode>,
                   override val enableBells: BooleanLiveData,
                   override val selectedButtonStr: StringLiveData,
                   override val timerOption: TimerOption,
                   val timer: DebateTimer) : State(), HasTimerOption {
    var running = BooleanLiveData(false)
    
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