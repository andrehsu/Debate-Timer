package org.debatetimer.timer

import kotlin.math.absoluteValue

class TimerConfiguration(val totalSeconds: Int) {
    companion object {
        fun parseTag(tag: String): TimerConfiguration {
            val seconds = tag.toInt()
            
            return TimerConfiguration(seconds)
        }
    }
    
    val bellsCountingUp = sortedSetOf(60, totalSeconds - 60)
    val countUpBellsText: String = bellsCountingUp.joinToString { secondsToString(it) }
    val countDownBellsText: String = bellsCountingUp.joinToString { secondsToString(totalSeconds - it) }
    val tag: String = toString()
    
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    
    val text: String
    
    init {
        val buttonTextSb = StringBuilder()
        if (minutes != 0) {
            buttonTextSb.append("${minutes}m")
        }
        if (seconds != 0) {
            buttonTextSb.append("${seconds}s")
        }
        
        text = buttonTextSb.toString()
    }
    
    override fun toString(): String {
        return "$totalSeconds"
    }
}

private fun secondsToString(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds.absoluteValue % 60
    return "%d:%02d".format(minutes, seconds)
}
