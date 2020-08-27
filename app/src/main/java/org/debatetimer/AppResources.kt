package org.debatetimer

import android.content.Context
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat

class ColorResources(context: Context) {
    @ColorInt
    val timerStart: Int = ContextCompat.getColor(context, R.color.timerStart)
    
    @ColorInt
    val timerNormal: Int = ContextCompat.getColor(context, R.color.timerNormal)
    
    @ColorInt
    val timerEnd: Int = ContextCompat.getColor(context, R.color.timerEnd)
    
    @ColorInt
    val timerOvertime: Int = ContextCompat.getColor(context, R.color.timerOvertime)
}

class StringResources(context: Context) {
    val on: String = context.getString(R.string.on)
    val off: String = context.getString(R.string.off)
    
    val start: String = context.getString(R.string.start)
    val pause: String = context.getString(R.string.pause)
    val resume: String = context.getString(R.string.resume)
    
    val overtimeBy: String = context.getString(R.string.overtime)
    val backwardSkipError: String = context.getString(R.string.backward_skip_error)
    
    val prefSelectedTimerConfigDefault: String = context.getString(R.string.pref_selected_timer_config_default)
}

class AppResources private constructor(context: Context) {
    val color: ColorResources = ColorResources(context)
    val string: StringResources = StringResources(context)
    
    companion object {
        private var instance: AppResources? = null
        
        @Synchronized
        fun getInstance(context: Context): AppResources {
            if (instance == null) {
                instance = AppResources(context)
            }
            
            return instance!!
        }
    }
}

