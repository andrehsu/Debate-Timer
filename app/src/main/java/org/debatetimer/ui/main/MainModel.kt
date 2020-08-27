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


sealed class MainModelState() {
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
}

class Initial private constructor(private val model: MainModel) : MainModelState() {
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
    override val bellsText: LiveData<String> = model.enableBells.map { if (it) model.res.string.on else model.res.string.off }
    override val timerControlButtonText: LiveData<String> = MutableLiveData("")
}

class TimerActive private constructor(private val model: MainModel, val timer: DebateTimer) : MainModelState() {
    companion object {
        fun MainModel.newTimerActiveState(timer: DebateTimer): TimerActive {
            return TimerActive(this, timer)
        }
    }
    
    override val isClockVisible: Boolean = true
    override val isOvertimeTextVisible: LiveData<Boolean> = timer.overTime
    override val selectedTimerConfigTag: String = timer.config.tag
    override val keepScreenOn: LiveData<Boolean> = timer.running
    override val timerOptionsClickable: LiveData<Boolean> = timer.running.map { !it }
    override val minutes: LiveData<Int> = MediatorLiveData<Int>().apply {
        fun updateValue() {
            value = when (model.countMode.value!!) {
                CountMode.CountUp -> timer.minutesCountUp.value
                CountMode.CountDown -> timer.minutesCountDown.value
            }
        }
        addSource(timer.minutesCountUp) { updateValue() }
        addSource(model.countMode) { updateValue() }
    }
    override val seconds: LiveData<Int> = MediatorLiveData<Int>().apply {
        fun updateValue() {
            value = when (model.countMode.value!!) {
                CountMode.CountUp -> timer.secondsCountUp.value
                CountMode.CountDown -> timer.secondsCountDown.value
            }
        }
        addSource(timer.secondsCountUp) { updateValue() }
        addSource(model.countMode) { updateValue() }
    }
    override val timerTextColor: LiveData<Int> = timer.textColor
    override val overTimeText: LiveData<String> = timer.overTimeText.map { model.res.string.overtimeBy.format(it) }
    override val bellsText: LiveData<String> = MediatorLiveData<String>().apply {
        fun updateValue() {
            value =
                    
                    if (model.enableBells.value!!) {
                        when (model.countMode.value!!) {
                            CountMode.CountUp -> timer.config.countUpBellsText
                            CountMode.CountDown -> timer.config.countDownBellsText
                        }
                    } else {
                        model.res.string.off
                    }
            
        }
        
        addSource(model.countMode) { updateValue() }
        addSource(model.enableBells) { updateValue() }
        
    }
    
    
    override val timerControlButtonText: LiveData<String> = timer.started.switchMap { started ->
        if (!started) {
            MutableLiveData(model.res.string.start)
        } else {
            timer.running.map { running -> if (running) model.res.string.pause else model.res.string.resume }
        }
    }
}

class MainModel(app: Application) : AndroidViewModel(app) {
    
    val prefs = AppPreferences.getInstance(getApplication())
    val res = AppResources.getInstance(getApplication())
    
    
    val countMode: LiveData<CountMode> = prefs.countMode
    val enableBells: LiveData<Boolean> = prefs.enableBells
    private val timersStr: LiveData<String> = prefs.timersStr
    
    private val bellRinger = BellRinger(getApplication())
    
    val timerConfigs: Map<String, TimerConfiguration> = parseTimerMapsStr(timersStr.value!!)
    private val _state: MutableLiveData<MainModelState> = MutableLiveData(newInitialState())
    val state: LiveData<MainModelState> = _state
    
    val keepScreenOn: LiveData<Boolean> = state.switchMap { it.keepScreenOn }
    
    init {
        val tag = prefs.selectedTimerConfigTag.value
        if (tag != res.string.prefSelectedTimerConfigDefault) {
            _state.value = newTimerActiveState(newTimerInstance(timerConfigs.getValue(tag)))
        }
        
        state.observeForever { state ->
            prefs.selectedTimerConfigTag.putValue(if (state is TimerActive) state.timer.config.tag else res.string.prefSelectedTimerConfigDefault)
            
        }
    }
    
    
    private fun newTimerInstance(timerConfig: TimerConfiguration): DebateTimer {
        return object : DebateTimer(getApplication(), timerConfig) {
            override fun onBell(debateBell: DebateBell) {
                if (prefs.enableBells.value)
                    bellRinger.playBell(debateBell)
            }
        }
    }
    
    fun onStartButtonClick() {
        when (val state = state.value) {
            is Initial -> throw RuntimeException("Invalid state. Button show not be visible.")
            is TimerActive -> {
                state.timer.running.value!!.let {
                    state.timer.setRunning(!it)
                }
            }
        }
    }
    
    fun onResetTime() {
        val state = state.value
        if (state is TimerActive) {
            onTimeButtonSelect(state.timer.config.tag)
        }
    }
    
    fun onTimeButtonSelect(buttonTag: String) {
        val state = state.value
        if (state is TimerActive) {
            state.timer.setRunning(false)
        }
    
        this._state.value = newTimerActiveState(newTimerInstance(timerConfigs.getValue(buttonTag)))
    }
    
    fun onToggleCountMode() {
        prefs.countMode.putValue(prefs.countMode.value.other())
    }
    
    fun onToggleDebateBells() {
        prefs.enableBells.putValue(!prefs.enableBells.value)
    }
    
    fun onSkipForward() {
        val state = state.value
        if (state is TimerActive) {
            state.timer.skipForward()
        }
    }
    
    fun onSkipBackward(): Boolean {
        val state = state.value
        if (state is TimerActive) {
            return state.timer.skipBackward()
        }
        return false
    }
    
    private fun parseTimerMapsStr(str: String): Map<String, TimerConfiguration> {
        val timerMaps = mutableMapOf<String, TimerConfiguration>()
        
        str.split('|').forEach { s ->
            val timerOption = TimerConfiguration.parseTag(s)
            
            timerMaps[timerOption.tag] = timerOption
        }
        
        return timerMaps
    }
    
    
}
