package org.debatetimer

import android.animation.LayoutTransition
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.snackbar.Snackbar
import org.debatetimer.databinding.MainFragmentBinding
import org.debatetimer.databinding.TimerButtonBinding


class MainFragment : Fragment() {
    private val model: MainModel by viewModels()
    private lateinit var binding: MainFragmentBinding
    private lateinit var res: AppResources
    
    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

//        val callback: OnBackPressedCallback = object : OnBackPressedCallback(true /* enabled by default */) {
//            override fun handleOnBackPressed() {  class ExitDialogFragment : DialogFragment() {
//                override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
//                    return AlertDialog.Builder(requireContext())
//                            .setTitle(R.string.exit_question)
//                            .setPositiveButton(android.R.string.yes) { _, _ -> this@MainActivity.finish() }
//                            .setNegativeButton(android.R.string.no) { _, _ -> dialog!!.cancel() }
//                            .create()
//                }
//            }
//
//                if (isTaskRoot) {
//                    ExitDialogFragment().show(supportFragmentManager, null)
//                } else {
//                    super.onBackPressed()
//                }
//            }
//        }
//        requireActivity().onBackPressedDispatcher.addCallback(this, callback)
        
        binding = MainFragmentBinding.inflate(layoutInflater)
        res = AppResources.getInstance(requireContext())
        
        binding.rootActivityMain.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
        
        binding.lifecycleOwner = this
        binding.viewModel = model
        
        model.keepScreenOn.observe(viewLifecycleOwner, {
            if (it) {
                activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
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
        
        return binding.root
    }
    
    
    fun onSkipBackward(@Suppress("UNUSED_PARAMETER") view: View) {
        val skipped = model.onSkipBackward()
        if (!skipped) {
            Snackbar.make(binding.rootActivityMain, res.string.backwardSkipError, Snackbar.LENGTH_SHORT).show()
        }
    }
    
}