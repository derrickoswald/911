package ch.ninecode.nine11;

import java.util.TimerTask;

import android.app.Activity;
import android.widget.TextView;

class CountdownTask extends TimerTask
{
    protected Activity _Activity;
    private int _Count;
    protected Callback _Callback;
    
    public interface Callback
    {
        public void execute ();
    }

    public CountdownTask (Activity activity, int count, Callback callback)
    {
        _Activity = activity;
        setCount (count);
        _Callback = callback;
    }

    public int getCount ()
    {
        return (_Count);
    }

    public void setCount (int count)
    {
        _Count = count + 1; // because it decrements before doing anything else
    }

    public void run ()
    {
        _Count--;
        _Activity.runOnUiThread (new Runnable()
        {
            @Override
            public void run()
            {
                TextView text = (TextView) (_Activity.findViewById (R.id.countdown));
                text.setVisibility (android.view.View.VISIBLE);
                text.setText (Integer.toString (getCount()));
            }
        });
        if (0 == getCount())
        {
            cancel ();
            _Activity.runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    TextView text = (TextView) (_Activity.findViewById (R.id.countdown));
                    text.setVisibility (android.view.View.INVISIBLE);
                    _Callback.execute ();
                }
            });
        }
    }
}

