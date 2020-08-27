package org.debatetimer.ui.main

import android.app.Application
import android.content.Context
import androidx.lifecycle.*
import org.debatetimer.AppPreferences
import org.debatetimer.AppResources
import org.debatetimer.CountMode
import org.debatetimer.other
import org.debatetimer.timer.BellRinger
import org.debatetimer.timer.DebateBell
import org.debatetimer.timer.DebateTimer

private val trueLiveData: LiveData<Boolean>
    get() = MutableLiveData(true)
private val falseLiveData: LiveData<Boolean>
    get() = MutableLiveData(false)
private val zeroLiveData: LiveData<Int>
    get() = MutableLiveData(0)

typealias ChangeStateFunction = (MainModelState) -> Unit


sealed class MainModelState(protected val context: Context, protected val changeState: ChangeStateFunction) {
    companion object {
        fun initialize(context: Context, changeState: ChangeStateFunction): MainModelState {
            val state = Initial(context, changeState)
            val tag = state.prefs.selectedTimerConfigTag.value
            if (tag in state.prefs.timerConfigs.value) {
                return TimerActive(context, changeState, tag)
            }
            
            return state
        }
    }
    
    protected val prefs = AppPreferences.getInstance(context)
    protected val res = AppResources.getInstance(context)
    
    val countMode: LiveData<CountMode> = prefs.countMode
    val enableBells: LiveData<Boolean> = prefs.enableBells
    
    abstract val isClockVisible: Boolean
    abstract val isOvertimeTextVisible: LiveData<Boolean>
    abstract val selectedTimerConfigTag: String
    abstract val keepScreenOn: LiveData<Boolean>
    abstract val timerOptionsClickable: LiveData<Boolean>
    abstract val minutes: LiveData<Int>
    abstract val seconds: LiveData<Int>
    abstract val timerTextColor: LiveData<Int>
    abstract val overTimeText: LiveData<String>
    abstract val bellsText: LiveData<String>
    abstract val timerControlButtonText: LiveData<String>
    abstract val isNegativeSignVisible: LiveData<Boolean>
    
    abstract fun onTimerConfigSelect(tag: String)
    abstract fun onSkipBackward(): Boolean
    abstract fun onSkipForward()
    abstract fun onTimerControlButtonClick()
    
    fun onToggleCountMode() {
        prefs.countMode.putValue(prefs.countMode.value.other())
    }
    
    fun onToggleDebateBells() {
        prefs.enableBells.putValue(!prefs.enableBells.value)
    }
}

class Initial(context: Context, changeState: ChangeStateFunction) : MainModelState(context, changeState) {
    override val isClockVisible: Boolean = false
    override val isOvertimeTextVisible: LiveData<Boolean> = falseLiveData
    override val selectedTimerConfigTag: String = "none"
    override val keepScreenOn: LiveData<Boolean> = falseLiveData
    override val timerOptionsClickable: LiveData<Boolean> = trueLiveData
    override val minutes: LiveData<Int> = zeroLiveData
    override val seconds: LiveData<Int> = zeroLiveData
    override val timerTextColor: LiveData<Int> = zeroLiveData
    override val overTimeText: LiveData<String> = MutableLiveData("")
    override val bellsText: LiveData<String> = enableBells.map { if (it) res.string.on else res.string.off }
    override val timerControlButtonText: LiveData<String> = MutableLiveData("")
    override val isNegativeSignVisible: LiveData<Boolean> = falseLiveData
    
    override fun onTimerConfigSelect(tag: String) {
        prefs.selectedTimerConfigTag.putValue(tag)
        changeState(TimerActive(context, changeState, tag))
    }
    
    override fun onSkipBackward(): Boolean {
        throw IllegalStateException("Button should not be visible")
    }
    
    override fun onSkipForward() {
        throw IllegalStateException("Button should not be visible")
    }
    
    override fun onTimerControlButtonClick() {
        throw IllegalStateException("Button should not be visible")
    }
}

class TimerActive(context: Context, changeState: ChangeStateFunction, timerConfigTag: String) : MainModelState(context, changeState) {
    private val bellRinger = BellRinger(context)
    
