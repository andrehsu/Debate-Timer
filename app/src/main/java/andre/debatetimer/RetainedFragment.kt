package andre.debatetimer

import android.app.Fragment
import android.os.Bundle


class RetainedFragment : Fragment() {
	lateinit var presenter: IMainPresenter
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		retainInstance = true
	}
}