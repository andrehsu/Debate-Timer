package andre.debatetimer.timer

import andre.debatetimer.Res
import andre.debatetimer.livedata.MutableBooleanLiveData
import andre.debatetimer.livedata.MutableIntLiveData
import kotlin.math.absoluteValue

abstract class DebateTimer(timerOption: TimerOption) {
    private var countUpSeconds: Int = 0
    private var countDownSeconds: Int = timerOption.totalSeconds
    private var timer: Timer = newTimerInstance()
    private var deciseconds: Int = 0
    
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
            ended.value = true
            return
        }
        
        countUpSeconds++
        countDownSeconds--
        
        val absVal = countDownSeconds.absoluteValue
        
        minutesCountDown.value = absVal / 60
        secondsCountDown.value = absVal % 60
        
        minutesCountUp.value = countUpSeconds / 60
        secondsCountUp.value = countUpSeconds % 60
        
        bellsSinceStart[countUpSeconds]?.let { onBell(it) }
        
        if (countDownSeconds <= 0 && countDownSeconds % 15 == 0) {
            onBell(DebateBell.Twice)
        }
        
        if (countUpSeconds == 60 && countDownSeconds > 60) {
            textColor.value = Res.color.timerNormal
            onFirstMinuteEnd()
        }
        
        if (countDownSeconds == 60) {
            textColor.value = Res.color.timerEnd
            onLastMinuteStart()
        }
        
        if (countDownSeconds == -1) {
            textColor.value = Res.color.timerOvertime
            overtime.value = true
        }
        
    }
    
    private fun pause() {
        timer.cancel()
        timer = newTimerInstance()
    }
    
    private fun resume() {
        timer.start()
    }
    
    private val bellsSinceStart = timerOption.bellsSinceStart
    
    @Suppress("MemberVisibilityCanBePrivate")
    val ended = MutableBooleanLiveData(false)
    val secondsCountDown = MutableIntLiveData(timerOption.seconds)
    val minutesCountDown = MutableIntLiveData(timerOption.minutes)
    val overtime = MutableBooleanLiveData(false)
    
    val secondsCountUp = MutableIntLiveData(0)
    val minutesCountUp = MutableIntLiveData(0)
    
    val textColor = MutableIntLiveData(Res.color.timerStart)
    
    val running = MutableBooleanLiveData(false)
    
    init {
        running.observeForever { running -> if (running) resume() else pause() }
    }
    
    open fun onFirstMinuteEnd() {}
    
    open fun onLastMinuteStart() {}
    
    open fun onBell(debateBell: DebateBell) {}
}

enum class DebateBell {
    Once, Twice
}