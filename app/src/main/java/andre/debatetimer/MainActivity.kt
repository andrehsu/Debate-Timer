package andre.debatetimer

import andre.debatetimer.EnvVars.longAnimTime
import andre.debatetimer.TimerDisplayMode.CountDown
import andre.debatetimer.TimerDisplayMode.CountUp
import andre.debatetimer.extensions.*
import andre.debatetimer.timer.DebateBell
import andre.debatetimer.timer.DebateTimer
import andre.debatetimer.timer.TimerOption
import android.app.Dialog
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.timer.*
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
		
		timerTexts = listOf(tv_timerNegative, tv_timer_m, tv_timer_s, tv_timer_colon)
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
	private lateinit var timerTexts: List<TextView>
	private var action_debateBell: MenuItem? = null
	
	override var timerMinutes: Int = 0
		set(value) {
			if (field != value) {
				field = value
				
				tv_timer_m.text = value.toString()
			}
		}
	override var timerSeconds: Int = 0
		set(value) {
			if (field != value) {
				field = value
				
				tv_timer_s.text = value.toString().padStart(2, '0')
			}
		}
	override var timerIsNegative: Boolean = false
		set(value) {
			if (field != value) {
				field = value
				
				if (value) {
					tv_timerNegative.setVisible()
					tv_timer_m.setGone()
					tv_timer_colon.setGone()
					guideline.layoutParams = (guideline.layoutParams as ConstraintLayout.LayoutParams).apply { guidePercent = 0.36f }
				} else {
					tv_timerNegative.setGone()
					tv_timer_m.setVisible()
					tv_timer_colon.setVisible()
					guideline.layoutParams = (guideline.layoutParams as ConstraintLayout.LayoutParams).apply { guidePercent = 0.43f }
				}
			}
		}
	override var timerTextColor: Int = -1
		set(value) {
			if (field != value) {
				field = value
				
				timerTexts.forEach { it.setTextColor(value) }
			}
		}
	override var timerDisplayMode = TimerDisplayMode.CountDown
		set(value) {
			if (field != value) {
				field = value
				
				tv_timerDisplayMode.text = getString(
						if (timerDisplayMode == CountUp) R.string.timer_display_count_up else R.string.timer_display_count_down
				)
				refreshTimer()
				refreshBells()
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
		when (state) {
			is WaitingToStart -> {
				if (timerDisplayMode == CountUp) {
					timerMinutes = 0
					timerSeconds = 0
				} else {
					timerMinutes = state.timerOption.minutesOnly
					timerSeconds = state.timerOption.secondsOnly
				}
				timerIsNegative = false
				timerTextColor = EnvVars.color_timerStart
			}
			is TimerStarted -> {
				val timer = state.timer
				if (timerDisplayMode == CountUp) {
					timerMinutes = timer.minutesSinceStart
					timerSeconds = timer.secondsSinceStart
					timerIsNegative = false
				} else {
					timerMinutes = timer.minutesLeft
					timerSeconds = timer.secondsLeft
					timerIsNegative = timer.isTimeEndNegative
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
				
				val bellString = if (timerDisplayMode == CountUp) {
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
				val timer = object : DebateTimer(state.timerOption) {
					override fun onSecond() = refreshTimer()
					
					override fun onFirstMinuteEnd() {
						timerTextColor = EnvVars.color_timerNormal
					}
					
					override fun onLastMinuteStart() {
						timerTextColor = EnvVars.color_timerEnd
					}
					
					override fun onOvertime() {
						timerTextColor = EnvVars.color_timerEnd
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
				}
				
				this.state = TimerStarted(this, state.timerOption, timer).also(::timerStarted)
			}
			is TimerStarted -> timerStarted(state)
		}
	}
	
	@Suppress("UNUSED_PARAMETER")
	override fun onToggleDisplayMode(view: View) {
		if (timerDisplayMode == CountUp) {
			timerDisplayMode = CountDown
		} else {
			timerDisplayMode = CountUp
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
		cl_timer.setVisible()
		bt_startPause.setVisible()
		tv_startingText.setGone()
	}
	
	//</editor-fold>
}