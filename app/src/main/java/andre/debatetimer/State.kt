package andre.debatetimer

import andre.debatetimer.CountMode.CountUp
import andre.debatetimer.livedata.*
import andre.debatetimer.timer.DebateTimer
import andre.debatetimer.timer.TimerOption
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import kotlin.math.absoluteValue

private const val Tag = "StateKt"

sealed class State {
    abstract val countMode: NonNullMutableLiveData<CountMode>
    abstract val timerOption: TimerOption
    
    abstract val enableBells: BooleanLiveData
    abstract val inOvertime: BooleanLiveData
    abstract val keepScreenOn: BooleanLiveData
    abstract val timerOptionsSelectable: BooleanLiveData
    
    abstract val minutes: IntLiveData
    abstract val seconds: IntLiveData
    abstract val timerTextColor: IntLiveData
    
    abstract val selectedTimerOptionText: MutableStringLiveData
    abstract val overtimeText: StringLiveData
    abstract val bellsText: StringLiveData
    abstract val timerControlButtonText: StringLiveData
    
    abstract val startingTextVisible: Boolean
    abstract val timerVisible: Boolean
    abstract val timerControlButtonVisible: Boolean
    abstract val resetButtonVisible: Boolean
}

class InitState(override val countMode: NonNullMutableLiveData<CountMode>,
                override val enableBells: BooleanLiveData) : State() {
    override val timerOption = TimerOption.Default
    
    override val inOvertime = BooleanLiveData(false)
    override val keepScreenOn = BooleanLiveData(false)
    override val timerOptionsSelectable = BooleanLiveData(true)
    
    override val minutes = MutableIntLiveData(0)
    override val seconds = MutableIntLiveData(0)
    override val timerTextColor = MutableIntLiveData(0)
    
    override val selectedTimerOptionText = MutableStringLiveData("")
    override val overtimeText = MutableStringLiveData("")
    override val bellsText = map(enableBells) { if (it) Res.string.on else Res.string.off }
    override val timerControlButtonText = MutableStringLiveData("")
    
    override val startingTextVisible = true
    override val timerVisible = false
    override val timerControlButtonVisible = false
    override val resetButtonVisible = false
}

class WaitingToStart(override val countMode: NonNullMutableLiveData<CountMode>,
                     override val enableBells: BooleanLiveData,
                     override val selectedTimerOptionText: MutableStringLiveData,
                     override val timerOption: TimerOption) : State() {
    override val inOvertime = MutableBooleanLiveData(false)
    override val keepScreenOn = MutableBooleanLiveData(false)
    override val timerOptionsSelectable = MutableBooleanLiveData(true)
    
    override val minutes = map(countMode) { if (it == CountUp) 0 else timerOption.minutes }
    override val seconds = map(countMode) { if (it == CountUp) 0 else timerOption.seconds }
    override val timerTextColor = MutableIntLiveData(Res.color.timerStart)
    
    override val overtimeText = MutableStringLiveData("")
    override val bellsText = bellsTextLiveData
    override val timerControlButtonText = MutableStringLiveData(Res.string.start)
    
    override val startingTextVisible: Boolean = false
    override val timerVisible: Boolean = true
    override val timerControlButtonVisible = true
    override val resetButtonVisible = false
}

class TimerStarted(override val countMode: NonNullMutableLiveData<CountMode>,
                   override val enableBells: BooleanLiveData,
                   override val selectedTimerOptionText: MutableStringLiveData,
                   override val timerOption: TimerOption,
                   private val timer: DebateTimer) : State() {
    val running = timer.running
    
    override val inOvertime = timer.overtime
    override val keepScreenOn = running
    override val timerOptionsSelectable = map(running) {
        !it
    }
    
    override val minutes = object : MutableIntLiveData(0) {
        init {
            updateValue()
        }
        
        override fun observe(owner: LifecycleOwner, observer: Observer<in Int>) {
            super.observe(owner, observer)
            
            countMode.observe(owner) { updateValue() }
            timer.minutesCountUp.observe(owner) { updateValue() }
        }
        
        private fun updateValue() {
            value = if (countMode.value == CountUp) {
                timer.minutesCountUp.value
            } else {
                timer.minutesCountDown.value.absoluteValue
            }
        }
    }
    override val seconds = object : MutableIntLiveData(0) {
        init {
            updateValue()
        }
        
        override fun observe(owner: LifecycleOwner, observer: Observer<in Int>) {
            super.observe(owner, observer)
            
            countMode.observe(owner) { updateValue() }
            timer.secondsCountUp.observe(owner) { updateValue() }
        }
        
        private fun updateValue() {
            value = if (countMode.value == CountUp) {
                timer.secondsCountUp.value
            } else {
                timer.secondsCountDown.value.absoluteValue
            }
        }
    }
    override val timerTextColor = timer.textColor
    
    override val overtimeText = object : MutableStringLiveData("") {
        init {
            updateValue()
        }
        
        private fun updateValue() {
            value = if (countMode.value == CountUp) {
                val m = minutes.value - timerOption.minutes
                val s = seconds.value - timerOption.seconds
                
                "%s %d:%02d".format(Res.string.overtimeBy, m, s)
            } else {
                Res.string.overtimeBy
            }
        }
    }
    override val bellsText = bellsTextLiveData
    override val timerControlButtonText = map(running) { if (it) Res.string.pause else Res.string.resume }
//    override val timerControlButtonText = MutableStringLiveData("Nani")
    
    override val startingTextVisible = false
    override val timerVisible = true
    override val timerControlButtonVisible = true
    override val resetButtonVisible = true
}

private fun <T, R> map(source: NonNullLiveData<T>, mapFunction: (T) -> R): NonNullLiveData<R> {
    return object : NonNullMutableLiveData<R>(mapFunction(source.value)) {
        override fun observe(owner: LifecycleOwner, observer: Observer<in R>) {
            super.observe(owner, observer)
            source.observe(owner) {
                Log.d(Tag, "$it")
                value = mapFunction(it)
            }
        }
    }
}

private val State.bellsTextLiveData
    get() = object : NonNullMutableLiveData<String>("") {
        init {
            updateValue()
        }
        
        override fun observe(owner: LifecycleOwner, observer: Observer<in String>) {
            super.observe(owner, observer)
            
            enableBells.observe(owner) { updateValue() }
            countMode.observe(owner) { updateValue() }
        }
        
        private fun updateValue() {
            value = if (enableBells.value) {
                if (countMode.value == CountUp) {
                    timerOption.countUpBellsText
                } else {
                    timerOption.countDownBellsText
                }
            } else {
                Res.string.off
            }
        }
    }