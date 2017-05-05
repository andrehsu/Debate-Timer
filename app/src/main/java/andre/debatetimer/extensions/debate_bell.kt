package andre.debatetimer.extensions

import andre.debatetimer.R
import android.media.MediaPlayer

/**
 * Created by Andre on 5/6/2017.
 */
var debateBellEnabled = true

enum class DebateBell(val intValue: Int, private val bell: MediaPlayer) {
	ONCE(1, MediaPlayer.create(EnvVars.applicationContext, R.raw.debate_bell_one)), TWICE(2, MediaPlayer.create(EnvVars.applicationContext, R.raw.debate_bell_two));
	
	fun ring() {
		if (debateBellEnabled)
			bell.start()
	}
}