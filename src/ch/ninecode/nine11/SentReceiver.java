package ch.ninecode.nine11;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsManager;
import android.widget.Toast;

public class SentReceiver extends BroadcastReceiver
{

	@Override
	public void onReceive (Context context, Intent intent)
	{
		switch (getResultCode())
		{
			case Activity.RESULT_OK:
				Toast.makeText (context, "SMS Sent!", Toast.LENGTH_LONG).show ();
				break;
			case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
				Toast.makeText (context, "SMS had a generic failure", Toast.LENGTH_LONG).show ();
				break;
			case SmsManager.RESULT_ERROR_RADIO_OFF:
				Toast.makeText (context, "SMS had a radio off failure", Toast.LENGTH_LONG).show ();
				break;
			case SmsManager.RESULT_ERROR_NULL_PDU:
				Toast.makeText (context, "SMS had a null PDU failure", Toast.LENGTH_LONG).show ();
				break;
		}
	}
}
