package ch.ninecode.nine11;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.RemoteViews;

public class AppWidgetProvider extends android.appwidget.AppWidgetProvider
{

	public void onUpdate (Context context, AppWidgetManager manager, int[] ids)
	{
		final int N = ids.length;
		int id;
		Bundle options;
		int category;
		boolean keyguard;
		int layout;
		Intent intent;
		PendingIntent pending;
		RemoteViews views;

		// perform this loop procedure for each App Widget that belongs to this provider
		for (int i = 0; i < N; i++)
		{
			id = ids[i];

			options = manager.getAppWidgetOptions (id);

			// get the value of OPTION_APPWIDGET_HOST_CATEGORY
			category = options.getInt(AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY, -1);

			// if the value is WIDGET_CATEGORY_KEYGUARD, it's a lockscreen widget
			keyguard = category == AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD;
			
			// get the appropriate layout
			layout = keyguard ? R.layout.keyguard_layout : R.layout.widget_layout;
			
			// create an Intent to launch a PanicActivity
			intent = new Intent (context, PanicActivity.class);
			pending = PendingIntent.getActivity (context, 0, intent, 0);

			// get the layout for the App Widget and attach an on-click listener to the button
			views = new RemoteViews (context.getPackageName (), layout);
			views.setOnClickPendingIntent (R.id.panic_button, pending);

			// tell the AppWidgetManager to perform an update on the current app widget
			manager.updateAppWidget (id, views);
		}
	}
}