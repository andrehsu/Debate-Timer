package andre.debatetimer.timer

import andre.debatetimer.EnvVars
import andre.debatetimer.livedata.BooleanLiveData
import andre.debatetimer.livedata.IntLiveData
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
        
        negative.value = countDownSeconds < 0
        minutesCountDown.value = absVal / 60
        secondsCountDown.value = absVal % 60
        
        minutesCountUp.value = countUpSeconds / 60
        secondsCountUp.value = countUpSeconds % 60
        
        bellsSinceStart[countUpSeconds]?.let { onBell(it) }
        
        if (countDownSeconds <= 0 && countDownSeconds % 15 == 0) {
            onBell(DebateBell.Twice)
        }
        
        if (countUpSeconds == 60 && countDownSeconds > 60) {
            textColor.value = EnvVars.color_timerNormal
            onFirstMinuteEnd()
        }
        
        if (countDownSeconds == 60) {
            textColor.value = EnvVars.color_timerEnd
            onLastMinuteStart()
        }
        
        if (countDownSeconds == -1) {
            textColor.value = EnvVars.color_timerEnd
            overtime.value = true
        }
        
    }
    
    fun pause() {
        timer.cancel()
        timer = newTimerInstance()
    }
    
    fun resume() {
        timer.start()
    }
    
    private val bellsSinceStart = timerOption.bellsSinceStart
    
    val negative = BooleanLiveData()
    val ended = BooleanLiveData()
    val secondsCountDown = IntLiveData(timerOption.seconds)
    val minutesCountDown = IntLiveData(timerOption.minutes)
    val overtime = BooleanLiveData()
    
    val secondsCountUp = IntLiveData()
    val minutesCountUp = IntLiveData()
    
    val textColor = IntLiveData(EnvVars.color_timerStart)
    
    open fun onFirstMinuteEnd() {}
    
    open fun onLastMinuteStart() {}
    
    open fun onBell(debateBell: DebateBell) {}
}

enum class DebateBell {
    Once, Twice
}