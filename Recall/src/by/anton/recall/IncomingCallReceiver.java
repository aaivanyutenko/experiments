package by.anton.recall;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.widget.Toast;

public class IncomingCallReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		// TELEPHONY MANAGER class object to register one listner
		TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		// Register listener for LISTEN_CALL_STATE
		final Context pcontext = context;
		telephonyManager.listen(new PhoneStateListener() {
			public void onCallStateChanged(int state, String incomingNumber) {
				// state = 1 means when phone is ringing
				if (state == 1) {
					String msg = " New Phone Call Event. Incomming Number : " + incomingNumber;
					int duration = Toast.LENGTH_LONG;
					Toast toast = Toast.makeText(pcontext, msg, duration);
					toast.show();
				}
			}
		}, PhoneStateListener.LISTEN_CALL_STATE);
	}

}
