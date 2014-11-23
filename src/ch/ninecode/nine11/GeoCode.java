package ch.ninecode.nine11;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ch.ninecode.nine11.R;
import android.app.Activity;
import android.os.AsyncTask;
import android.widget.EditText;

public class GeoCode extends AsyncTask<Double, Void, String>
{
	Activity _Activity;

	public GeoCode (Activity activity)
	{
		_Activity = activity;
	}
	protected String doInBackground (Double... coordinates)
	{
		return (getAddress (coordinates[0], coordinates[1]));
	}

	protected void onPostExecute (String result)
	{
		EditText text = (EditText)(_Activity.findViewById (R.id.addressText));
		text.setText (result);
	}

	public static String readFully (InputStream is)
	{

		BufferedReader reader = new BufferedReader (new InputStreamReader (is));
		StringBuilder sb = new StringBuilder ();

		String line = null;
		try
		{
			while (null != (line = reader.readLine ()))
				sb.append (line + "\n");
		}
		catch (IOException e)
		{
			e.printStackTrace ();
		}
		finally
		{
			try
			{
				is.close ();
			}
			catch (IOException e)
			{
				e.printStackTrace ();
			}
		}
	
		return (sb.toString ());
	}

	public static String getAddress (double longitude, double latitude)
	{
		String uri;
		URL url;
		HttpURLConnection connection;
		InputStream is;
		String json;
		JSONObject result;
		JSONArray results;
		String ret;
		
		ret = "";

		try
		{
			// https://maps.googleapis.com/maps/api/geocode/json?latlng=40.714224,-73.961452&key=API_KEY
			uri = "https://maps.googleapis.com/maps/api/geocode/json?latlng=" + latitude + "," + longitude;
			url = new URL (uri);
			connection = (HttpURLConnection) url.openConnection ();
			try
			{
				is = connection.getInputStream ();
				json = readFully (is);
				result = new JSONObject (json);
				results = result.getJSONArray ("results");
				if (0 != results.length ())
					ret = results.getJSONObject (0).getString ("formatted_address");
			}
			catch (JSONException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			finally
			{
				connection.disconnect ();
			}
		}
		catch (MalformedURLException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace ();
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace ();
		}

		return (ret);
	}
}
