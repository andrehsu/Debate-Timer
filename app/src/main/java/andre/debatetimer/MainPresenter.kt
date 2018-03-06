package andre.debatetimer

import andre.debatetimer.extensions.defaultSharedPreferences
import andre.debatetimer.timer.DebateBell
import andre.debatetimer.timer.DebateTimer
import andre.debatetimer.timer.TimerOption
import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.SoundPool
import android.view.View
import android.widget.Button

class MainPresenter(override var view: IMainView) : IMainPresenter, SharedPreferences.OnSharedPreferenceChangeListener {
	private var soundPool: SoundPool
	private var debate_bell_one: Int = -1
	private var debate_bell_two: Int = -1
	
	override var state: State = WaitingToBegin
		set(value) {
			if (value == WaitingToBegin) {
				view.resetBegan()
			} else {
				view.setBegan()
				val oldValue = field
				field = value
				
				if (oldValue is TimerStarted) {
					oldValue.setRunning(view, false)
				}
			}
		}
	
	
	init {
		view.presenter = this
		
		val attributes = AudioAttributes.Builder()
				.setUsage(AudioAttributes.USAGE_MEDIA)
				.setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
				.build()
		soundPool = SoundPool.Builder()
				.setMaxStreams(1)
				.setAudioAttributes(attributes)
				.build()
		
		debate_bell_one = soundPool.load(view.context, R.raw.debate_bell_one, 1)
		debate_bell_two = soundPool.load(view.context, R.raw.debate_bell_two, 1)
		
		EnvVars.init(view.context)
		setupSharedPreference(view.context)
	}
	
	private fun setupSharedPreference(context: Context) {
		Prefs.init(context)
		
		context.defaultSharedPreferences.registerOnSharedPreferenceChangeListener(this)
	}
	
	override fun onDestroy(context: Context) {
		context.defaultSharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
	}
	
	override fun newTimerInstance(timerOption: TimerOption): DebateTimer {
		return object : DebateTimer(timerOption) {
			override fun onSecond() = view.refreshTimer()
			
			override fun onFirstMinuteEnd() {
				view.timerTextColor = EnvVars.color_timerNormal
			}
			
			override fun onLastMinuteStart() {
				view.timerTextColor = EnvVars.color_timerEnd
			}
			
			override fun onOvertime() {
				view.timerTextColor = EnvVars.color_timerEnd
				view.updateTimerBinding()
			}
			
			override fun onBell(debateBell: DebateBell) {
				if (Prefs.debateBellEnabled)
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
			
			override fun onEnd() {
				state.let {
					if (it is TimerStarted) {
						it.ended = true
					}
				}
				view.updateTimerBinding()
			}
		}
	}
	
	override fun onStartPause(@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE") v: View) {
		fun timerStarted(state: TimerStarted) {
			if (state.running) {
				view.tv_startPauseText = view.context.getString(R.string.resume)
				state.setRunning(view, false)
			} else {
				view.tv_startPauseText = view.context.getString(R.string.pause)
				state.setRunning(view, true)
			}
		}
		
		view.crossfadeStartPause()
		val state = state
		when (state) {
			is WaitingToStart -> {
				val timer = newTimerInstance(state.timerOption)
				this.state = TimerStarted(state.timerOption, timer).also(::timerStarted)
			}
			is TimerStarted -> timerStarted(state)
		}
	}
	
	
	override fun onToggleDisplayMode() {
		Prefs.countMode = if (Prefs.countMode == CountMode.CountUp) CountMode.CountDown else CountMode.CountUp
	}
	
	override fun onTimeButtonSelect(@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE") v: View) {
		v as Button
		
		view.setTimeButtonAsSelected(v)
		
		view.tv_startPauseText = view.context.getString(R.string.start)
		
		state = WaitingToStart(view.timerButtons[v]!!)
		
		view.refreshTimer()
		view.refreshBells()
	}
	
	override fun onSharedPreferenceChanged(sp: SharedPreferences, key: String) {
		when (key) {
			Prefs.pref_bell_enabled_key -> {
				view.refreshBells()
				view.updateDebateBellIcon()
			}
			Prefs.pref_count_mode -> {
				view.timerCountMode = Prefs.countMode
				view.refreshUpdateCountModeTitle()
			}
		}
	}
}