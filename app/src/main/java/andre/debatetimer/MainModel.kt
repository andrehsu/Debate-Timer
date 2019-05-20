package andre.debatetimer

import andre.debatetimer.livedata.NonNullMutableLiveData
import andre.debatetimer.timer.DebateBell
import andre.debatetimer.timer.DebateTimer
import andre.debatetimer.timer.TimerOption
import android.app.Application
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.lifecycle.AndroidViewModel

class MainModel(application: Application) : AndroidViewModel(application) {
    private var soundPool: SoundPool
    private var debateBellOnce: Int = -1
    private var debateBellTwice: Int = -1
    
    val state: NonNullMutableLiveData<State>
    
    lateinit var timerMaps: Map<String, TimerOption>
    
    init {
        val context = application.applicationContext
    
        Res.init(context)
        Prefs.init(context)
        state = NonNullMutableLiveData(InitState(Prefs.countMode, Prefs.debateBellEnabled))
    
        //region Setup debate bells
        val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        soundPool = SoundPool.Builder()
                .setMaxStreams(1)
                .setAudioAttributes(attributes)
                .build()
        
        debateBellOnce = soundPool.load(context, R.raw.debate_bell_one, 1)
        debateBellTwice = soundPool.load(context, R.raw.debate_bell_two, 1)
        //endregion
    }
    
    
    private fun newTimerInstance(timerOption: TimerOption): DebateTimer {
        return object : DebateTimer(timerOption) {
            override fun onBell(debateBell: DebateBell) {
                if (Prefs.debateBellEnabled.value)
                    soundPool.play(
                            when (debateBell) {
                                DebateBell.Once -> debateBellOnce
                                DebateBell.Twice -> debateBellTwice
                            },
                            1f,
                            1f,
                            1,
                            0,
                            1f
                    )
            }
        }
    }
    
    fun onTimerControl() {
        when (val state = state.value) {
            is WaitingToStart -> {
                val timer = newTimerInstance(state.timerOption)
                this.state.value = TimerStarted(
                        state.countMode,
                        state.enableBells,
                        state.selectedTimerOptionText,
                        state.timerOption,
                        timer
                ).also {
                    it.running.value = (true)
                }
            }
            is TimerStarted -> {
                if (state.running.value) {
                    state.running.value = (false)
                } else {
                    state.running.value = (true)
                }
            }
        }
    }
    
    fun onResetTime() {
        onTimeButtonSelect(state.value.selectedTimerOptionText.value)
    }
    
    fun onTimeButtonSelect(buttonStr: String) {
        val state = state.value
        if (state is TimerStarted) {
            state.running.value = (false)
        }
    
        this.state.value = WaitingToStart(
                state.countMode,
                state.enableBells,
                state.selectedTimerOptionText,
                timerMaps.getValue(buttonStr)
        )
    
        state.selectedTimerOptionText.value = buttonStr
    }
    
    fun onToggleCountMode() {
        Prefs.countMode.value = (if (Prefs.countMode.value == CountMode.CountUp) CountMode.CountDown else CountMode.CountUp)
    }
    
    fun onToggleDebateBells() {
        Prefs.debateBellEnabled.value = !Prefs.debateBellEnabled.value
    }
}