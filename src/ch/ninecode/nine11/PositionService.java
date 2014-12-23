package ch.ninecode.nine11;

import java.util.ArrayList;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;

public class PositionService extends Service implements PositionChangeListener, GeoCode.AddressResultListener
{
    private final IBinder _Binder = new PositionBinder ();
    private ArrayList<LocationChangeListener> _Listeners;
    private String _LatLong;
    private String _Address;
    private Position _Position;

    /**
     * Class used for the client Binder.
     */
    public class PositionBinder extends Binder
    {
        public PositionService getService ()
        {
            return (PositionService.this);
        }
    }

    @Override
    public void onCreate ()
    {
        Location location;

        super.onCreate ();
        PreferenceManager.setDefaultValues (this, R.xml.preferences, false);
        _Listeners = new ArrayList<LocationChangeListener> ();
        _LatLong = "";
        _Address = "";
        _Position = new Position (this);
        _Position.addPositionChangeListener (this);
        location = _Position.getLocation ();
        if (null != location)
            setLocation (location);
    }

    @Override
    public void onDestroy ()
    {
        super.onDestroy ();
        _Position.removePositionChangeListener (this);
    }

    @Override
    public IBinder onBind (Intent intent)
    {
        return (_Binder);
    }

    //
    // PositionChangeListener interface
    //

    @Override
    public void onPositionChange (Position position)
    {
        setLocation (position.getLocation ());
    }

    //
    // GeoCode.AddressResultListener interface
    //

    @Override
    public void onAddressResult (String address)
    {
        _Address = address;
        postUpdate ();
    }

    //
    // implementation
    //

    public void addPositionChangeListener (LocationChangeListener listener)
    {
        synchronized (_Listeners)
        {
            _Listeners.add (listener);
        }
    }

    public void removePositionChangeListener (LocationChangeListener listener)
    {
        synchronized (_Listeners)
        {
            _Listeners.remove (listener);
        }
    }

    protected void setLocation (Location location)
    {
        double lon;
        double lat;
        
        lon = location.getLongitude ();
        lat = location.getLatitude ();
        _LatLong = "" + lon + "," + lat + " (" + location.getProvider () + ")";
        _Address = "https://maps.google.com/maps?q=" + lat + "," + lon;

        new GeoCode (this).execute (lon, lat);
        postUpdate ();
    }
    
    @TargetApi (Build.VERSION_CODES.LOLLIPOP)
    public void postUpdate ()
    {
        String address;
        String location;

        location = getLatLong ();
        address = getAddress ();

        RemoteViews views = new RemoteViews ("ch.ninecode.nine11", R.layout.panic_button);
        if (null != address)
            views.setTextViewText (R.id.address, address);
        if (null != location)
            views.setTextViewText (R.id.location, location);
        Intent intent = new Intent (this, PanicActivity.class);
        PendingIntent pending = PendingIntent.getActivity (this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent (R.id.notification, pending);
        views.setOnClickPendingIntent (R.id.panic_button, pending);
        views.setOnClickPendingIntent (R.id.address, pending);
        views.setOnClickPendingIntent (R.id.location, pending);

        NotificationCompat.Builder builder = new NotificationCompat.Builder (this);
        builder.setContent (views);
        builder.setSmallIcon (R.drawable.logo);
        if (Build.VERSION.SDK_INT >= 21)
            builder.setVisibility (Notification.VISIBILITY_PUBLIC);

        Notification notification = builder.build ();
        ((NotificationManager) getSystemService (NOTIFICATION_SERVICE)).notify (1, notification);

        synchronized (_Listeners)
        {
            // TODO: should really _Listeners.clone ();
            for (LocationChangeListener listener : _Listeners)
                try
                {
                    listener.onLocationChange (location, address);
                }
                catch (Exception e)
                {
                    Log.e ("ch.ninecode.portalviewer.Origin", "OriginChangeListener threw an exception.", e);
                }
        }
    }

    public String getLatLong ()
    {
        return (_LatLong);
    }

    public String getAddress ()
    {
        return (_Address);
    }
}
