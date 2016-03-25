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
    boolean _Started;
    PositionService _PositionService;

    @Override
    protected void onCreate (Bundle savedInstanceState)
    {
        super.onCreate (savedInstanceState);

        _Started = false;
        _PositionService = null;
        PreferenceManager.setDefaultValues (this, R.xml.preferences, false);
        setContentView (R.layout.activity_main);

        // bind to PositionService
        Intent intent = new Intent (this, PositionService.class);
        bindService (intent, this, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy ()
    {
        super.onDestroy ();

        // unbind from PositionService
        unbindService (this);
    }

    @Override
    protected void onStart ()
    {
        super.onStart ();

        if (null != _PositionService)
            _PositionService.addPositionChangeListener (this);
        _Started = true;
    }

    @Override
    protected void onStop ()
    {
        super.onStop ();

        _Started = false;
        if (null != _PositionService)
            _PositionService.removePositionChangeListener (this);
    }

    @Override
    public boolean onCreateOptionsMenu (Menu menu)
    {
        // inflate the menu; this adds items to the action bar if it is present
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
        EditText text;

        text = (EditText) (findViewById (R.id.locationText));
        text.setText (location);
        text = (EditText) (findViewById (R.id.addressText));
        text.setText (address);
    }

    //
    // implementation
    //
    public void CallForHelp (View view)
    {
        Intent intent;

        intent = new Intent (this, PanicActivity.class);
        startActivity (intent);
    }
}
