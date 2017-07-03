package andre.debatetimer

import andre.debatetimer.extensions.*
import andre.debatetimer.extensions.EnvVars.color_timerEnd
import andre.debatetimer.extensions.EnvVars.color_timerNormal
import andre.debatetimer.extensions.EnvVars.color_timerStart
import andre.debatetimer.extensions.EnvVars.init
import andre.debatetimer.extensions.EnvVars.longAnimTime
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

class MainActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {
	//<editor-fold desc="State fields and functions">
	interface State
	
	interface HasTimerOption : State {
		val timerOption: TimerOption
	}
	
	object WaitingToBegin : State
	
	class WaitingToStart(override val timerOption: TimerOption) : State, HasTimerOption
	
	inner class TimerStarted(override val timerOption: TimerOption, val timer: DebateTimer) : State, HasTimerOption {
		var running: Boolean = false
			set(value) {
				if (field != value) {
					field = value
					
					if (value) {
						timer.resume()
						window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
						buttons.forEach {
							it.isClickable = false
							it.alpha = 0.54f
						}
					} else {
						timer.pause()
						window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
						buttons.forEach {
							it.isClickable = true
							it.alpha = 1.0f
						}
					}
				}
			}
	}
	
	private var state: State = WaitingToBegin
		set(value) {
			require(value !is WaitingToBegin)
			
			val oldValue = field
			field = value
			
			if (oldValue is WaitingToBegin) {
				cl_timer.setVisible()
				bt_startPause.setVisible()
				tv_startingText.setGone()
			}
			
			if (oldValue is TimerStarted && (value !is TimerStarted || value is TimerStarted && oldValue.timer !== value.timer)) {
				oldValue.timer.pause()
			}
		}
	//</editor-fold>
	
	lateinit var soundPool: SoundPool
	var debate_bell_one: Int = -1
	var debate_bell_two: Int = -1
	
	//<editor-fold desc="Activity callbacks">
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)
		
		init(this)
		
		val attributes = AudioAttributes.Builder()
				.setUsage(AudioAttributes.USAGE_MEDIA)
				.setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
				.build()
		
		soundPool = SoundPool.Builder()
				.setMaxStreams(1)
				.setAudioAttributes(attributes)
				.build()
		
		debate_bell_one = soundPool.load(this, R.raw.debate_bell_one, 1)
		debate_bell_two = soundPool.load(this, R.raw.debate_bell_two, 1)
		
		volumeControlStream = AudioManager.STREAM_MUSIC
		
		timerTexts = listOf(tv_timerNegative, tv_timer_m, tv_timer_s, tv_timer_colon)
		buttons = mutableListOf()
		ll_timeButtons.forEachChild { child ->
			if (child is Button) {
				buttons += child
				
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
				child.setAllCaps(false)
				
				child.setOnClickListener(this::onTimeButtonClick)
			} else {
				child.setInvisible()
			}
		}
		
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
	private lateinit var buttons: List<Button>
	private lateinit var timerTexts: List<TextView>
	private var action_debateBell: MenuItem? = null
	
	private var ui_minutes: Int = 0
		set(value) {
			if (field != value) {
				field = value
				
				tv_timer_m.text = value.toString()
			}
		}
	private var ui_seconds: Int = 0
		set(value) {
			if (field != value) {
				field = value
				
				tv_timer_s.text = value.toString().padStart(2, '0')
			}
		}
	private var ui_isNegative: Boolean = false
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
	private var ui_color: Int = -1
		set(value) {
			if (field != value) {
				field = value
				
				timerTexts.forEach { it.setTextColor(value) }
			}
		}
	private var ui_timerDisplayCountUp = false
		set(value) {
			if (field != value) {
				field = value
				
				tv_timerDisplayMode.text = getString(
						if (value) R.string.timer_display_count_up else R.string.timer_display_count_down
				)
				refreshTimer()
				refreshBells()
			}
		}
	
	private fun refreshTimer() {
		val state = state
		when (state) {
			is WaitingToStart -> {
				if (ui_timerDisplayCountUp) {
					ui_minutes = 0
					ui_seconds = 0
				} else {
					ui_minutes = state.timerOption.minutesOnly
					ui_seconds = state.timerOption.secondsOnly
				}
				ui_isNegative = false
				ui_color = color_timerStart
			}
			is TimerStarted -> {
				val timer = state.timer
				if (ui_timerDisplayCountUp) {
					ui_minutes = timer.minutesSinceStart
					ui_seconds = timer.secondsSinceStart
					ui_isNegative = false
				} else {
					ui_minutes = timer.minutesLeft
					ui_seconds = timer.secondsLeft
					ui_isNegative = timer.isTimeEndNegative
				}
			}
		}
	}
	
	private fun refreshBells() {
		val state = state
		if (Prefs.debateBellEnabled && state is HasTimerOption) {
			val timerOption = state.timerOption
			
			if (timerOption.countUpString.isEmpty()) {
				tv_bellsAt.setInvisible()
			} else {
				tv_bellsAt.setVisible()
				
				val bellString = if (ui_timerDisplayCountUp) {
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
	fun onStartPauseClick(view: View) {
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
						ui_color = color_timerNormal
					}
					
					override fun onLastMinuteStart() {
						ui_color = color_timerEnd
					}
					
					override fun onOvertime() {
						ui_color = color_timerEnd
					}
					
					override fun onBell(debateBell: DebateBell) {
						if (Prefs.debateBellEnabled)
							soundPool.play(
									when (debateBell) {
										DebateBell.Once -> debate_bell_one
										DebateBell.Twice -> debate_bell_two
									},
									.99f,
									.99f,
									1,
									0,
									.99f)
					}
				}
				
				this.state = TimerStarted(state.timerOption, timer).also(::timerStarted)
			}
			is TimerStarted -> timerStarted(state)
		}
	}
	
	@Suppress("UNUSED_PARAMETER")
	fun onToggleElapsedRemainingClick(view: View) {
		ui_timerDisplayCountUp = !ui_timerDisplayCountUp
	}
	
	fun onTimeButtonClick(view: View) {
		buttons.forEach {
			it.setTextColor(getColorCompat(R.color.buttonUnselected))
		}
		
		view as Button
		view.setTextColor(getColorCompat(R.color.buttonSelected))
		
		bt_startPause.text = getString(R.string.start)
		
		val timerOption = TimerOption.parseTag(view.tag as String)
		
		val state = state
		if (state is TimerStarted) {
			state.running = false
		}
		
		this.state = WaitingToStart(timerOption)
		
		refreshTimer()
		refreshBells()
	}
	//</editor-fold>
}