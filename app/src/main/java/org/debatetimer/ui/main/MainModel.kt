package org.debatetimer.ui.main

import android.app.Application
import androidx.lifecycle.*
import org.debatetimer.AppPreferences
import org.debatetimer.AppResources
import org.debatetimer.CountMode
import org.debatetimer.other
import org.debatetimer.timer.BellRinger
import org.debatetimer.timer.DebateBell
import org.debatetimer.timer.DebateTimer
import org.debatetimer.timer.TimerConfiguration
import org.debatetimer.ui.main.Initial.Companion.newInitialState
import org.debatetimer.ui.main.TimerActive.Companion.newTimerActiveState

private val trueLiveData: LiveData<Boolean>
    get() = MutableLiveData(true)
private val falseLiveData: LiveData<Boolean>
    get() = MutableLiveData(false)
private val zeroLiveData: LiveData<Int>
    get() = MutableLiveData(0)


sealed class MainModelState(model: MainModel) {
    protected val prefs = AppPreferences.getInstance(model.getApplication())
    protected val res = AppResources.getInstance(model.getApplication())
    
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
    
    abstract fun onTimerConfigSelect()
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

class Initial private constructor(model: MainModel) : MainModelState(model) {
    companion object {
        fun MainModel.newInitialState(): Initial {
            return Initial(this)
        }
    }
    
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
    
    override fun onTimerConfigSelect() {}
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

class TimerActive private constructor(model: MainModel, val timerConfig: TimerConfiguration) : MainModelState(model) {
    companion object {
        fun MainModel.newTimerActiveState(timerConfig: TimerConfiguration): TimerActive {
            return TimerActive(this, timerConfig)
        }
    }
    
    private val bellRinger = BellRinger(model.getApplication())
    
    
    val timer: DebateTimer = object : DebateTimer(res, timerConfig) {
        override fun onBell(debateBell: DebateBell) {
            if (prefs.enableBells.value)
                bellRinger.playBell(debateBell)
        }
    }
    override val isClockVisible: Boolean = true
    override val isOvertimeTextVisible: LiveData<Boolean> = timer.overTime
    override val selectedTimerConfigTag: String = timer.config.tag
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
                            CountMode.CountUp -> timer.config.countUpBellsText
                            CountMode.CountDown -> timer.config.countDownBellsText
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
    
    override fun onTimerConfigSelect() {
        timer.setRunning(false)
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
    private val state: MutableLiveData<MainModelState> = MutableLiveData(newInitialState())
    
    val prefs = AppPreferences.getInstance(getApplication())
    val res = AppResources.getInstance(getApplication())
    
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
    
    
    init {
        val tag = prefs.selectedTimerConfigTag.value
        if (tag != res.string.prefSelectedTimerConfigDefault) {
            state.value = newTimerActiveState(prefs.timerConfigs.value.getValue(tag))
        }
        
        state.observeForever { state ->
            prefs.selectedTimerConfigTag.putValue(if (state is TimerActive) state.timer.config.tag else res.string.prefSelectedTimerConfigDefault)
        }
    }
    
    
    fun onTimeButtonSelect(tag: String) {
        state.value!!.onTimerConfigSelect()
        
        
        this.state.value = newTimerActiveState(prefs.timerConfigs.value.getValue(tag))
    }
    
    
    fun onTimerControlButtonClick() = state.value!!.onTimerControlButtonClick()
    
    fun onToggleCountMode() = state.value!!.onToggleCountMode()
    
    fun onToggleDebateBells() = state.value!!.onToggleDebateBells()
    
    fun onSkipForward() = state.value!!.onSkipForward()
    
    fun onSkipBackward(): Boolean = state.value!!.onSkipBackward()
}
