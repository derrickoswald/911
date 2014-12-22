package ch.ninecode.nine11;

import java.util.Timer;
import java.util.TimerTask;

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
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

public class PanicActivity extends Activity implements ServiceConnection, LocationChangeListener
{
    static String TAG_SENT = "SMS_SENT";
    static String TAG_DELIVERED = "SMS_DELIVERED";

    BroadcastReceiver _Sent;
    BroadcastReceiver _Delivered;

    PositionService _PositionService;
    Timer _Timer;
    int _Color = 0xffff0000;

    @Override
    protected void onCreate (Bundle savedInstanceState)
    {
        super.onCreate (savedInstanceState);
        setContentView (R.layout.activity_panic);
        _Sent = new SentReceiver ();
        _Delivered = new DeliveredReceiver ();
    }

    @Override
    protected void onStart ()
    {
        super.onStart ();

        // bind to PositionService
        Intent intent = new Intent (this, PositionService.class);
        bindService (intent, this, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop ()
    {
        super.onStop ();

        // unbind from PositionService
        unbindService (this);
    }

    @Override
    protected void onResume ()
    {
        super.onResume ();

        registerReceiver (_Sent, new IntentFilter (TAG_SENT));
        registerReceiver (_Delivered, new IntentFilter (TAG_DELIVERED));

        int id = 001;
        // get an instance of the NotificationManager service
        NotificationManager manager = (NotificationManager) (getSystemService (NOTIFICATION_SERVICE));
        manager.cancel (id);
    }

    @Override
    protected void onPause ()
    {
        super.onPause ();
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
        _PositionService.addPositionChangeListener (this);

        _Timer = new Timer ();
        MyTimerTask task = new MyTimerTask (6);
        _Timer.schedule (task, 25, 1000);
    }

    @Override
    public void onServiceDisconnected (ComponentName name)
    {
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

    class MyTimerTask extends TimerTask
    {
        protected int _Count;
        public MyTimerTask (int count)
        {
            _Count = count;
        }
        public void run ()
        {
            _Count--;
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    TextView text = (TextView) (findViewById (R.id.countdown));
                    text.setVisibility (android.view.View.VISIBLE);
                    text.setText (Integer.toString (_Count));
                }
            });
            if (0 == _Count)
            {
                _Timer.cancel ();
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        TextView text = (TextView) (findViewById (R.id.countdown));
                        text.setVisibility (android.view.View.INVISIBLE);
                        CallForHelp ();
                    }
                });
            }
        }
    };

    public void Transition (View view)
    {
        _Timer.cancel ();
        TextView text = (TextView) (findViewById (R.id.countdown));
        text.setVisibility (android.view.View.INVISIBLE);
        finish ();
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

        ImageButton button = (ImageButton) (findViewById (R.id.panic_button));
        button.setImageResource (R.drawable.dontpanic);
        _Color = 0xff000000;
    }
}
