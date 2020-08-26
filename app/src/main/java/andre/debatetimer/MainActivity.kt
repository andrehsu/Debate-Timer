package andre.debatetimer

import andre.debatetimer.databinding.ActivityMainBinding
import andre.debatetimer.databinding.TimerButtonBinding
import android.animation.LayoutTransition
import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import com.google.android.material.snackbar.Snackbar


class MainActivity : AppCompatActivity() {
    private val model: MainModel by viewModels()
    private lateinit var binding: ActivityMainBinding
    private lateinit var res: AppResources
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        res = AppResources.getInstance(this)
        
        binding.rootActivityMain.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
        
        binding.lifecycleOwner = this
        binding.viewModel = model
        
        model.keepScreenOn.observe(this, {
            if (it) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        })
        
        for (timerOptions in model.timerConfigs.values) {
            val timerButtonBinding = TimerButtonBinding.inflate(layoutInflater, binding.llTimeButtons, true)
            
            timerButtonBinding.lifecycleOwner = this
            timerButtonBinding.viewModel = model
            timerButtonBinding.text = timerOptions.text
            timerButtonBinding.tag = timerOptions.tag
        }

//        model.timerControlButtonText.observe(this, {
//            TransitionManager.beginDelayedTransition(binding.rootActivityMain)
//        })
//
//        model.enableBells.observe(this, {
//            TransitionManager.beginDelayedTransition(binding.rootActivityMain)
//        })
//
//        model.countMode.observe(this, {
//            TransitionManager.beginDelayedTransition(binding.rootActivityMain)
//        })
//
//        model.timerOption.observe(this, {
//            TransitionManager.beginDelayedTransition(binding.rootActivityMain)
//        })
        
        //endregion
    }
    
    fun onSkipBackward(@Suppress("UNUSED_PARAMETER") view: View) {
        val skipped = model.onSkipBackward()
        if (!skipped) {
            Snackbar.make(binding.rootActivityMain, res.string.backwardSkipError, Snackbar.LENGTH_SHORT).show()
        }
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