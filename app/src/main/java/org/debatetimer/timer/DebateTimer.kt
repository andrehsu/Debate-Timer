package org.debatetimer.timer

import android.os.CountDownTimer
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.debatetimer.AppResources
import kotlin.math.absoluteValue

abstract class DebateTimer(val res: AppResources, private val config: TimerConfiguration) {
    private var countUpTotalSeconds: Int = 0
    private val countDownTotalSeconds: Int
        get() = config.totalSeconds - countUpTotalSeconds
    private var timer: CountDownTimer = newTimerInstance()
    val countUpTotalSecondsLD: MutableLiveData<Int> = MutableLiveData(0)
    
    private val _overTime = MutableLiveData(false)
    val overTime: LiveData<Boolean> = _overTime
    private val _overTimeText = MutableLiveData("")
    val overTimeText: LiveData<String> = _overTimeText
    
    
    val secondsCountUp: Int
        get() = countUpTotalSeconds % 60
    val minutesCountUp: Int
        get() = countUpTotalSeconds / 60
    
    val secondsCountDown: Int
        get() = countDownTotalSeconds.absoluteValue % 60
    val minutesCountDown: Int
        get() = countDownTotalSeconds.absoluteValue / 60
    
    private val _textColor = MutableLiveData(res.color.timerStart)
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
    
    fun skipForward() {
        val newSeconds = this.countUpTotalSeconds + 10
        this.countUpTotalSeconds = newSeconds
        updateTime(false)
    }
    
    fun skipBackward(): Boolean {
        val newSeconds = this.countUpTotalSeconds - 10
        if (newSeconds < 0) {
            return false
        }
        this.countUpTotalSeconds = newSeconds
        updateTime(false)
    
        return true
    }
    
    private fun newTimerInstance() = object : CountDownTimer(10000000L, 1000) {
        override fun onTick(millisUntilFinished: Long) {
            onSecondInternal()
        }
        
        override fun onFinish() {
        
        }
    }
    
    private fun onSecondInternal() {
//        if (countDownSeconds <= -60) {
//            pause()
//            _ended.value = true
//            return
//        }
        this.countUpTotalSeconds += 1
        updateTime()
    }
    
    private fun updateTime(ringBell: Boolean = true) {
        countUpTotalSecondsLD.value = countUpTotalSeconds
    
        if (ringBell) {
            if (countUpTotalSeconds in config.bellsCountingUp) {
                onBell(DebateBell.Once)
            }
        
            if (countDownTotalSeconds <= 0 && countDownTotalSeconds % 15 == 0) {
                onBell(DebateBell.Twice)
            }
        }
    
        _textColor.value = when {
            countUpTotalSeconds < 60 -> res.color.timerStart
            countDownTotalSeconds < 0 -> res.color.timerOvertime
            countDownTotalSeconds <= 60 -> res.color.timerEnd
            else -> res.color.timerNormal
        }
    
        _overTime.value = countDownTotalSeconds < 0
    
        if (_overTime.value == true) {
            val minutes = minutesCountUp - config.minutes
            val seconds = secondsCountUp - config.seconds
            _overTimeText.value = "%d:%02d".format(minutes, seconds)
        }
    }
    
    private fun pause() {
        timer.cancel()
        timer = newTimerInstance()
    }
    
    private fun resume() {
        timer.start()
    
        countUpTotalSeconds -= 1
        if (!started.value!!) {
            _started.value = true
        }
    }
    
    open fun onBell(debateBell: DebateBell) {}
}