    val timerConfig = prefs.timerConfigs.value.getValue(timerConfigTag)
    private val timer: DebateTimer = object : DebateTimer(res, timerConfig) {
        override fun onBell(debateBell: DebateBell) {
            if (prefs.enableBells.value)
                bellRinger.playBell(debateBell)
        }
    }
    override val isClockVisible: Boolean = true
    override val selectedTimerConfigTag: String = timerConfig.tag
    override val keepScreenOn: LiveData<Boolean> = timer.running
    override val timerOptionsClickable: LiveData<Boolean> = timer.running.map { !it }
    override val minutes: LiveData<Int> = MediatorLiveData<Int>().apply {
        fun updateValue() {
            value = when (countMode.value!!) {
                CountMode.CountUp -> timer.minutesCountUp.value
                CountMode.CountDown -> timer.minutesCountDown.value
            }
        }
        addSource(timer.minutesCountUp) { updateValue() }
        addSource(countMode) { updateValue() }
    }
    override val seconds: LiveData<Int> = MediatorLiveData<Int>().apply {
        fun updateValue() {
            value = when (countMode.value!!) {
                CountMode.CountUp -> timer.secondsCountUp.value
                CountMode.CountDown -> timer.secondsCountDown.value
            }
        }
        addSource(timer.secondsCountUp) { updateValue() }
        addSource(countMode) { updateValue() }
    }
    override val timerTextColor: LiveData<Int> = timer.textColor
    override val overTimeText: LiveData<String> = timer.overTimeText.map { res.string.overtimeBy.format(it) }
    override val bellsText: LiveData<String> = MediatorLiveData<String>().apply {
        fun updateValue() {
            value =
                    
                    if (enableBells.value!!) {
                        when (countMode.value!!) {
                            CountMode.CountUp -> timerConfig.countUpBellsText
                            CountMode.CountDown -> timerConfig.countDownBellsText
                        }
                    } else {
                        res.string.off
                    }
            
        }
        
        addSource(countMode) { updateValue() }
        addSource(enableBells) { updateValue() }
    
    }
    override val timerControlButtonText: LiveData<String> = timer.started.switchMap { started ->
        if (!started) {
            MutableLiveData(res.string.start)
        } else {
            timer.running.map { running -> if (running) res.string.pause else res.string.resume }
        }
    }
    
    override val isNegativeSignVisible: LiveData<Boolean> = MediatorLiveData<Boolean>().apply {
        fun updateValue() {
            value = countMode.value!! == CountMode.CountDown && timer.overTime.value!!
        }
        
        addSource(countMode) { updateValue() }
        addSource(timer.overTime) { updateValue() }
    }
    override val isOvertimeTextVisible: LiveData<Boolean> = MediatorLiveData<Boolean>().apply {
        fun updateValue() {
            value = countMode.value!! == CountMode.CountUp && timer.overTime.value!!
        }
        
        addSource(countMode) { updateValue() }
        addSource(timer.overTime) { updateValue() }
    }
    
    
    override fun onTimerConfigSelect(tag: String) {
        prefs.selectedTimerConfigTag.putValue(tag)
        timer.setRunning(false)
        
        changeState(TimerActive(context, changeState, tag))
    }
    
    override fun onSkipBackward(): Boolean {
        return timer.skipBackward()
    }
    
    override fun onSkipForward() {
        timer.skipForward()
    }
    
    override fun onTimerControlButtonClick() {
        timer.running.value!!.let { timer.setRunning(!it) }
    }
}

class MainModel(app: Application) : AndroidViewModel(app) {
    private fun changeState(state: MainModelState) {
        this.state.value = state
    }
    
    private val state: MutableLiveData<MainModelState> = MutableLiveData(MainModelState.initialize(getApplication(), this::changeState))
    
    private val prefs = AppPreferences.getInstance(getApplication())
    
    val keepScreenOn: LiveData<Boolean> = state.switchMap { it.keepScreenOn }
    val bellsText: LiveData<String> = state.switchMap { it.bellsText }
    val isClockVisible: LiveData<Boolean> = state.map { it.isClockVisible }
    val overTimeText: LiveData<String> = state.switchMap { it.overTimeText }
    val isOvertimeTextVisible: LiveData<Boolean> = state.switchMap { it.isOvertimeTextVisible }
    val timerTextColor: LiveData<Int> = state.switchMap { it.timerTextColor }
    val minutes: LiveData<Int> = state.switchMap { it.minutes }
    val seconds: LiveData<Int> = state.switchMap { it.seconds }
    val timerControlButtonText: LiveData<String> = state.switchMap { it.timerControlButtonText }
    val timerOptionsClickable: LiveData<Boolean> = state.switchMap { it.timerOptionsClickable }
    val selectedTimerConfigTag: LiveData<String> = state.map { it.selectedTimerConfigTag }
    val enableBells: LiveData<Boolean> = state.switchMap { it.enableBells }
    val countMode: LiveData<CountMode> = state.switchMap { it.countMode }
    val isNegativeSignVisible: LiveData<Boolean> = state.switchMap { it.isNegativeSignVisible }
    
    fun onTimeConfigSelect(tag: String) = state.value!!.onTimerConfigSelect(tag)
    
    fun onTimerControlButtonClick() = state.value!!.onTimerControlButtonClick()
    
    fun onToggleCountMode() = state.value!!.onToggleCountMode()
    
    fun onToggleDebateBells() = state.value!!.onToggleDebateBells()
    
    fun onSkipForward() = state.value!!.onSkipForward()
    
    fun onSkipBackward(): Boolean = state.value!!.onSkipBackward()
}
