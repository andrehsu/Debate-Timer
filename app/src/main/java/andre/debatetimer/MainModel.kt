package andre.debatetimer

import andre.debatetimer.livedata.MutableLiveData
import andre.debatetimer.livedata.SharedPreferenceLiveData
import andre.debatetimer.livedata.map
import andre.debatetimer.livedata.switchMap
import andre.debatetimer.timer.DebateBell
import andre.debatetimer.timer.DebateTimer
import andre.debatetimer.timer.TimerOption
import android.app.Application
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.lifecycle.*

class MainModel(application: Application) : AndroidViewModel(application) {
    lateinit var timerMaps: Map<String, TimerOption>
    
    
    init {
        val context = application.applicationContext
        
        Res.init(context)
        Prefs.init(context)
    }
    
    private val _countMode: SharedPreferenceLiveData<CountMode> = Prefs.countMode
    val countMode: LiveData<CountMode> = _countMode
    private val _enableBells: SharedPreferenceLiveData<Boolean> = Prefs.enableBells
    val enableBells: LiveData<Boolean> = _enableBells
    
    private val _timerOption: MutableLiveData<TimerOption?> = MutableLiveData(null)
    val timerOption: LiveData<TimerOption?> = _timerOption
    private val _timer: MutableLiveData<DebateTimer?> = MutableLiveData(null)
    val running = _timer.switchMap { timer -> timer?.running ?: MutableLiveData() }
    val overTime = _timer.switchMap { timer -> timer?.overTime ?: MutableLiveData() }
    val keepScreenOn = running
    val timerOptionsSelectable = running.map { !it }
    val minutes = _timer.switchMap { timer ->
        if (timer != null) {
            MediatorLiveData<Int>().apply {
                fun updateValue() {
                    this.value = when (countMode.value!!) {
                        CountMode.CountUp -> timer.secondsCountUp.value
                        CountMode.CountDown -> timer.secondsCountDown.value
                    }
                }
                
                addSource(timer.secondsCountUp) { updateValue() }
                addSource(countMode) { updateValue() }
            }
        } else {
            MutableLiveData<Int>()
        }
    }
    val seconds: LiveData<Int> = _timer.switchMap { timer ->
        if (timer != null) {
            MediatorLiveData<Int>().apply {
                fun updateValue() {
                    this.value = when (countMode.value!!) {
                        CountMode.CountUp -> timer.secondsCountUp.value
                        CountMode.CountDown -> timer.secondsCountDown.value
                    }
                }
                
                addSource(timer.secondsCountUp) { updateValue() }
                addSource(countMode) { updateValue() }
            }
        } else {
            MutableLiveData<Int>()
        }
    }
    val timerTextColor: LiveData<Int> = Transformations.switchMap(_timer) {
        it?.textColor ?: MutableLiveData()
    }
    val overTimeText: LiveData<String> = Transformations.switchMap(_timer) {
        it?.overTimeText ?: MutableLiveData()
    }
    val bellsText: LiveData<String> = _timerOption.switchMap { timerOption ->
        if (timerOption != null) {
            MediatorLiveData<String>().apply {
                fun updateValue() {
                    this.value = if (enableBells.value!!) {
                        when (countMode.value!!) {
                            CountMode.CountUp -> timerOption.countUpBellsText
                            CountMode.CountDown -> timerOption.countDownBellsText
                        }
                    } else {
                        Res.string.off
                    }
                }
                addSource(countMode) { updateValue() }
                addSource(enableBells) { updateValue() }
            }
        } else {
            MutableLiveData<String>()
        }
    }
    val timerControlButtonText: LiveData<String> = Transformations.switchMap(_timer) { timer ->
        if (timer == null) {
            MutableLiveData(Res.string.start)
        } else {
            MediatorLiveData<String>().apply {
                fun updateValue() {
                    this.value = if (timer.running.value!!) {
                        Res.string.pause
                    } else {
                        Res.string.resume
                    }
                }
                addSource(timer.running) { updateValue() }
            }
        }
        
    }
    
    private var soundPool: SoundPool
    private var debateBellOnce: Int = -1
    private var debateBellTwice: Int = -1
    
    init {
        val context = application.applicationContext
        
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
                if (Prefs.enableBells.value)
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
        val timer = _timer.value
        if (timer == null) {
            _timer.value = newTimerInstance(timerOption.value!!)
        } else {
            if (timer.started.value!!) {
                timer.setRunning(!timer.running.value!!)
            } else {
                timer.setRunning(true)
            }
        }
    }
    
    fun onResetTime() {
        timerOption.value?.let {
            onTimeButtonSelect(it.tag)
        }
    }
    
    fun onTimeButtonSelect(buttonTag: String) {
        _timer.value?.setRunning(false)
        
        _timerOption.value = timerMaps.getValue(buttonTag)
    }
    
    fun onToggleCountMode() {
        Prefs.countMode.putValue(Prefs.countMode.value.other())
    }
    
    fun onToggleDebateBells() {
        Prefs.enableBells.putValue(!Prefs.enableBells.value)
    }
}