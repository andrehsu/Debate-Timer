package andre.debatetimer.timer

import andre.debatetimer.Res
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlin.math.absoluteValue

abstract class DebateTimer(private val timerOption: TimerOption) {
    private var countUpSeconds: Int = 0
    private var countDownSeconds: Int = timerOption.totalSeconds
    private var timer: Timer = newTimerInstance()
    private var deciseconds: Int = 0
    
    private val bellsSinceStart = timerOption.bellsSinceStart
    
    private val _ended = MutableLiveData(false)
    val ended: LiveData<Boolean> = _ended
    private val _overTime = MutableLiveData(false)
    val overTime: LiveData<Boolean> = _overTime
    private val _overTimeText = MutableLiveData("")
    val overTimeText: LiveData<String> = _overTimeText
    
    private val _secondsCountDown = MutableLiveData(timerOption.seconds)
    val secondsCountDown: LiveData<Int> = _secondsCountDown
    private val _minutesCountDown = MutableLiveData(timerOption.minutes)
    val minutesCountDown: LiveData<Int> = _minutesCountDown
    
    private val _secondsCountUp = MutableLiveData(0)
    val secondsCountUp: LiveData<Int> = _secondsCountUp
    private val _minutesCountUp = MutableLiveData(0)
    val minutesCountUp: LiveData<Int> = _minutesCountUp
    
    private val _textColor = MutableLiveData(Res.color.timerStart)
    val textColor: LiveData<Int> = _textColor
    
    private val _running = MutableLiveData(false)
    val running: LiveData<Boolean> = _running
    
    private val _started = MutableLiveData(false)
    val started: LiveData<Boolean> = _started
    
    fun setRunning(value: Boolean) {
        if (value) {
            resume()
        } else {
            pause()
        }
        _running.value = value
    }
    
    private fun newTimerInstance() = object : Timer(100) {
        override fun onTick() {
            deciseconds++
            if (deciseconds == 10) {
                deciseconds = 0
                onSecondInternal()
            }
        }
    }
    
    private fun onSecondInternal() {
        if (countDownSeconds <= -60) {
            pause()
            _ended.value = true
            return
        }
    
        if (!started.value!!) {
            _started.value = true
        }
        
        countUpSeconds++
        countDownSeconds--
        
        val absVal = countDownSeconds.absoluteValue
    
        _minutesCountDown.value = absVal / 60
        _secondsCountDown.value = absVal % 60
    
        _minutesCountUp.value = countUpSeconds / 60
        _secondsCountUp.value = countUpSeconds % 60
        
        bellsSinceStart[countUpSeconds]?.let { onBell(it) }
        
        if (countDownSeconds <= 0 && countDownSeconds % 15 == 0) {
            onBell(DebateBell.Twice)
        }
        
        if (countUpSeconds == 60 && countDownSeconds > 60) {
            _textColor.value = Res.color.timerNormal
        }
        
        if (countDownSeconds == 60) {
            _textColor.value = Res.color.timerEnd
        }
        
        if (countDownSeconds == -1) {
            _textColor.value = Res.color.timerOvertime
            _overTime.value = true
        }
    
        if (_overTime.value == true) {
            val minutes = minutesCountUp.value!! - timerOption.minutes
            val seconds = secondsCountUp.value!! - timerOption.seconds
            _overTimeText.value = "%d:%02d".format(minutes, seconds)
        }
    }
    
    private fun pause() {
        timer.cancel()
        timer = newTimerInstance()
    }
    
    private fun resume() {
        timer.start()
    }
    
    open fun onBell(debateBell: DebateBell) {}
}

enum class DebateBell {
    Once, Twice
}