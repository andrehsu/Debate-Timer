package andre.debatetimer

import andre.debatetimer.CountMode.CountUp
import andre.debatetimer.extensions.*
import andre.debatetimer.timer.TimerOption
import android.app.Dialog
import android.media.AudioManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
	private lateinit var model: MainModel
	
	private lateinit var timerBindings: Map<TimerDisplayMode, TimerBinding>
	private lateinit var timerButtons: List<Button>
	private var action_debateBell: MenuItem? = null
	private var action_countMode: MenuItem? = null
	
	private var timerMinutes: Int = 0
		set(value) {
			field = value
			timerBinding.minutes = value
		}
	private var timerSeconds: Int = 0
		set(value) {
			field = value
			timerBinding.seconds = value
		}
	private var timerTextColor: Int = 0
		set(value) {
			field = value
			timerBinding.color = value
		}
	private var timerCountMode: CountMode = CountUp
		set(value) {
			field = value
			tv_timerCountMode.text = getString(
					if (timerCountMode == CountUp) R.string.timer_display_count_up else R.string.timer_display_count_down
			)
		}
	private var timerBinding: TimerBinding = NullBinding
		set(value) {
			field = value
			for (timerBinding in timerBindings.values) {
				timerBinding.isVisible = timerBinding.timerDisplayMode == this.timerBinding.timerDisplayMode
			}
		}
	
	private var buttonsActive: Boolean = false
		set(value) {
			field = value
			if (value) {
				timerButtons.forEach {
					it.isClickable = false
					it.alpha = 0.54f
				}
			} else {
				timerButtons.forEach {
					it.isClickable = true
					it.alpha = 1.0f
				}
			}
		}
	private var keepScreenOn: Boolean = false
		set(value) {
			field = value
			if (value) {
				window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
			} else {
				window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
			}
		}
	
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)
		
		model = ViewModelProviders.of(this).get(MainModel::class.java)
		
		timerBindings = getBindings(this)
		
		fl_timer.setOnClickListener({
			tv_startPause.fadeOut(EnvVars.longAnimTime)
			model.onStartPause()
			tv_startPause.fadeIn(EnvVars.longAnimTime)
		})
		
		val sp = defaultSharedPreferences
		
		val timersStr = sp.getStringSet(getString(R.string.pref_timers), mutableSetOf(
				"180;-1",
				"240;-1",
				"300;-1",
				"360;-1",
				"420;-1",
				"480;-1")).toList().sorted()
		
		val timerMaps = mutableMapOf<String, TimerOption>()
		val timerButtons = mutableListOf<Button>()
		
		timersStr.forEach { str ->
			val timerOption = TimerOption.parseTag(str)
			
			if (timerOption != null) {
				val layout = layoutInflater.inflate(R.layout.timer_button, ll_timeButtons, false) as Button
				
				layout.text = with(timerOption) {
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
				layout.setTextColor(getColorCompat(R.color.buttonUnselected))
				layout.setOnClickListener(::onTimeButtonSelect)
				ll_timeButtons.addView(layout)
				timerMaps[layout.text.toString()] = timerOption
				timerButtons += layout
			}
		}
		model.timerMaps = timerMaps
		
		this.timerButtons = timerButtons
		
		volumeControlStream = AudioManager.STREAM_MUSIC
		
		model.state.observe(this, Observer { state ->
			when (state) {
				is InitState -> resetBegan()
				else -> setBegan()
			}
			
			when (state) {
				is InitState -> {
					timerBinding = NullBinding
				}
				is HasTimerOption -> {
					val bellString = if (timerCountMode == CountUp) {
						state.timerOption.countUpString
					} else {
						state.timerOption.countDownString
					}
					tv_bellsAt.text = resources.getQuantityString(R.plurals.bells_at, state.timerOption.bellsSinceStart.count(), bellString)
					
					when (state) {
						is WaitingToStart -> {
							tv_startPause.text = getString(R.string.start)
							timerBinding = timerBindings[TimerDisplayMode.Normal]!!
							timerTextColor = EnvVars.color_timerStart
							
							if (timerCountMode == CountUp) {
								timerMinutes = 0
								timerSeconds = 0
							} else {
								timerMinutes = state.timerOption.minutesOnly
								timerSeconds = state.timerOption.secondsOnly
							}
						}
						is TimerStarted -> {
							state.timer.isTimeEndNegative.observe(this) { isTimeEndNegative ->
								timerBinding = if (isTimeEndNegative) {
									timerBindings.getValue(TimerDisplayMode.Negative)
								} else {
									timerBindings.getValue(TimerDisplayMode.Normal)
								}
							}
							
							state.ended.observe(this) { ended ->
								timerBinding = if (ended) {
									timerBindings.getValue(TimerDisplayMode.End)
								} else {
									timerBindings.getValue(TimerDisplayMode.Normal)
								}
							}
							
							state.timer.minutesSinceStart.observe(this) {
								timerMinutes = if (timerCountMode == CountUp) {
									state.timer.minutesSinceStart.value
								} else {
									state.timer.minutesLeft.value
								}
							}
							
							state.timer.secondsSinceStart.observe(this) {
								timerSeconds = if (timerCountMode == CountUp) {
									state.timer.secondsSinceStart.value
								} else {
									state.timer.secondsLeft.value
								}
							}
							
							state.timer.textColor.observe(this) { textColor ->
								timerTextColor = textColor
							}
							
							state.running.observe(this) { running ->
								tv_startPause.text = if (running) {
									getString(R.string.pause)
								} else {
									getString(R.string.resume)
								}
								buttonsActive = running
								keepScreenOn = running
							}
						}
					}
				}
			}
			
			Prefs.countMode.observe(this) { countMode ->
				timerCountMode = countMode
			}
		})
		Prefs.debateBellEnabled.observe(this) { debateBellEnabled ->
			if (debateBellEnabled) {
				tv_bellsAt.setVisible()
				action_debateBell?.icon = getDrawable(R.drawable.ic_notifications_active_white_24dp)
			} else {
				tv_bellsAt.setInvisible()
				action_debateBell?.icon = getDrawable(R.drawable.ic_notifications_off_white_24dp)
			}
			
		}
		Prefs.countMode.observe(this) { countMode ->
			action_countMode?.title = getString(
					if (countMode == CountUp) {
						R.string.show_time_remaining
					} else {
						R.string.show_time_elapsed
					}
			)
		}
		
		model.selectedButton.observe(this) { txt ->
			timerButtons.forEach {
				if (it.text == txt) {
					it.setTextColor(getColorCompat(R.color.buttonSelected))
				} else {
					it.setTextColor(getColorCompat(R.color.buttonUnselected))
				}
			}
			
		}
	}
	
	private fun onTimeButtonSelect(v: View) {
		v as Button
		model.onTimeButtonSelect(v)
	}
	
	override fun onBackPressed() {
		class ExitDialogFragment : androidx.fragment.app.DialogFragment() {
			override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
				return AlertDialog.Builder(this@MainActivity)
						.setTitle(R.string.exit_question)
						.setPositiveButton(android.R.string.yes, { _, _ -> this@MainActivity.finish() })
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
		action_countMode = menu.findItem(R.id.action_count_mode)
		
		return true
	}
	
	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		when (item.itemId) {
			R.id.action_debate_bell -> {
				Prefs.debateBellEnabled.apply(!Prefs.debateBellEnabled.value)
			}
			R.id.action_count_mode -> {
				Prefs.countMode.apply(if (Prefs.countMode.value == CountUp) CountMode.CountDown else CountUp)
			}
			else -> return super.onOptionsItemSelected(item)
		}
		
		return true
	}
	
	private fun setBegan() {
		fl_timer.setVisible()
		tv_startPause.setVisible()
		tv_timerCountMode.setVisible()
		tv_startingText.setGone()
	}
	
	private fun resetBegan() {
		fl_timer.setInvisible()
		tv_startPause.setInvisible()
		tv_timerCountMode.setInvisible()
		
		tv_startingText.setVisible()
		
		tv_startPause.text = getString(R.string.start)
	}
}