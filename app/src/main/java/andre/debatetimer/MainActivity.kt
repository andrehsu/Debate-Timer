package andre.debatetimer

import andre.debatetimer.CountMode.CountUp
import andre.debatetimer.databinding.ActivityMainBinding
import andre.debatetimer.databinding.TimerButtonBinding
import andre.debatetimer.extensions.defaultSharedPreferences
import andre.debatetimer.extensions.setGone
import andre.debatetimer.extensions.setVisible
import andre.debatetimer.timer.TimerOption
import android.app.Dialog
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.transition.TransitionManager
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {
    private lateinit var model: MainModel
    private lateinit var binding: ActivityMainBinding
    
    private lateinit var timerButtons: List<Button>
    /**
     * Property that controls whether the timer option buttons are selectable
     */
    private var timerOptionButtonsSelectable: Boolean = true
        set(value) {
            field = value
            timerButtons.forEach {
                DataBindingUtil.getBinding<TimerButtonBinding>(it)!!.selectable = value
            }
        }
    /**
     * Property that controls whether the screen is kept on
     */
    private var keepScreenOn: Boolean = false
        set(value) {
            field = value
            if (value) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    
    /**
     * onCreate method
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        model = ViewModelProviders.of(this).get(MainModel::class.java)
    
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
    
        binding.viewmodel = model
        
        val sp = defaultSharedPreferences
        
        val timersStr = sp.getStringSet(getString(R.string.pref_timers), mutableSetOf(
                "180;-1",
                "240;-1",
                "300;-1",
                "360;-1",
                "420;-1",
                "480;-1"))!!.sorted()
        
        val timerMaps = mutableMapOf<String, TimerOption>()
        val timerButtons = mutableListOf<Button>()
        
        timersStr.forEach { str ->
            val timerOption = TimerOption.parseTag(str)
            
            if (timerOption != null) {
                val timerButtonBinding: TimerButtonBinding = DataBindingUtil.inflate(layoutInflater, R.layout.timer_button, ll_timeButtons, true)
    
                timerButtonBinding.viewmodel = model
    
                val buttonTextSb = StringBuilder()
                with(timerOption) {
                    if (minutes != 0) {
                        buttonTextSb.append("${minutes}m")
                    }
                    if (seconds != 0) {
                        buttonTextSb.append("${seconds}s")
                    }
                }
                timerButtonBinding.text = buttonTextSb.toString()
    
                timerButtonBinding.textColor = getColorCompat(R.color.buttonUnselected)
                timerMaps[timerButtonBinding.text!!] = timerOption
                timerButtons += timerButtonBinding.root as Button
            }
        }
        model.timerMaps = timerMaps
        
        this.timerButtons = timerButtons
        
        Prefs.debateBellEnabled.observe(this, Observer { debateBellEnabled ->
            updateBellsText()
            binding.enableBells = debateBellEnabled
        })
        
        Prefs.countMode.observe(this, Observer { countMode ->
            updateBellsText()
            updateTime()
            binding.countMode = countMode
        })
    
        model.state.observe(this, stateObserver)
        
        model.selectedButtonStr.observe(this) { txt ->
            updateTime()
            timerButtons.forEach {
                if (it.text == txt) {
                    it.setTextColor(getColorCompat(R.color.buttonSelected))
                } else {
                    it.setTextColor(getColorCompat(R.color.buttonUnselected))
                }
            }
        }
    }
    
    /**
     * Updates the bell text shown on the toggle bell button
     */
    private fun updateBellsText() {
        val state = model.state.value
        bt_debateBell.text = when {
            Prefs.debateBellEnabled.value && state is InitState -> "On"
            Prefs.debateBellEnabled.value && state is HasTimerOption -> getString(
                    R.string.bells_at,
                    if (Prefs.countMode.value == CountUp) state.timerOption.countUpString else state.timerOption.countDownString
            )
            else -> "Off"
        }
    }
    
    /**
     * Updates timer time
     */
    private fun updateTime() {
        val state = model.state.value
        binding.minutes = when (state) {
            is InitState -> 0
            is WaitingToStart ->
                if (Prefs.countMode.value == CountUp) {
                    0
                } else {
                    state.timerOption.minutes
                }
            is TimerStarted ->
                if (Prefs.countMode.value == CountUp) {
                    state.timer.minutesCountUp.value
                } else {
                    state.timer.minutesCountDown.value
                }
        }
        binding.seconds = when (state) {
            is InitState -> 0
            is WaitingToStart ->
                if (Prefs.countMode.value == CountUp) {
                    0
                } else {
                    state.timerOption.seconds
                }
            is TimerStarted ->
                if (Prefs.countMode.value == CountUp) {
                    state.timer.secondsCountUp.value
                } else {
                    state.timer.secondsCountDown.value
                }
        }
        tv_overtime.text = when (state) {
            is InitState -> "Error"
            is WaitingToStart -> "Error"
            is TimerStarted -> {
                if (Prefs.countMode.value == CountUp) {
                    getString(
                            R.string.overtime_with_time,
                            state.timer.minutesCountUp.value - state.timerOption.minutes,
                            state.timer.secondsCountUp.value - state.timerOption.seconds
                    )
                } else {
                    getString(
                            R.string.overtime
                    )
                }
            }
        }
    }
    
    override fun onBackPressed() {
        class ExitDialogFragment : DialogFragment() {
            override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
                return AlertDialog.Builder(this@MainActivity)
                        .setTitle(R.string.exit_question)
                        .setPositiveButton(android.R.string.yes) { _, _ -> this@MainActivity.finish() }
                        .setNegativeButton(android.R.string.no) { _, _ -> dialog.cancel() }
                        .create()
            }
        }
        
        if (isTaskRoot) {
            ExitDialogFragment().show(supportFragmentManager, null)
        } else {
            super.onBackPressed()
        }
    }
    
    private val stateObserver = Observer<State> { state ->
        TransitionManager.beginDelayedTransition(root_activity_main)
        when (state) {
            is InitState -> {
                binding.timerVisible = false
                bt_startPause.setGone()
                bt_resetTime.setGone()
                
                tv_startingText.setVisible()
                
                timerOptionButtonsSelectable = true
            }
            
            else -> {
                binding.timerVisible = true
                bt_startPause.setVisible()
                
                tv_startingText.setGone()
                timerOptionButtonsSelectable = true
                
                when (state) {
                    is WaitingToStart -> {
                        bt_startPause.text = getString(R.string.start)
                        binding.color = EnvVars.color_timerStart
                        bt_resetTime.setGone()
                    }
                    is TimerStarted -> {
                        bt_resetTime.setVisible()
                        
                        updateBellsText()
                        
                        state.timer.overtime.observe(this@MainActivity) {
                            binding.overtime = it
                        }
                        
                        state.timer.secondsCountUp.observe(this@MainActivity) {
                            updateTime()
                        }
                        
                        state.timer.textColor.observe(this@MainActivity) { textColor ->
                            binding.color = textColor
                        }
                        
                        state.running.observe(this@MainActivity) { running ->
                            TransitionManager.beginDelayedTransition(root_activity_main)
                            bt_startPause.text = if (running) {
                                getString(R.string.pause)
                            } else {
                                getString(R.string.resume)
                            }
                            timerOptionButtonsSelectable = !running
                            keepScreenOn = running
                        }
                    }
                }
            }
        }
    }
}