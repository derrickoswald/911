package ch.ninecode.nine11;

import java.util.Timer;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

public class PanicActivity extends Activity implements ServiceConnection, LocationChangeListener
{
    static String _ClassName = "ch.ninecode.nine11.PanicActivity";

    static String TAG_SENT = "SMS_SENT";
    static String TAG_DELIVERED = "SMS_DELIVERED";

    BroadcastReceiver _Sent;
    BroadcastReceiver _Delivered;

    protected boolean _Started;
    protected PositionService _PositionService;
    protected Timer _Timer;
    protected CountdownTask _Task;
    protected int _Color = 0xffff0000;

    @Override
    protected void onCreate (Bundle savedInstanceState)
    {
        super.onCreate (savedInstanceState);
        Log.i (_ClassName, "onCreate");

        _Started = false;
        PreferenceManager.setDefaultValues (this, R.xml.preferences, false);
        setContentView (R.layout.activity_panic);
        _Sent = new SentReceiver ();
        _Delivered = new DeliveredReceiver ();

        // bind to PositionService
        Intent intent = new Intent (this, PositionService.class);
        bindService (intent, this, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy ()
    {
        super.onDestroy ();
        Log.i (_ClassName, "onDestroy");

        // unbind from PositionService
        unbindService (this);
    }

    @Override
    protected void onStart ()
    {
        super.onStart ();
        Log.i (_ClassName, "onStart");

        if (null != _PositionService)
            _PositionService.addPositionChangeListener (this);
        _Started = true;

        _Timer = new Timer ();
        _Task = new CountdownTask (this, getDelay (), new CountdownTask.Callback () { @Override
        public void execute () { CallForHelp (); } });
    }

    @Override
    protected void onRestoreInstanceState (Bundle savedInstanceState)
    {
        super.onRestoreInstanceState (savedInstanceState);

        _Task.setCount (savedInstanceState.getInt ("countdown"));
    }

    @Override
    protected void onSaveInstanceState (Bundle outState)
    {
        super.onSaveInstanceState (outState);

        outState.putInt ("countdown", _Task.getCount ());
    }

    @Override
    protected void onStop ()
    {
        super.onStop ();
        Log.i (_ClassName, "onStop");

        _Started = false;
        if (null != _PositionService)
            _PositionService.removePositionChangeListener (this);

        _Timer.cancel ();
        _Timer.purge ();
        _Timer = null;
        _Task = null;
    }

    @Override
    protected void onResume ()
    {
        super.onResume ();
        Log.i (_ClassName, "onResume");

        registerReceiver (_Sent, new IntentFilter (TAG_SENT));
        registerReceiver (_Delivered, new IntentFilter (TAG_DELIVERED));

        int id = 001;
        // get an instance of the NotificationManager service
        NotificationManager manager = (NotificationManager) (getSystemService (NOTIFICATION_SERVICE));
        manager.cancel (id);

        _Timer.schedule (_Task, 25, 1000);
    }

    @Override
    protected void onPause ()
    {
        super.onPause ();
        Log.i (_ClassName, "onPause");

        _Task.cancel ();

        unregisterReceiver (_Sent);
        unregisterReceiver (_Delivered);
    }

    //
    // ServiceConnection interface
    //

    @Override
    public void onServiceConnected (ComponentName name, IBinder service)
    {
        _PositionService = ((PositionService.PositionBinder)service).getService ();
        if (_Started)
            _PositionService.addPositionChangeListener (this);
    }

    @Override
    public void onServiceDisconnected (ComponentName name)
    {
        _PositionService.removePositionChangeListener (this);
        _PositionService = null;
    }

    //
    // LocationChangeListener interface
    //

    /**
     * Do the needful when a position change happens.
     */
    @Override
    public void onLocationChange (String location, String address)
    {
        TextView text;

        text = (TextView) (findViewById (R.id.messageText));
        text.setText (location + "\n" + address);
        text.setTextColor (_Color);
    }

    //
    // implementation
    //

    public void Transition (View view)
    {
        _Timer.cancel ();
        TextView text = (TextView) (findViewById (R.id.countdown));
        text.setVisibility (android.view.View.INVISIBLE);
        finish ();
    }

    // <rant>I can't believe you have to jump through these hoops because SharedPreferences doesn't have a getInt that works on strings.</rant>
    public int getDelay ()
    {
        SharedPreferences preferences;
        String value;
        int ret;

        preferences = PreferenceManager.getDefaultSharedPreferences (this);
        value = preferences.getString (getString (R.string.panic_delay_key), "10");
        try
        {
            ret = Integer.parseInt (value);
        }
        catch (NumberFormatException ex)
        {
            ret = 10;
        }

        return (ret);
    }

    public void CallForHelp ()
    {
        if (null != _PositionService)
        {
            String latlong = _PositionService.getLatLong ();
            String address = _PositionService.getAddress ();

            if ("" != latlong)
                sendMessage (address, latlong);
            else
                Toast.makeText (getApplicationContext (), "No position available, please try again later!", Toast.LENGTH_LONG).show ();
        }
        else
            Toast.makeText (getApplicationContext (), "No position service, please try again later!", Toast.LENGTH_LONG).show ();
    }

    public void sendMessage (String address, String location)
    {
        String full_message = address + " " + location;
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences (this);
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

        TextView text = (TextView) (findViewById (R.id.messageText));
        text.setText (full_message);
        text.setTextColor (_Color);

        if (interactive)
        {
            Intent sendIntent = new Intent (Intent.ACTION_VIEW, Uri.parse ("sms:" + phone_numbers[0]));
            sendIntent.putExtra ("sms_body", address);
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
                        smsManager.sendTextMessage (phone_numbers[i], smsc, address, pending_sent, pending_delivered);
                    }
                    catch (Exception e)
                    {
                        Toast.makeText (getApplicationContext (), "SMS to " + phone_numbers[i] + " failed. " + e.getMessage (), Toast.LENGTH_LONG).show ();
                        e.printStackTrace ();
                    }
        }

        String[] email_addresses =
        {
            sharedPref.getString (getString (R.string.email_key_1), ""),
            sharedPref.getString (getString (R.string.email_key_2), ""),
            sharedPref.getString (getString (R.string.email_key_3), "")
        };
        String host = sharedPref.getString (getString (R.string.mailhost_key), "smtp.gmail.com");
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
            (new SendMail (this)).execute (new SendMail.Details (subject, full_message, host, user, password, to));

        ImageButton button = (ImageButton) (findViewById (R.id.panic_button));
        button.setImageResource (R.drawable.dontpanic);
        _Color = 0xff000000;
    }
}
