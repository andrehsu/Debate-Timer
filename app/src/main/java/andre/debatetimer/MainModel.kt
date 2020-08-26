package andre.debatetimer

import andre.debatetimer.timer.BellRinger
import andre.debatetimer.timer.DebateBell
import andre.debatetimer.timer.DebateTimer
import andre.debatetimer.timer.TimerConfiguration
import android.app.Application
import androidx.lifecycle.*

class MainModel(app: Application) : AndroidViewModel(app) {
    
    private val trueLiveData: LiveData<Boolean>
        get() = MutableLiveData(true)
    private val falseLiveData: LiveData<Boolean>
        get() = MutableLiveData(false)
    private val zeroLiveData: LiveData<Int>
        get() = MutableLiveData(0)
    
    private val prefs = AppPreferences.getInstance(getApplication())
    private val res = AppResources.getInstance(getApplication())
    
    val countMode: LiveData<CountMode> = prefs.countMode
    val enableBells: LiveData<Boolean> = prefs.enableBells
    val timersStr: LiveData<String> = prefs.timersStr
    
    val bellRinger = BellRinger(getApplication())
    
    val timerConfigs: Map<String, TimerConfiguration> = parseTimerMapsStr(timersStr.value!!)
    private val _state: MutableLiveData<State> = MutableLiveData(Initial)
    val state: LiveData<State> = _state
    
    val clockVisible: LiveData<Boolean> = state.switchMap { state ->
        when (state) {
            is Initial -> falseLiveData
            is TimerActive -> trueLiveData
        }
    }
    
    
    val showOverTimeText: LiveData<Boolean> = state.switchMap { state ->
        when (state) {
            is Initial -> falseLiveData
            is TimerActive -> state.timer.overTime
        }
    }
    
    val keepScreenOn: LiveData<Boolean> = state.switchMap { state ->
        when (state) {
            is Initial -> falseLiveData
            is TimerActive -> state.timer.running
        }
    }
    
    val selectedTimerOptionTag: LiveData<String> = state.map { state ->
        when (state) {
            is Initial -> "None"
            is TimerActive -> state.timerConfig.tag
        }
    }
    
    
    val timerOptionsClickable: LiveData<Boolean> = state.switchMap { state ->
        when (state) {
            is Initial -> trueLiveData
            is TimerActive -> state.running.map { running -> !running }
        }
    }
    
    val minutes = state.switchMap { state ->
        when (state) {
            is Initial -> zeroLiveData
    
            is TimerActive -> {
                val mediatorLiveData = MediatorLiveData<Int>()
                fun updateValue() {
                    mediatorLiveData.value = when (countMode.value!!) {
                        CountMode.CountUp -> state.timer.minutesCountUp.value
                        CountMode.CountDown -> state.timer.minutesCountDown.value
                    }
                }
                mediatorLiveData.addSource(state.timer.minutesCountUp) { updateValue() }
                mediatorLiveData.addSource(countMode) { updateValue() }
        
                mediatorLiveData
            }
        }
    }
    
    val seconds = state.switchMap { state ->
        when (state) {
            is Initial -> zeroLiveData
            is TimerActive -> {
                val mediatorLiveData = MediatorLiveData<Int>()
                fun updateValue() {
                    mediatorLiveData.value = when (countMode.value!!) {
                        CountMode.CountUp -> state.timer.secondsCountUp.value
                        CountMode.CountDown -> state.timer.secondsCountDown.value
                    }
                }
                mediatorLiveData.addSource(state.timer.secondsCountUp) { updateValue() }
                mediatorLiveData.addSource(countMode) { updateValue() }
        
                mediatorLiveData
            }
        }
    }
    
    val timerTextColor: LiveData<Int> = state.switchMap { state ->
        when (state) {
            is Initial -> zeroLiveData
            is TimerActive -> state.timer.textColor
        }
    }
    val overTimeText: LiveData<String> = state.switchMap { state ->
        when (state) {
            is Initial -> MutableLiveData("")
            is TimerActive -> state.timer.overTimeText.map { res.string.overtimeBy.format(it) }
        }
    }
    
    val bellsText: LiveData<String> = state.switchMap { state ->
        val mediatorLiveData = MediatorLiveData<String>()
        fun updateValue() {
            mediatorLiveData.value = when (state) {
                is Initial -> if (enableBells.value!!) res.string.on else res.string.off
                is TimerActive -> {
                    if (enableBells.value!!) {
                        when (countMode.value!!) {
                            CountMode.CountUp -> state.timerConfig.countUpBellsText
                            CountMode.CountDown -> state.timerConfig.countDownBellsText
                        }
                    } else {
                        res.string.off
                    }
                }
            }
            
        }
        
        mediatorLiveData.addSource(countMode) { updateValue() }
        mediatorLiveData.addSource(enableBells) { updateValue() }
        mediatorLiveData
    }
    
    
    val timerControlButtonText: LiveData<String> = state.switchMap { state ->
        when (state) {
            is Initial -> MutableLiveData("Gone")
            is TimerActive -> state.started.switchMap { started ->
                if (!started) {
                    MutableLiveData(res.string.start)
                } else {
                    state.running.map { running -> if (running) res.string.pause else res.string.resume }
                }
            }
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
                state.running.value!!.let {
                    state.timer.setRunning(!it)
                }
            }
        }
    }
    
    fun onResetTime() {
        val state = state.value
        if (state is TimerActive) {
            onTimeButtonSelect(state.timerConfig.tag)
        }
    }
    
    fun onTimeButtonSelect(buttonTag: String) {
        val state = state.value
        if (state is TimerActive) {
            state.timer.setRunning(false)
        }
    
        this._state.value = TimerActive(newTimerInstance(timerConfigs.getValue(buttonTag)))
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