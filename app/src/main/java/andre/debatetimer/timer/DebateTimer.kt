package andre.debatetimer.timer

import andre.debatetimer.EnvVars
import andre.debatetimer.extensions.abs
import andre.debatetimer.livedata.BooleanLiveData
import andre.debatetimer.livedata.IntLiveData

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
			onEnd()
			return
		}
		
		countUpSeconds++
		countDownSeconds--
		
		val absVal = countDownSeconds.abs()
		
		isTimeEndNegative.value = countDownSeconds < 0
		secondsCountDown.value = absVal % 60
		minutesCountDown.value = absVal / 60
		
		secondsCountUp.value = countUpSeconds % 60
		minutesCountUp.value = countUpSeconds / 60
		
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
			onOvertime()
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
	
	var isTimeEndNegative = BooleanLiveData()
		private set
	var secondsCountDown = IntLiveData(timerOption.seconds)
		private set
	var minutesCountDown = IntLiveData(timerOption.minutes)
		private set
	
	var secondsCountUp = IntLiveData()
		private set
	var minutesCountUp = IntLiveData()
		private set
	
	var textColor = IntLiveData(EnvVars.color_timerStart)
		private set
	
	
	open fun onFirstMinuteEnd() {}
	
	open fun onLastMinuteStart() {}
	
	open fun onOvertime() {}
	
	open fun onEnd() {}
	
	open fun onBell(debateBell: DebateBell) {}
	
	override fun toString(): String = "DebateTimer"
}

enum class DebateBell {
	Once, Twice
}