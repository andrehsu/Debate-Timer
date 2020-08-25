package andre.debatetimer

import andre.debatetimer.databinding.ActivityMainBinding
import andre.debatetimer.databinding.TimerButtonBinding
import andre.debatetimer.timer.TimerOption
import android.animation.LayoutTransition
import android.app.Dialog
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.forEach
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.preference.PreferenceManager
import androidx.transition.TransitionManager


class MainActivity : AppCompatActivity() {
    private val model: MainModel by viewModels()
    private lateinit var binding: ActivityMainBinding
    
    /**
     * Whether timer options should be selectable
     */
    private var timerOptionsSelectable: Boolean = true
        set(value) {
            field = value
            binding.llTimeButtons.forEach {
                DataBindingUtil.getBinding<TimerButtonBinding>(it)!!.selectable = value
            }
        }
    
    /**
     * Whether screen should be kept on
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
    
    private var timerOptionTag: String = ""
        set(value) {
            field = value
            binding.llTimeButtons.forEach {
                DataBindingUtil.getBinding<TimerButtonBinding>(it)!!.selectedBindingString = value
            }
        }
    
    /**
     * onCreate method
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
    
        binding.rootActivityMain.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
    
        binding.lifecycleOwner = this
        binding.viewModel = model
    
    
        model.timerOptionsSelectable.observe(this, {
            timerOptionsSelectable = it
        })
    
    
        model.timerOption.observe(this, Observer {
            timerOptionTag = it?.tag ?: "None"
        })
    
        model.keepScreenOn.observe(this, Observer {
            keepScreenOn = it
        })
    
        model.timerControlButtonText.observe(this, Observer {
            TransitionManager.beginDelayedTransition(binding.rootActivityMain)
        })
    
        model.enableBells.observe(this, Observer {
            TransitionManager.beginDelayedTransition(binding.rootActivityMain)
        })
    
        model.countMode.observe(this, Observer {
            TransitionManager.beginDelayedTransition(binding.rootActivityMain)
        })
    
        model.timerOption.observe(this, Observer {
            TransitionManager.beginDelayedTransition(binding.rootActivityMain)
        })
        
        //region Setup timer options
        val sp = PreferenceManager.getDefaultSharedPreferences(this)
        
        val timersStr = sp.getString(
                getString(R.string.pref_timers_key),
                getString(R.string.pref_timers_default)
        )!!
        
        val timerMaps = mutableMapOf<String, TimerOption>()
        
        timersStr.split('|').forEach { str ->
            val timerOption = TimerOption.parseTag(str)
            
            if (timerOption != null) {
                val timerButtonBinding: TimerButtonBinding = DataBindingUtil.inflate(layoutInflater, R.layout.timer_button, binding.llTimeButtons, true)
                
                timerButtonBinding.viewModel = model
                
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
                
                timerMaps[timerButtonBinding.text!!] = timerOption
            }
        }
        model.timerMaps = timerMaps
        //endregion
    }
    
    override fun onBackPressed() {
        class ExitDialogFragment : DialogFragment() {
            override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
                return AlertDialog.Builder(this@MainActivity)
                        .setTitle(R.string.exit_question)
                        .setPositiveButton(android.R.string.yes) { _, _ -> this@MainActivity.finish() }
                        .setNegativeButton(android.R.string.no) { _, _ -> dialog!!.cancel() }
                        .create()
            }
        }
        
        if (isTaskRoot) {
            ExitDialogFragment().show(supportFragmentManager, null)
        } else {
            super.onBackPressed()
        }
    }
}