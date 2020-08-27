package org.debatetimer.ui.main

import android.animation.LayoutTransition
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.snackbar.Snackbar
import org.debatetimer.AppPreferences
import org.debatetimer.AppResources
import org.debatetimer.R
import org.debatetimer.databinding.MainFragmentBinding
import org.debatetimer.databinding.TimerButtonBinding


class MainFragment : Fragment() {
    private val model: MainModel by viewModels()
    
    private lateinit var res: AppResources
    private lateinit var prefs: AppPreferences
    private lateinit var onBackPressedCallback: OnBackPressedCallback
    
    private lateinit var binding: MainFragmentBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        res = AppResources.getInstance(requireContext())
        prefs = AppPreferences.getInstance(requireContext())
        
        onBackPressedCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                class ExitDialogFragment : DialogFragment() {
                    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
                        return AlertDialog.Builder(requireContext())
                                .setTitle(R.string.exit_question)
                                .setPositiveButton(android.R.string.yes) { _, _ -> requireActivity().finish() }
                                .setNegativeButton(android.R.string.no) { _, _ -> dialog!!.cancel() }
                                .create()
                    }
                }
                
                ExitDialogFragment().show(parentFragmentManager, null)
            }
        }
    
        requireActivity().onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }
    
    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = MainFragmentBinding.inflate(layoutInflater)
        
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.rootActivityMain.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = model
        
        model.keepScreenOn.observe(viewLifecycleOwner, {
            if (it) {
                onBackPressedCallback.isEnabled = true
                activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                onBackPressedCallback.isEnabled = false
                activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        })
        
        for (timerOptions in prefs.timerConfigs.value.values) {
            val timerButtonBinding = TimerButtonBinding.inflate(layoutInflater, binding.llTimeButtons, true)
            
            timerButtonBinding.lifecycleOwner = viewLifecycleOwner
            timerButtonBinding.viewModel = model
            timerButtonBinding.text = timerOptions.text
            timerButtonBinding.tag = timerOptions.tag
        }
        
        binding.btMinus10.setOnClickListener {
            val skipped = model.onSkipBackward()
            if (!skipped) {
                Snackbar.make(binding.rootActivityMain, res.string.backwardSkipError, Snackbar.LENGTH_SHORT).show()
            }
        }
    }
}