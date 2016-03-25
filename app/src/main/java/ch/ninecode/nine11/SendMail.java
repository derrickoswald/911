package ch.ninecode.nine11;

import ch.ninecode.nine11.R;
import android.app.Activity;
import android.os.AsyncTask;
import android.widget.EditText;
import android.widget.Toast;

public class SendMail extends AsyncTask<SendMail.Details, Void, Void>
{
	Activity _Activity;

	public static class Details
	{
		public String _Subject;
		public String _Message;
		public String _Sender;
		public String _Host;
		public String _Password;
		public String _Recipient;
		public Details (String subject, String message, String host, String sender, String password, String recipient)
		{
			_Subject = subject;
			_Message = message;
            _Host = host;
			_Sender = sender;
			_Password = password;
			_Recipient = recipient;
		}
	}


	public SendMail (Activity activity)
	{
		_Activity = activity;
	}

	@Override
    protected Void doInBackground (Details... details)
	{
		try
		{
			MailSender sender = new MailSender (details[0]._Host, details[0]._Sender, details[0]._Password);
			sender.sendMail (details[0]._Subject, details[0]._Message, details[0]._Sender, details[0]._Recipient);
		}
		catch (Exception e)
		{
			Toast.makeText (_Activity.getApplicationContext (), "Email to " + details[0]._Recipient + " failed. " + e.getMessage (), Toast.LENGTH_LONG).show ();
			e.printStackTrace ();
		}

		return (null);
	}

	protected void onPostExecute (String result)
	{
		EditText text = (EditText)(_Activity.findViewById (R.id.addressText));
		text.setText (result);
	}
}
