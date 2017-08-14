package andre.debatetimer

import andre.debatetimer.EnvVars.longAnimTime
import andre.debatetimer.TimerCountMode.CountDown
import andre.debatetimer.TimerCountMode.CountUp
import andre.debatetimer.extensions.CrossfadeAnimator.Companion.crossfadeTo
import andre.debatetimer.extensions.setGone
import andre.debatetimer.extensions.setInvisible
import andre.debatetimer.extensions.setVisible
import andre.debatetimer.timer.DebateBell
import andre.debatetimer.timer.DebateTimer
import andre.debatetimer.timer.TimerOption
import android.app.Dialog
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.Button
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.defaultSharedPreferences
import org.jetbrains.anko.forEachChild

class MainActivity : AppCompatActivity(),
		SharedPreferences.OnSharedPreferenceChangeListener,
		TimerView {
	private var state: State = WaitingToBegin
		set(value) {
			require(value !is WaitingToBegin)
			if (value !== field) {
				val oldValue = field
				field = value
				
				if (oldValue is WaitingToBegin) {
					onFirstTimeButtonSelect()
				}
				
				if (oldValue is TimerStarted) {
					oldValue.running = false
				}
			}
		}
	
	private var soundPool: SoundPool
	private var debate_bell_one: Int = -1
	private var debate_bell_two: Int = -1
	
	init {
		val attributes = AudioAttributes.Builder()
				.setUsage(AudioAttributes.USAGE_MEDIA)
				.setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
				.build()
		
		soundPool = SoundPool.Builder()
				.setMaxStreams(1)
				.setAudioAttributes(attributes)
				.build()
	}
	
	//<editor-fold desc="Activity callbacks">
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)
		
		EnvVars.init(this)
		
		debate_bell_one = soundPool.load(this, R.raw.debate_bell_one, 1)
		debate_bell_two = soundPool.load(this, R.raw.debate_bell_two, 1)
		
		volumeControlStream = AudioManager.STREAM_MUSIC
		
		timerBindings = getBindings(this)
		timerBinding = NullBinding
		val timeButtons = mutableListOf<Button>()
		ll_timeButtons.forEachChild { child ->
			if (child is Button) {
				timeButtons += child
				
				val tag = child.tag as String
				
				val timerOption = TimerOption.parseTag(tag)
				
				child.text = with(timerOption) {
					if (minutesOnly != 0 && secondsOnly != 0) {
						"${minutesOnly}m${secondsOnly}s"
					} else if (minutesOnly != 0) {
						"${minutesOnly}m"
					} else if (secondsOnly != 0) {
						"${secondsOnly}s"
					} else {
						"nil"
					}
				}
				child.setTextColor(getColorCompat(R.color.buttonUnselected))
				
				child.setOnClickListener(this::onTimeButtonSelect)
			} else {
				child.setInvisible()
			}
		}
		this.timeButtons = timeButtons
		
		setupSharedPreference()
	}
	
	override fun onBackPressed() {
		class ExitDialogFragment : DialogFragment() {
			override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
				return AlertDialog.Builder(activity)
						.setTitle(R.string.exit_question)
						.setPositiveButton(android.R.string.yes, { _, _ -> activity.finish() })
						.setNegativeButton(android.R.string.no, { _, _ -> dialog.cancel() })
						.create()
			}
		}
		
		if (isTaskRoot) {
			ExitDialogFragment().show(supportFragmentManager, null)
		} else {
			super.onBackPressed()
		}
	}
	
	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		menuInflater.inflate(R.menu.activity_main, menu)
		action_debateBell = menu.findItem(R.id.action_debate_bell)
		
		updateDebateBellIcon()
		return true
	}
	
	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		when (item.itemId) {
			R.id.action_debate_bell -> {
				Prefs.debateBellEnabled = !Prefs.debateBellEnabled
				
				refreshBells()
			}
			else -> return super.onOptionsItemSelected(item)
		}
		
		return true
	}
	
	override fun onDestroy() {
		super.onDestroy()
		val state = state
		if (state is TimerStarted) {
			state.timer.pause()
		}
		
		defaultSharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
	}
	//</editor-fold>
	
	//<editor-fold desc="SharedPreference">
	private fun setupSharedPreference() {
		val sharedPreference = defaultSharedPreferences
		
		Prefs.init(this)
		
		updateDebateBellIcon()
		
		sharedPreference.registerOnSharedPreferenceChangeListener(this)
	}
	
	override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
		when (key) {
			Prefs.pref_bell_enabled_key -> updateDebateBellIcon()
		}
	}
	
	private fun updateDebateBellIcon() {
		action_debateBell?.icon = getDrawable(
				if (Prefs.debateBellEnabled) {
					R.drawable.ic_notifications_active_white_24dp
				} else {
					R.drawable.ic_notifications_off_white_24dp
				}
		)
	}
	//</editor-fold>
	
	//<editor-fold desc="UI fields and functions">
	private lateinit var timeButtons: List<Button>
	private lateinit var timerBindings: Map<TimerDisplayMode, TimerBinding>
	private var action_debateBell: MenuItem? = null
	
	override var timerMinutes: Int = 0
		set(value) {
			if (field != value) {
				field = value
				
				timerBinding.minutes = value
			}
		}
	override var timerSeconds: Int = 0
		set(value) {
			if (field != value) {
				field = value
				timerBinding.seconds = value
			}
		}
	override var timerTextColor: Int = -1
		set(value) {
			if (field != value) {
				field = value
				timerBinding.color = field
			}
		}
	override var timerCountMode = TimerCountMode.CountDown
		set(value) {
			if (field != value) {
				field = value
				
				tv_timerCountMode.text = getString(
						if (timerCountMode == CountUp) R.string.timer_display_count_up else R.string.timer_display_count_down
				)
				refreshTimer()
				refreshBells()
				updateTimerBinding()
			}
		}
	lateinit var timerBinding: TimerBinding
	
	fun updateTimerBinding() {
		val state = state
		when {
			state is TimerStarted && state.ended -> timerBinding = timerBindings.getValue(TimerDisplayMode.End)
			state is TimerStarted && state.timer.isTimeEndNegative -> timerBinding = timerBindings.getValue(TimerDisplayMode.Negative)
			else -> timerBinding = timerBindings.getValue(TimerDisplayMode.Normal)
		}
		for (timerBinding in timerBindings.values) {
			timerBinding.isVisible = timerBinding.timerDisplayMode == this.timerBinding.timerDisplayMode
		}
	}
	
	override var buttonsActive: Boolean = false
		set(value) {
			field = value
			if (value) {
				timeButtons.forEach {
					it.isClickable = false
					it.alpha = 0.54f
				}
			} else {
				timeButtons.forEach {
					it.isClickable = true
					it.alpha = 1.0f
				}
			}
		}
	override var keepScreenOn: Boolean = false
		set(value) {
			field = value
			if (value) {
				window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
			} else {
				window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
			}
		}
	
	override fun refreshTimer() {
		val state = state
		updateTimerBinding()
		when (state) {
			is WaitingToStart -> {
				if (timerCountMode == CountUp) {
					timerMinutes = 0
					timerSeconds = 0
				} else {
					timerMinutes = state.timerOption.minutesOnly
					timerSeconds = state.timerOption.secondsOnly
				}
				timerTextColor = EnvVars.color_timerStart
			}
			is TimerStarted -> {
				val timer = state.timer
				if (timerCountMode == CountUp) {
					timerMinutes = timer.minutesSinceStart
					timerSeconds = timer.secondsSinceStart
				} else {
					timerMinutes = timer.minutesLeft
					timerSeconds = timer.secondsLeft
				}
			}
		}
	}
	
	override fun refreshBells() {
		val state = state
		if (Prefs.debateBellEnabled && state is HasTimerOption) {
			val timerOption = state.timerOption
			
			if (timerOption.countUpString.isEmpty()) {
				tv_bellsAt.setInvisible()
			} else {
				tv_bellsAt.setVisible()
				
				val bellString = if (timerCountMode == CountUp) {
					timerOption.countUpString
				} else {
					timerOption.countDownString
				}
				
				tv_bellsAt.text = resources.getQuantityString(R.plurals.bells_at, timerOption.bellsSinceStart.count(), bellString)
			}
		} else {
			tv_bellsAt.setInvisible()
		}
	}
	
	@Suppress("UNUSED_PARAMETER")
	override fun onStartPause(view: View) {
		fun timerStarted(state: TimerStarted) {
			if (state.running) {
				bt_startPause.text = getString(R.string.resume)
				state.running = false
			} else {
				bt_startPause.text = getString(R.string.pause)
				state.running = true
			}
		}
		
		bt_startPause crossfadeTo bt_startPause withDuration longAnimTime
		val state = state
		when (state) {
			is WaitingToStart -> {
				val timer = newTimerInstance(state.timerOption)
				this.state = TimerStarted(this, state.timerOption, timer).also(::timerStarted)
			}
			is TimerStarted -> timerStarted(state)
		}
	}
	
	fun newTimerInstance(timerOption: TimerOption): DebateTimer {
		return object : DebateTimer(timerOption) {
			override fun onSecond() = refreshTimer()
			
			override fun onFirstMinuteEnd() {
				timerTextColor = EnvVars.color_timerNormal
			}
			
			override fun onLastMinuteStart() {
				timerTextColor = EnvVars.color_timerEnd
			}
			
			override fun onOvertime() {
				timerTextColor = EnvVars.color_timerEnd
				updateTimerBinding()
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
				this@MainActivity.state.let {
					if (it is TimerStarted) {
						it.ended = true
					}
				}
				updateTimerBinding()
			}
		}
	}
	
	@Suppress("UNUSED_PARAMETER")
	override fun onToggleDisplayMode(view: View) {
		if (timerCountMode == CountUp) {
			timerCountMode = CountDown
		} else {
			timerCountMode = CountUp
		}
	}
	
	override fun onTimeButtonSelect(view: View) {
		timeButtons.forEach {
			it.setTextColor(getColorCompat(R.color.buttonUnselected))
		}
		
		view as Button
		view.setTextColor(getColorCompat(R.color.buttonSelected))
		
		bt_startPause.text = getString(R.string.start)
		
		val timerOption = TimerOption.parseTag(view.tag as String)
		
		this.state = WaitingToStart(timerOption)
		
		refreshTimer()
		refreshBells()
	}
	
	override fun onFirstTimeButtonSelect() {
		fl_timer.setVisible()
		bt_startPause.setVisible()
		tv_startingText.setGone()
	}
	
	//</editor-fold>
}