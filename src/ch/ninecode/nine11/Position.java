package ch.ninecode.nine11;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import ch.ninecode.nine11.PositionChangeListener;

public class Position
    implements
        LocationListener
{
	static String _ClassName = "ch.ninecode.nineoneone.Position";
	
    // TODO: make these configurable via settings
    protected long _MinumumNetworkUpdateTime =  1000; // minimum time interval between network location updates, in milliseconds
    protected float _MinimumNetworkUpdateDistance = 100; // minimum distance between network location updates, in meters
    protected long _MinumumGPSUpdateTime = 1000; // minimum time interval between GPS location updates, in milliseconds
    protected float _MinimumGPSUpdateDistance = 100; // minimum distance between GPS location updates, in meters
    protected long _MinumumPassiveUpdateTime = 1000; // minimum time interval between passive location updates, in milliseconds
    protected float _MinimumPassiveUpdateDistance = 100; // minimum distance between passive location updates, in meters
    protected int _AgeIntervalThreshold = 1000 * 60 * 2; // maximum time between significant readings, in milliseconds
    protected float _AccuracyThreshold = 200.0f; // maximum difference in estimated accuracy (difference in standard deviation if normally distributed), in meters

    protected Location _Current;

    protected Context _Context;

	private ArrayList<PositionChangeListener> _Listeners;

    public Position (Context context)
    {
        _Context = context;
        _Current = null;
        _Listeners = new ArrayList<PositionChangeListener> ();
    }
    
    public Location getLocation ()
    {
        return (_Current);
    }
    
    public void startListening ()
    {
        LocationManager manager;
        List<String> providers;
        
        Log.i (_ClassName, "listening");
        manager = (LocationManager)_Context.getSystemService (Context.LOCATION_SERVICE);
        providers = manager.getProviders (false);
        for (String provider:providers)
            try
            {
                if (manager.isProviderEnabled (provider))
                    switch (provider)
                    {
                        case LocationManager.NETWORK_PROVIDER:
                            manager.requestLocationUpdates (provider, _MinumumNetworkUpdateTime, _MinimumNetworkUpdateDistance, this);
                            break;
                        case LocationManager.GPS_PROVIDER:
                            manager.requestLocationUpdates (provider, _MinumumGPSUpdateTime, _MinimumGPSUpdateDistance, this);
                            break;
                        case LocationManager.PASSIVE_PROVIDER:
                            manager.requestLocationUpdates (provider, _MinumumPassiveUpdateTime, _MinimumPassiveUpdateDistance, this);
                            break;
                        default:
                            Log.i (_ClassName, "location provider '" + provider + "' is not supported"); 
                    }
                else
                    Log.i (_ClassName, "location provider '" + provider + "' is not enabled");
            }
            catch (SecurityException se)
            {
                Log.i (_ClassName, "permission is not present", se);
            }
            catch (RuntimeException rte)
            {
                Log.i (_ClassName, "calling thread has no Looper", rte);
            }
    }
    
    public void stopListening ()
    {
        LocationManager manager;
        
        Log.i (_ClassName, "not listening");
        manager = (LocationManager)_Context.getSystemService (Context.LOCATION_SERVICE);
        manager.removeUpdates (this);
    }

    public void addPositionChangeListener (PositionChangeListener listener)
    {
        boolean listening;
        
        synchronized (_Listeners)
        {
            listening = 0 != _Listeners.size ();
            if (!_Listeners.contains (listener))
            {
                _Listeners.add (listener);
                if (!listening)
                    startListening ();
            }
        }
    }

    public void removePositionChangeListener (PositionChangeListener listener)
    {
        synchronized (_Listeners)
        {
            if (_Listeners.remove (listener))
                if (0 == _Listeners.size ())
                    stopListening ();
        }
    }


    /**
     * Determines whether one Location reading is better than the current
     * Location fix
     * 
     * @param location
     *            The new Location that you want to evaluate
     * @param currentBestLocation
     *            The current Location fix, to which you want to compare the new
     *            one
     */
    protected boolean isBetterLocation (Location location, Location currentBestLocation)
    {
        long time_delta;
        boolean much_newer;
        boolean much_older;
        boolean newer;
        float accuracy_delta;
        boolean less_accurate;
        boolean more_accurate;
        boolean much_less_accurate;
        boolean ret;
        
        ret = false;
        
        if (null == currentBestLocation)
            // a new location is always better than no location
            ret = true;
        else
        {
    
            // check whether the new location fix is newer or older
            time_delta  = location.getTime () - currentBestLocation.getTime ();
            much_newer = time_delta > _AgeIntervalThreshold;
            much_older = time_delta < -_AgeIntervalThreshold;
            newer = time_delta > 0;
    
            if (much_newer)
                // after so much time the user has likely moved
                ret = true;
            else
                if (!much_older)
                {
                    // not too old, so check whether the new location fix is more or less accurate
                    accuracy_delta = location.getAccuracy () - currentBestLocation.getAccuracy ();
                    less_accurate = accuracy_delta > 0.0f;
                    more_accurate = accuracy_delta < 0.0f;
                    much_less_accurate = accuracy_delta > _AccuracyThreshold;
            
                    if (more_accurate)
                        // location is more accurate
                        ret = true;
                    else
                        if (newer && !less_accurate)
                            // location is newer and not any less accurate
                            ret = true;
                        else
                            if (newer && !much_less_accurate && isSameProvider (location.getProvider (), currentBestLocation.getProvider ()))
                                // location is newer, not much less accurate and the old and new location are from the same provider
                                ret = true;
                            // otherwise it's not newer or not any more accurate and maybe from another provider, so ignore it
                }
                // else the new location is much older, hence it must be worse
        }
        
        return (ret);
    }

    protected boolean isSameProvider (String provider1, String provider2)
    {
        boolean ret;
        
        ret = false;

        if (null == provider1)
            ret = null == provider2; // can't really tell, but assume they're the same
        else
            ret = provider1.equals (provider2);

        return (ret);
    }

    //
    // LocationListener interface
    // A LocationListener whose onLocationChanged(Location) method will be called for each location update.
    //

    @Override
    public void onLocationChanged (Location location)
    {
        Log.i (_ClassName, "location changed " + location.getLatitude () + "" +  location.getLongitude ());
        if (isBetterLocation (location, _Current))
        {
            _Current = location;
            synchronized (_Listeners)
            {
                // TODO: should really _Listeners.clone ();
                for (PositionChangeListener listener : _Listeners)
                    try
                    {
                        listener.onPositionChange (this);
                    }
                    catch (Exception e)
                    {
                        Log.e (_ClassName, "PositionChangeListener threw an exception", e);
                    }
            }

        }
    }

    @Override
    public void onStatusChanged (String provider, int status, Bundle extras)
    {
        // TODO Auto-generated method stub
    }

    @Override
    public void onProviderEnabled (String provider)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void onProviderDisabled (String provider)
    {
        // TODO Auto-generated method stub

    }

}
