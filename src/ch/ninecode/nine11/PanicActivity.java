package ch.ninecode.nine11;

import android.app.Activity;
import android.app.NotificationManager;
import android.os.Bundle;

public class PanicActivity extends Activity
{

    @Override
    protected void onCreate (Bundle savedInstanceState)
    {
        super.onCreate (savedInstanceState);
        setContentView (R.layout.activity_panic);
    }

    @Override
    protected void onResume ()
    {
        super.onResume ();
        int id = 001;
        // get an instance of the NotificationManager service
        NotificationManager manager = (NotificationManager) (getSystemService (NOTIFICATION_SERVICE));
        manager.cancel (id);
    }
}
