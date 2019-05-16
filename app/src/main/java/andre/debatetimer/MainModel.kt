package andre.debatetimer

import andre.debatetimer.livedata.NonNullLiveData
import andre.debatetimer.livedata.StateLiveData
import andre.debatetimer.livedata.StringLiveData
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
    
    val state: StateLiveData
    
    lateinit var timerMaps: Map<String, TimerOption>
    
    init {
        val context = application.applicationContext
        
        EnvVars.init(context)
        Prefs.init(context)
        state = NonNullLiveData(InitState(Prefs.countMode, Prefs.debateBellEnabled, StringLiveData("")))
        
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
                            1f)
            }
        }
    }
    
    fun onStartPause() {
        fun toggleRunning(state: TimerStarted) {
            if (state.running.value) {
                state.setRunning(false)
            } else {
                state.setRunning(true)
            }
        }
    
        when (val state = state.value) {
            is WaitingToStart -> {
                val timer = newTimerInstance(state.timerOption)
                this.state.value = state.toTimerStarted(timerOption = state.timerOption, debateTimer = timer).also(::toggleRunning)
            }
            is TimerStarted -> toggleRunning(state)
        }
    }
    
    fun onResetTime() {
        onTimeButtonSelect(state.value.selectedButtonStr.value)
    }
    
    fun onTimeButtonSelect(buttonStr: String) {
        val state = state.value
        if (state is TimerStarted) {
            state.setRunning(false)
        }
    
        this.state.value = state.toWaitingToStart(timerOption = timerMaps.getValue(buttonStr))
    
        state.selectedButtonStr.value = buttonStr
    }
    
    fun onToggleCountMode() {
        Prefs.countMode.value = (if (Prefs.countMode.value == CountMode.CountUp) CountMode.CountDown else CountMode.CountUp)
    }
    
    fun onToggleDebateBells() {
        Prefs.debateBellEnabled.value = !Prefs.debateBellEnabled.value
    }
}