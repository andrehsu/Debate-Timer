package andre.debatetimer.extensions

import andre.debatetimer.BuildConfig
import android.util.Log

/**
 * Created by Andre on 5/6/2017.
 */
interface DebugLogger {
	private val LOG_TAG: String
		get() = this::class.java.simpleName
	
	fun debug(msg: () -> String) {
		if (BuildConfig.DEBUG) {
			Log.d(LOG_TAG, msg())
		}
	}
}