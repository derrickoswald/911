package ch.ninecode.nine11;

import android.content.Context;
import android.preference.EditTextPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

public class EditTextPreferenceWithValueSummary extends EditTextPreference
{
	public EditTextPreferenceWithValueSummary (Context context)
	{
		super (context);
	}

	public EditTextPreferenceWithValueSummary (Context context, AttributeSet attrs)
	{
		super (context, attrs);
	}

	@Override
	protected View onCreateView (ViewGroup parent)
	{
		View ret;
		
		setSummary (getText ());
		ret = super.onCreateView (parent);

		return (ret);
	}

	@Override
	protected void onDialogClosed (boolean positiveResult)
	{
		super.onDialogClosed (positiveResult);
		if (positiveResult)
			setSummary (getText ());
	}
}