package org.debatetimer.timer

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.debatetimer.AppResources
import kotlin.math.absoluteValue

abstract class DebateTimer(context: Context, val timerConfig: TimerConfiguration) {
    private val res = AppResources.getInstance(context)
    
    private var countUpSeconds: Int = 0
    private val countDownSeconds: Int
        get() = timerConfig.totalSeconds - countUpSeconds
    private var timer: Timer = newTimerInstance()
    private var deciseconds: Int = 0
    
    private val bellsSinceStart = timerConfig.bellsSinceStart
    
    //    private val _ended = MutableLiveData(false)
//    val ended: LiveData<Boolean> = _ended
    private val _overTime = MutableLiveData(false)
    val overTime: LiveData<Boolean> = _overTime
    private val _overTimeText = MutableLiveData("")
    val overTimeText: LiveData<String> = _overTimeText
    
    private val _secondsCountDown = MutableLiveData(timerConfig.seconds)
    val secondsCountDown: LiveData<Int> = _secondsCountDown
    private val _minutesCountDown = MutableLiveData(timerConfig.minutes)
    val minutesCountDown: LiveData<Int> = _minutesCountDown
    
    private val _secondsCountUp = MutableLiveData(0)
    val secondsCountUp: LiveData<Int> = _secondsCountUp
    private val _minutesCountUp = MutableLiveData(0)
    val minutesCountUp: LiveData<Int> = _minutesCountUp
    
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
        val newSeconds = countUpSeconds + 10
        countUpSeconds = newSeconds
        updateTime(false)
    }
    
    fun skipBackward(): Boolean {
        val newSeconds = countUpSeconds - 10
        if (newSeconds < 0) {
            return false
        }
        countUpSeconds = newSeconds
        updateTime(false)
        
        return true
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
//        if (countDownSeconds <= -60) {
//            pause()
//            _ended.value = true
//            return
//        }
        countUpSeconds++
        
        updateTime()
    }
    
    private fun updateTime(ringBell: Boolean = true) {
        val absVal = countDownSeconds.absoluteValue
        
        _minutesCountDown.value = absVal / 60
        _secondsCountDown.value = absVal % 60
        
        _minutesCountUp.value = countUpSeconds / 60
        _secondsCountUp.value = countUpSeconds % 60
        
        if (ringBell) {
            bellsSinceStart[countUpSeconds]?.let { onBell(it) }
            
            if (countDownSeconds <= 0 && countDownSeconds % 15 == 0) {
                onBell(DebateBell.Twice)
            }
        }
        
        _textColor.value = when {
            countUpSeconds < 60 -> res.color.timerStart
            countDownSeconds < 0 -> res.color.timerOvertime
            countDownSeconds <= 60 -> res.color.timerEnd
            else -> res.color.timerNormal
        }
        
        if (countDownSeconds < 0) {
            _overTime.value = true
        }
        
        if (_overTime.value == true) {
            val minutes = minutesCountUp.value!! - timerConfig.minutes
            val seconds = secondsCountUp.value!! - timerConfig.seconds
            _overTimeText.value = "%d:%02d".format(minutes, seconds)
        }
    }
    
    private fun pause() {
        timer.cancel()
        timer = newTimerInstance()
    }
    
    private fun resume() {
        timer.start()
        if (!started.value!!) {
            _started.value = true
        }
    }
    
    open fun onBell(debateBell: DebateBell) {}
}
