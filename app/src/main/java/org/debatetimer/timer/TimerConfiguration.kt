package org.debatetimer.timer

import org.debatetimer.timer.DebateBell.Once
import kotlin.math.absoluteValue

class TimerConfiguration(val tag: String, val totalSeconds: Int, bellsCountingUp: Map<Int, DebateBell>) {
    companion object {
        val default = TimerConfiguration("default", 180, mapOf())
        
        fun parseTag(tag: String): TimerConfiguration {
            val seconds = tag.toInt()
            
            val bells = mapOf(60 to Once, seconds - 60 to Once)
            
            return TimerConfiguration(tag, seconds, bells)
        }
    }
    
    val countUpBellsText: String
    val countDownBellsText: String
    val bellsSinceStart: Map<Int, DebateBell>
    
    init {
        this.bellsSinceStart = bellsCountingUp.toSortedMap()
        val sorted = this.bellsSinceStart.filter { (_, v) -> v == Once }.map { it.key }
        
        countUpBellsText = sorted.joinToString { secondsToString(it) }
        countDownBellsText = sorted.joinToString { secondsToString(totalSeconds - it) }
    }
    
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
        return "$seconds"
    }
}

private fun secondsToString(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds.absoluteValue % 60
    return "%d:%02d".format(minutes, seconds)
}
