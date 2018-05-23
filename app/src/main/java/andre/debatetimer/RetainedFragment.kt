package andre.debatetimer

import android.os.Bundle
import androidx.fragment.app.Fragment


class RetainedFragment : Fragment() {
	lateinit var presenter: MainModel
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		retainInstance = true
	}
}