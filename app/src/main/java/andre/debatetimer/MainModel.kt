package andre.debatetimer

import andre.debatetimer.livedata.StringLiveData
import andre.debatetimer.timer.DebateBell
import andre.debatetimer.timer.DebateTimer
import andre.debatetimer.timer.TimerOption
import android.app.Application
import android.media.AudioAttributes
import android.media.SoundPool
import android.widget.Button
import androidx.lifecycle.AndroidViewModel

class MainModel(application: Application) : AndroidViewModel(application) {
	private var soundPool: SoundPool
	private var debate_bell_one: Int = -1
	private var debate_bell_two: Int = -1
	
	var selectedButton = StringLiveData()
	
	val state = LiveState(InitState)
	
	lateinit var timerMaps: Map<String, TimerOption>
	
	init {
		val context = application.applicationContext
		
		EnvVars.init(context)
		Prefs.init(context)
		
		val attributes = AudioAttributes.Builder()
				.setUsage(AudioAttributes.USAGE_MEDIA)
				.setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
				.build()
		soundPool = SoundPool.Builder()
				.setMaxStreams(1)
				.setAudioAttributes(attributes)
				.build()
		
		debate_bell_one = soundPool.load(context, R.raw.debate_bell_one, 1)
		debate_bell_two = soundPool.load(context, R.raw.debate_bell_two, 1)
	}
	
	
	private fun newTimerInstance(timerOption: TimerOption): DebateTimer {
		return object : DebateTimer(timerOption) {
			override fun onBell(debateBell: DebateBell) {
				if (Prefs.debateBellEnabled.value)
					soundPool.play(
							when (debateBell) {
								DebateBell.Once -> debate_bell_one
								DebateBell.Twice -> debate_bell_two
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
		
		val state = state.value
		when (state) {
			is WaitingToStart -> {
				val timer = newTimerInstance(state.timerOption)
				this.state.value = TimerStarted(state.timerOption, timer).also(::toggleRunning)
			}
			is TimerStarted -> toggleRunning(state)
		}
	}
	
	
	fun onTimeButtonSelect(buttonStr:String) {
		selectedButton.value = buttonStr
		state.value = WaitingToStart(timerMaps.getValue(buttonStr))
	}
}