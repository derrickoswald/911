package ch.ninecode.nine11;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class DeliveredReceiver extends BroadcastReceiver
{

	@Override
	public void onReceive (Context context, Intent intent)
	{
		// extended data ("pdu")
		Toast.makeText (context, "SMS delivered", Toast.LENGTH_LONG).show ();
	}
}
