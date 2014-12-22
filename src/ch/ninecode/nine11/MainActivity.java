package ch.ninecode.nine11;

import ch.ninecode.nine11.R;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

public class MainActivity extends Activity implements ServiceConnection, LocationChangeListener
{
    PositionService _PositionService;

    @Override
    protected void onCreate (Bundle savedInstanceState)
    {
        super.onCreate (savedInstanceState);
        PreferenceManager.setDefaultValues (this, R.xml.preferences, false);
        setContentView (R.layout.activity_main);
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
    public boolean onCreateOptionsMenu (Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater ().inflate (R.menu.main, menu);
        return (true);
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

    public void CallForHelp (View view)
    {
        Intent intent;

        intent = new Intent (this, PanicActivity.class);
        startActivity (intent);
    }

    //
    // ServiceConnection interface
    //

    @Override
    public void onServiceConnected (ComponentName name, IBinder service)
    {
        PositionService.PositionBinder binder = (PositionService.PositionBinder)service;
        _PositionService = binder.getService ();
        _PositionService.addPositionChangeListener (MainActivity.this);
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
        EditText text;

        text = (EditText) (findViewById (R.id.locationText));
        text.setText (location);
        text = (EditText) (findViewById (R.id.addressText));
        text.setText (address);
    }
}
