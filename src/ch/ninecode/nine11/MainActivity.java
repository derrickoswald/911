package ch.ninecode.nine11;

import ch.ninecode.nine11.R;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.telephony.SmsManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.RemoteViews;
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
		PreferenceManager.setDefaultValues (this, R.xml.preferences, false);
		setContentView (R.layout.activity_main);
		_Position = new Position (this);
		_Sent = new SentReceiver ();
		_Delivered = new DeliveredReceiver ();
		begin ();
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
		Intent intent;
		boolean ret;
		
		int id = item.getItemId ();
		if (id == R.id.action_settings)
		{
            intent = new Intent (this, SettingsActivity.class);
            startActivity (intent);
			ret = true;
		}
		else
			ret = super.onOptionsItemSelected (item);
		
		return (ret);
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
		if (null != _Position.getLocation ())
		{
			String message = "" + _Position.getLocation ().getLongitude () + "°N," + _Position.getLocation ().getLatitude () + "°E";
			if (0.0 != _Position.getLocation ().getAccuracy ())
				message += " ±" + _Position.getLocation ().getAccuracy () + "m";
			EditText text = (EditText)(findViewById (R.id.addressText));
			String address = text.getText ().toString ();
			sendMessage (address, message);
		}
		else
			Toast.makeText (getApplicationContext (), "No position available, please try again later!", Toast.LENGTH_LONG).show ();
	}
	
	public void sendMessage (String short_message, String long_message)
	{
		String full_message = short_message + " " + long_message;
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		boolean interactive = sharedPref.getBoolean (getString (R.string.sms_interactive_key), false);
		String[] phone_numbers =
		{
			sharedPref.getString (getString (R.string.sms_key_1), ""),
			sharedPref.getString (getString (R.string.sms_key_2), ""),
			sharedPref.getString (getString (R.string.sms_key_3), "")
		};
		String smsc = sharedPref.getString (getString (R.string.smsc_key), "");
		if ("".equals (smsc))
			smsc = null;
		
		EditText text = (EditText)(findViewById (R.id.messageText));
		text.setText (full_message);
		
		if (interactive)
		{
	        Intent sendIntent = new Intent (Intent.ACTION_VIEW, Uri.parse ("sms:" + phone_numbers[0]));
	        sendIntent.putExtra ("sms_body", short_message); 
	        startActivity (sendIntent);
		}
		else
		{
			Intent intent = new Intent (TAG_SENT);
			intent.setComponent (new ComponentName ("ch.ninecode.nine11", "SentReceiver"));
			PendingIntent pending_sent = PendingIntent.getBroadcast (this, 0, intent, 0);
			intent = new Intent (TAG_DELIVERED);
			intent.setComponent (new ComponentName ("ch.ninecode.nine11", "DeliveredReceiver"));
			PendingIntent pending_delivered = PendingIntent.getBroadcast (this, 0, intent, 0);
	
			for (int i = 0; i < phone_numbers.length; i++)
				if (!"".equals (phone_numbers[i]))
					try
					{
						SmsManager smsManager = SmsManager.getDefault ();
						smsManager.sendTextMessage (phone_numbers[i], smsc, short_message, pending_sent, pending_delivered);
					}
					catch (Exception e)
					{
						Toast.makeText (getApplicationContext (), "SMS to " + phone_numbers[i] + " failed. " + e.getMessage (), Toast.LENGTH_LONG).show ();
						e.printStackTrace ();
					}
		}

		String [] email_addresses =
		{
			sharedPref.getString (getString (R.string.email_key_1), ""),
			sharedPref.getString (getString (R.string.email_key_2), ""),
			sharedPref.getString (getString (R.string.email_key_3), "")
		};
		String user = sharedPref.getString (getString (R.string.gmail_account_key), "username@gmail.com");
		String password = sharedPref.getString (getString (R.string.gmail_password_key), "secret");
		String subject = sharedPref.getString (getString (R.string.email_subject_key), "PANIC BUTTON!");
		String to = "";
		for (int i = 0; i < email_addresses.length; i++)
			if (!"".equals (email_addresses[i]))
			{
				if (!"".equals (to))
					to += ",";
				to += email_addresses[i];
			}
		if (!"".equals (to))
			(new SendMail (this)).execute (new SendMail.Details (subject, full_message, user, password, to));
	}

	protected void begin ()
	{
		RemoteViews views = new RemoteViews ("ch.ninecode.nine11", R.layout.panic_button);
		Intent intent = new Intent (this, PanicActivity.class);
		PendingIntent pending =
		    PendingIntent.getActivity(
		    this,
		    0,
		    intent,
		    PendingIntent.FLAG_UPDATE_CURRENT
		);
		views.setOnClickPendingIntent (R.id.notification, pending);

		NotificationCompat.Builder builder = new NotificationCompat.Builder (this);
		builder.setContent (views);
		builder.setSmallIcon (R.drawable.logo);

	    Notification notification = builder.build ();
	    notification.visibility = Notification.VISIBILITY_PUBLIC;
	    ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify (1, notification);
	}
}
