package ch.ninecode.nine11;

import ch.ninecode.nine11.R;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;


public class MainActivity extends Activity implements PositionChangeListener
{
	static String TAG_SENT = "SMS_SENT";
	static String TAG_DELIVERED = "SMS_DELIVERED";

	Position _Position;
	BroadcastReceiver _Sent;
	BroadcastReceiver _Delivered;

	@Override
	protected void onCreate (Bundle savedInstanceState)
	{
		super.onCreate (savedInstanceState);
		setContentView (R.layout.activity_main);
		_Position = new Position (this);
		_Sent = new SentReceiver ();
		_Delivered = new DeliveredReceiver ();
	}

	@Override
	public boolean onCreateOptionsMenu (Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater ().inflate (R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected (MenuItem item)
	{
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId ();
		if (id == R.id.action_settings)
		{
			return true;
		}
		return super.onOptionsItemSelected (item);
	}

	@Override
	protected void onStart ()
	{
		Location location;

		super.onStart ();
		_Position.addPositionChangeListener (this);
		location = _Position.getLocation ();
		if (null != location)
			setText (location);
	}

	@Override
	protected void onStop ()
	{
		super.onStop ();
		_Position.removePositionChangeListener (this);
	}

	@Override
	protected void onResume ()
	{
		super.onResume ();
		registerReceiver (_Sent, new IntentFilter (TAG_SENT));
		registerReceiver (_Delivered, new IntentFilter (TAG_DELIVERED));
	}

	@Override
	protected void onPause ()
	{
		super.onPause ();
		unregisterReceiver (_Sent);
		unregisterReceiver (_Delivered);
	}

	@Override
	public void onPositionChange (Position position)
	{
		setText (position.getLocation ());
	}
	
	public void setText (Location location)
	{
		EditText text = (EditText)(findViewById (R.id.locationText));
		text.setText (location.toString ());
		new GeoCode (this).execute (location.getLongitude (), location.getLatitude ());
	}
	
	public void CallForHelp (View view)
	{
		String message = "LL = " + _Position.getLocation ().getLongitude () + "," + _Position.getLocation ().getLatitude ();
		if (0.0 != _Position.getLocation ().getAccuracy ())
			message += "\n±" + _Position.getLocation ().getAccuracy () + "m";
		EditText text = (EditText)(findViewById (R.id.addressText));
		String address = text.getText ().toString ();
		if ("" != address)
			message += "\n" + address;

		sendMessage (message);
	}
	
	public void sendMessage (String message)
	{
		EditText text = (EditText)(findViewById (R.id.messageText));
		text.setText (message);
		
		String phoneNo = "+41762719067";
		String myphoneNo = "+41767003530";

		if (true)
		{
	        Intent sendIntent = new Intent (Intent.ACTION_VIEW, Uri.parse ("sms:" + phoneNo));
	        sendIntent.putExtra ("sms_body", message); 
	        startActivity (sendIntent);
		}
		else
		{
			Intent intent = new Intent (TAG_SENT);
			intent.setComponent (new ComponentName ("ch.ninecode.nine11", "SentReceiver"));
			PendingIntent pending_sent = PendingIntent.getBroadcast (getApplicationContext (), 0, intent, 0);
			intent = new Intent (TAG_DELIVERED);
			intent.setComponent (new ComponentName ("ch.ninecode.nine11", "DeliveredReceiver"));
			PendingIntent pending_delivered= PendingIntent.getBroadcast (getApplicationContext (), 0, intent, 0);
	
			try
			{
				SmsManager smsManager = SmsManager.getDefault ();
				smsManager.sendTextMessage (phoneNo, myphoneNo, message, pending_sent, pending_delivered);
			}
			catch (Exception e)
			{
				Toast.makeText (getApplicationContext (), "SMS failed, please try again later!", Toast.LENGTH_LONG).show ();
				e.printStackTrace ();
			}

		}
	}
}
