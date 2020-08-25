package andre.debatetimer

import andre.debatetimer.timer.BellRinger
import andre.debatetimer.timer.DebateBell
import andre.debatetimer.timer.DebateTimer
import andre.debatetimer.timer.TimerOption
import android.app.Application
import androidx.lifecycle.*

class MainModel(app: Application) : AndroidViewModel(app) {
    
    private val trueLiveData: LiveData<Boolean>
        get() = MutableLiveData(true)
    private val falseLiveData: LiveData<Boolean>
        get() = MutableLiveData(false)
    private val zeroLiveData: LiveData<Int>
        get() = MutableLiveData(0)
    
    private val prefs = AppPreference.getInstance(getApplication())
    private val res = AppResources.getInstance(getApplication())
    
    val countMode: LiveData<CountMode> = prefs.countMode
    val enableBells: LiveData<Boolean> = prefs.enableBells
    val timersStr: LiveData<String> = prefs.timersStr
    
    val bellRinger = BellRinger(getApplication())
    
    val timerMaps: Map<String, TimerOption> = parseTimerMapsStr(timersStr.value!!)
    private val _state: MutableLiveData<State> = MutableLiveData(Initial)
    val state: LiveData<State> = _state
    
    val clockVisible: LiveData<Boolean> = state.switchMap { state ->
        when (state) {
            is Initial -> falseLiveData
            is WaitingToStart -> trueLiveData
            is TimerActive -> trueLiveData
        }
    }
    
    
    val showOverTimeText: LiveData<Boolean> = state.switchMap { state ->
        when (state) {
            is Initial -> falseLiveData
            is WaitingToStart -> falseLiveData
            is TimerActive -> state.timer.overTime
        }
    }
    
    val keepScreenOn: LiveData<Boolean> = state.switchMap { state ->
        when (state) {
            is Initial -> falseLiveData
            is WaitingToStart -> falseLiveData
            is TimerActive -> state.timer.running
        }
    }
    
    val selectedTimerOptionTag: LiveData<String> = state.map { state ->
        when (state) {
            is Initial -> "None"
            is WaitingToStart -> state.timerOption.tag
            is TimerActive -> state.timerOption.tag
        }
    }
    
    
    val timerOptionsClickable: LiveData<Boolean> = state.switchMap { state ->
        when (state) {
            is Initial -> trueLiveData
            is WaitingToStart -> trueLiveData
            is TimerActive -> state.running.map { running -> !running }
        }
    }
    
    val minutes = state.switchMap { state ->
        when (state) {
            is Initial -> zeroLiveData
            is WaitingToStart -> {
                val mediatorLiveData = MediatorLiveData<Int>()
                fun updateValue() {
                    mediatorLiveData.value = when (countMode.value!!) {
                        CountMode.CountUp -> 0
                        CountMode.CountDown -> state.timerOption.minutes
                    }
                }
                mediatorLiveData.addSource(countMode) { updateValue() }
        
                mediatorLiveData
            }
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
            is WaitingToStart -> {
                val mediatorLiveData = MediatorLiveData<Int>()
                fun updateValue() {
                    mediatorLiveData.value = when (countMode.value!!) {
                        CountMode.CountUp -> 0
                        CountMode.CountDown -> state.timerOption.seconds
                    }
                }
                mediatorLiveData.addSource(countMode) { updateValue() }
        
                mediatorLiveData
            }
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
            is WaitingToStart -> MutableLiveData(res.color.timerStart)
            is TimerActive -> state.timer.textColor
        }
    }
    val overTimeText: LiveData<String> = state.switchMap { state ->
        when (state) {
            is Initial -> MutableLiveData("")
            is WaitingToStart -> MutableLiveData("")
            is TimerActive -> state.timer.overTimeText
        }
    }
    
    val bellsText: LiveData<String> = state.switchMap { state ->
        val mediatorLiveData = MediatorLiveData<String>()
        fun updateValue() {
            mediatorLiveData.value = when (state) {
                is Initial -> if (enableBells.value!!) res.string.on else res.string.off
                is WaitingToStart -> {
                    if (enableBells.value!!) {
                        when (countMode.value!!) {
                            CountMode.CountUp -> state.timerOption.countUpBellsText
                            CountMode.CountDown -> state.timerOption.countDownBellsText
                        }
                    } else {
                        res.string.off
                    }
                }
                is TimerActive -> {
                    if (enableBells.value!!) {
                        when (countMode.value!!) {
                            CountMode.CountUp -> state.timerOption.countUpBellsText
                            CountMode.CountDown -> state.timerOption.countDownBellsText
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
            is WaitingToStart -> MutableLiveData(res.string.start)
            is TimerActive -> state.running.map { running -> if (running) res.string.pause else res.string.resume }
        }
    }
    
    
    private fun newTimerInstance(timerOption: TimerOption): DebateTimer {
        return object : DebateTimer(getApplication(), timerOption) {
            override fun onBell(debateBell: DebateBell) {
                if (prefs.enableBells.value)
                    bellRinger.playBell(debateBell)
            }
        }
    }
    
    fun onStartButtonClick() {
        when (val state = state.value) {
            is Initial -> throw RuntimeException("Invalid state. Button show not be visible.")
            is WaitingToStart -> {
                this._state.value = TimerActive(newTimerInstance(state.timerOption)).also { it.timer.setRunning(true) }
        
            }
            is TimerActive -> {
                state.running.value!!.let {
                    state.timer.setRunning(!it)
                }
            }
        }
    }
    
    fun onResetTime() {
        val state = state.value
        if (state is HasTimerOption) {
            onTimeButtonSelect(state.timerOption.tag)
        }
    }
    
    fun onTimeButtonSelect(buttonTag: String) {
        val state = state.value
        if (state is TimerActive) {
            state.timer.setRunning(false)
        }
    
        this._state.value = WaitingToStart(timerMaps.getValue(buttonTag))
    }
    
    fun onToggleCountMode() {
        prefs.countMode.putValue(prefs.countMode.value.other())
    }
    
    fun onToggleDebateBells() {
        prefs.enableBells.putValue(!prefs.enableBells.value)
    }
    
    private fun parseTimerMapsStr(str: String): Map<String, TimerOption> {
        val timerMaps = mutableMapOf<String, TimerOption>()
        
        str.split('|').forEach { s ->
            val timerOption = TimerOption.parseTag(s)
            
            if (timerOption != null) {
                timerMaps[timerOption.tag] = timerOption
            }
        }
        
        return timerMaps
    }
}